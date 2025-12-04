package de.muenchen.dave.lageplaene.api.controller;

import de.muenchen.dave.errorhandling.ResourceNotFoundException;
import de.muenchen.dave.lageplaene.api.dto.DocumentDto;
import de.muenchen.dave.lageplaene.domain.service.LageplanService;
import de.muenchen.refarch.integration.s3.domain.exception.FileSystemAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/lageplan")
@Tag(name = "Lageplan", description = "API zum Abfragen der Lagepläne.")
@Validated
public class LageplanController {

    private final LageplanService lageplanService;

    @GetMapping
    @Operation(summary = "Liefert den aktuellsten Lageplan für eine gegebene Messstelle.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Der Lageplan wurde erfolgreich abgefragt."),
                    @ApiResponse(responseCode = "404", description = "Der angefragte Lageplan wurde nicht gefunden."),
                    @ApiResponse(responseCode = "500", description = "Bei der Bearbeitung des Requests ist ein Fehler aufgetreten.")
            }
    )
    public ResponseEntity<DocumentDto> getLageplan(@RequestParam(value = "mstId") @NotBlank final String mstId)
            throws FileSystemAccessException, ResourceNotFoundException {
        log.info("Abfrage des aktuellsten Lageplans: {}", mstId);
        final DocumentDto dto = lageplanService.getNewestLageplanForGivenMessstelleId(mstId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/exists")
    @Operation(summary = "Existiert für die spezifizierte Messstelle ein Lageplan?")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Der Lageplan wurde erfolgreich abgefragt."),
                    @ApiResponse(responseCode = "500", description = "Bei der Bearbeitung des Requests ist ein Fehler aufgetreten.")
            }
    )
    public ResponseEntity<Boolean> lageplanExists(@RequestParam(value = "mstId") @NotBlank final String mstId) throws FileSystemAccessException {
        log.debug("Abfrage auf Lageplan: {}", mstId);
        final Boolean hasLageplan = lageplanService.lageplanForGivenMessstelleIdExists(mstId);
        return ResponseEntity.ok(hasLageplan);
    }
}
