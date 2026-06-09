package timp.controller;

import timp.dto.RoadRequest;
import timp.dto.RoadResponse;
import timp.service.RoadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    private final RoadService roadService;

    public RoadController(RoadService roadService) {
        this.roadService = roadService;
    }

    @Operation(summary = "Получение информации о всех дорогах")
    @GetMapping
    public ResponseEntity<List<RoadResponse>> getAllRoads() {
        return ResponseEntity.ok(roadService.getAllRoads());
    }

    @Operation(summary = "Создание дороги")
    @PostMapping
    public ResponseEntity<RoadResponse> createRoad(@Valid @RequestBody RoadRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(roadService.createRoad(request));
    }

    @Operation(summary = "Изменение информации о дороге")
    @PutMapping("/{id}")
    public ResponseEntity<RoadResponse> updateRoad(@PathVariable Long id,
                                                    @Valid @RequestBody RoadRequest request) {
        return ResponseEntity.ok(roadService.updateRoad(id, request));
    }

    @Operation(summary = "Удаление дороги")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoad(@PathVariable Long id) {
        roadService.deleteRoad(id);
        return ResponseEntity.noContent().build();
    }
}
