package org.nr31.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.SupportedLocaleDTO;
import org.nr31.backend.service.PublicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Endpoints for publicly available resources")
public class PublicController {

    private final PublicService publicService;

    @Operation(summary = "Get supported locales", description = "Retrieves a list of all supported locales")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved supported locales", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SupportedLocaleDTO.class))))
    })
    @GetMapping(value = "/locales", produces = "application/json")
    public ResponseEntity<List<SupportedLocaleDTO>> getSupportedLocales() {
        return ResponseEntity.ok(publicService.getSupportedLocales());
    }
}
