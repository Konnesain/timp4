package timp.controller;

import timp.model.RefreshToken;
import timp.model.User;
import timp.repository.UserRepository;
import timp.config.JwtUtil;
import timp.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final int accessCookieMaxAge;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          RefreshTokenService refreshTokenService,
                          @Value("${jwt.access-expiration}") long accessExpiration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.accessCookieMaxAge = (int) (accessExpiration / 1000);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String password,
                                   HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Неверный логин или пароль", "success", false));
        }

        String accessToken = jwtUtil.generateAccessToken(username);
        RefreshToken rt = refreshTokenService.createRefreshToken(username);
        setJwtCookie(response, accessToken);
        return ResponseEntity.ok(Map.of(
                "username", username,
                "token", accessToken,
                "refreshToken", rt.getToken(),
                "success", true
        ));
    }

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
        if (userRepository.findByUsername(username.trim()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Пользователь с таким именем уже существует", "success", false));
        }

        User user = new User(username.trim(), passwordEncoder.encode(password));
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        RefreshToken rt = refreshTokenService.createRefreshToken(user.getUsername());
        setJwtCookie(response, accessToken);
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "token", accessToken,
                "refreshToken", rt.getToken(),
                "success", true
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam String refreshToken,
                                     HttpServletResponse response) {
        try {
            RefreshToken rt = refreshTokenService.verifyAndRotate(refreshToken);
            String accessToken = jwtUtil.generateAccessToken(rt.getUsername());
            setJwtCookie(response, accessToken);
            return ResponseEntity.ok(Map.of(
                    "username", rt.getUsername(),
                    "token", accessToken,
                    "refreshToken", rt.getToken(),
                    "success", true
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Недействительный refresh token", "success", false));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String refreshToken,
                                    HttpServletResponse response) {
        try {
            RefreshToken rt = refreshTokenService.verifyAndRotate(refreshToken);
            refreshTokenService.revokeAllForUser(rt.getUsername());
        } catch (RuntimeException ignored) {
        }
        clearJwtCookie(response);
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
}
