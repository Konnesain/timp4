package timp.controller;

import timp.model.RefreshToken;
import timp.model.SecurityEvent;
import timp.model.User;
import timp.repository.UserRepository;
import timp.config.JwtUtil;
import timp.service.CustomUserDetailsService;
import timp.service.RefreshTokenService;
import timp.service.SecurityEventLogger;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final SecurityEventLogger eventLogger;
    private final int accessCookieMaxAge;
    private final int refreshCookieMaxAge;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          CustomUserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          RefreshTokenService refreshTokenService,
                          SecurityEventLogger eventLogger,
                          @Value("${jwt.access-expiration}") long accessExpiration,
                          @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.eventLogger = eventLogger;
        this.accessCookieMaxAge = (int) (accessExpiration / 1000);
        this.refreshCookieMaxAge = (int) (refreshExpiration / 1000);
    }

    @Operation(summary = "Аутентификация пользователя", description = "Устанавливает HttpOnly cookie jwt_token и refresh_token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Успешный вход"),
        @ApiResponse(responseCode = "401", description = "Неверный логин или пароль")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String password,
                                   HttpServletResponse response) {
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            eventLogger.log(SecurityEvent.EventType.AUTH_FAILED,
                    "Неверный логин или пароль: " + username, false, -1, username);
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Неверный логин или пароль", "success", false));
        }
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            eventLogger.log(SecurityEvent.EventType.AUTH_FAILED,
                    "Неверный логин или пароль: " + username, false, -1, username);
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Неверный логин или пароль", "success", false));
        }

        int userId = userRepository.findByUsername(username)
                .map(u -> u.getId().intValue())
                .orElse(0);
        eventLogger.log(SecurityEvent.EventType.AUTH_LOGIN,
                "Успешный вход: " + username, true, userId, username);

        String accessToken = jwtUtil.generateAccessToken(username);
        RefreshToken rt = refreshTokenService.createRefreshToken(username);
        setJwtCookie(response, accessToken);
        setRefreshCookie(response, rt.getToken());
        return ResponseEntity.ok(Map.of(
                "username", username,
                "success", true
        ));
    }

    @Operation(summary = "Регистрация пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Пользователь создан и аутентифицирован"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации (пустое имя, короткий пароль, дубликат)")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                      @RequestParam String password,
                                      HttpServletResponse response) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Имя пользователя не может быть пустым", "success", false));
        }
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Пароль должен содержать минимум 6 символов", "success", false));
        }
        if (username.trim().length() > 255) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Имя пользователя не должно превышать 255 символов", "success", false));
        }
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Пользователь с таким именем уже существует", "success", false));
        }

        User user = new User(username.trim(), passwordEncoder.encode(password));
        userRepository.save(user);
        eventLogger.logRegistration(user.getUsername(), user.getId().intValue());

        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        RefreshToken rt = refreshTokenService.createRefreshToken(user.getUsername());
        setJwtCookie(response, accessToken);
        setRefreshCookie(response, rt.getToken());
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "success", true
        ));
    }

    @Operation(summary = "Обновление access токена", description = "Читает refresh_token из HttpOnly cookie, устанавливает новые jwt_token и refresh_token",
               parameters = { @Parameter(in = ParameterIn.COOKIE, name = "refresh_token", description = "Refresh токен (устанавливается при login/register)", required = true) })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Токены обновлены"),
        @ApiResponse(responseCode = "401", description = "Недействительный или истёкший refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request,
                                     HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Refresh token not found", "success", false));
        }
        try {
            RefreshToken rt = refreshTokenService.verifyAndRotate(refreshToken);
            String accessToken = jwtUtil.generateAccessToken(rt.getUsername());
            clearJwtCookie(response);
            clearRefreshCookie(response);
            setJwtCookie(response, accessToken);
            setRefreshCookie(response, rt.getToken());
            return ResponseEntity.ok(Map.of(
                    "username", rt.getUsername(),
                    "success", true
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Недействительный refresh token", "success", false));
        }
    }

    @Operation(summary = "Выход из системы", description = "Отзывает все refresh токены пользователя и очищает HttpOnly cookie",
               parameters = { @Parameter(in = ParameterIn.COOKIE, name = "refresh_token", description = "Refresh токен (устанавливается при login/register)", required = true) })
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Все refresh токены пользователя отозваны, cookie очищена")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken != null) {
            refreshTokenService.revokeByToken(refreshToken);
        }
        clearJwtCookie(response);
        clearRefreshCookie(response);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setPath("/");
        cookie.setMaxAge(accessCookieMaxAge);
        response.addCookie(cookie);
    }

    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", null);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setPath("/");
        cookie.setMaxAge(refreshCookieMaxAge);
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
