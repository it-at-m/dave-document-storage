package de.muenchen.dave.lageplaene.api.controller;

import de.muenchen.dave.errorhandling.ResourceNotFoundException;
import de.muenchen.dave.lageplaene.api.dto.DocumentDto;
import de.muenchen.dave.lageplaene.domain.service.LageplanService;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/lageplan")
@Tag(name = "Lageplan", description = "API zum Abfragen der Lagepläne.")
@Validated
public class LageplanController {

    private final LageplanService lageplanService;

    public LageplanController(LageplanService lageplanService) {
        this.lageplanService = lageplanService;
    }

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
            throws S3Exception, ResourceNotFoundException {
        log.info("Abfrage des aktuellsten Lageplans: {}", mstId);
        final Optional<DocumentDto> dto = lageplanService.getNewestLageplanForGivenMessstelleId(mstId);
        if (dto.isPresent())
            return ResponseEntity.ok(dto.get());
        else
            throw new ResourceNotFoundException("Der angefragte Lageplan wurde nicht gefunden.");
    }

    @GetMapping("/exists")
    @Operation(summary = "Existiert für die spezifizierte Messstelle ein Lageplan?")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Der Lageplan wurde erfolgreich abgefragt."),
                    @ApiResponse(responseCode = "500", description = "Bei der Bearbeitung des Requests ist ein Fehler aufgetreten.")
            }
    )
    public ResponseEntity<Boolean> lageplanExists(@RequestParam(value = "mstId") @NotBlank final String mstId) throws S3Exception {
        log.debug("Abfrage auf Lageplan: {}", mstId);
        return ResponseEntity.ok(lageplanService.lageplanForGivenMessstelleIdExists(mstId).isPresent());
    }
}
