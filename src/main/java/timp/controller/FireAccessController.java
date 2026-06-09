package timp.controller;

import timp.dto.FireAccessRequest;
import timp.dto.FireAccessResponse;
import timp.service.FireAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/fire-access")
public class FireAccessController {

    private final FireAccessService fireAccessService;

    public FireAccessController(FireAccessService fireAccessService) {
        this.fireAccessService = fireAccessService;
    }

    @Operation(summary = "Получение информации о всех пожарных подъездах")
    @GetMapping
    public ResponseEntity<List<FireAccessResponse>> getAll() {
        return ResponseEntity.ok(fireAccessService.getAll());
    }

    @Operation(summary = "Создание пожарного подъезда")
    @PostMapping
    public ResponseEntity<FireAccessResponse> create(@Valid @RequestBody FireAccessRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(fireAccessService.create(request));
    }

    @Operation(summary = "Изменение информации о пожарном подъезде")
    @PutMapping("/{id}")
    public ResponseEntity<FireAccessResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody FireAccessRequest request) {
        return ResponseEntity.ok(fireAccessService.update(id, request));
    }

    @Operation(summary = "Удаление пожарного подъезда")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fireAccessService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
