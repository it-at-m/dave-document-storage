package de.muenchen.dave.lageplaene.domain.service;

import de.muenchen.dave.errorhandling.ResourceNotFoundException;
import de.muenchen.dave.lageplaene.api.dto.DocumentDto;
import de.muenchen.refarch.integration.s3.adapter.out.s3.S3Adapter;
import de.muenchen.refarch.integration.s3.domain.exception.FileSystemAccessException;
import io.minio.http.Method;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LageplanService {

    static final String SEPARATOR = "/";

    private final S3Adapter s3Adapter;
    private final String lageplaeneBasePath;
    private final Integer expirationInMinutes;

    public LageplanService(
            final S3Adapter s3Adapter,
            @Value("${de.muenchen.dave.document-storage.lageplaene.base-path}") final String basePath,
            @Value("${de.muenchen.dave.document-storage.lageplaene.expiration-in-minutes}") final Integer expirationInMinutes) {
        this.s3Adapter = s3Adapter;
        this.lageplaeneBasePath = basePath;
        this.expirationInMinutes = expirationInMinutes;
    }

    /**
     * Liefert die Lageplan-Informationen für eine bestimmte Messstelle zurück.
     */
    public DocumentDto getLageplan(final String mstId) throws FileSystemAccessException, ResourceNotFoundException {
        final String pathToLageplan = buildPathToLageplan(lageplaeneBasePath, mstId);
        final Optional<String> filePath = getFirstFileFromFolder(pathToLageplan);
        if (filePath.isPresent()) {
            final String url = s3Adapter.getPresignedUrl(filePath.get(), Method.GET, expirationInMinutes);
            return new DocumentDto(url);
        } else {
            log.error("Kein Dokument gefunden: {}", pathToLageplan);
            throw new ResourceNotFoundException(pathToLageplan);
        }
    }

    /**
     * Liefert zurück, ob für eine bestimmte Messstelle ein Lageplan existiert.
     */
    public Boolean lageplanExists(final String mstId) throws FileSystemAccessException {
        final String pathToLageplan = buildPathToLageplan(lageplaeneBasePath, mstId);
        final Optional<String> filePath = getFirstFileFromFolder(pathToLageplan);
        return filePath.isPresent();
    }

    private Optional<String> getFirstFileFromFolder(final String pathToFile) throws FileSystemAccessException {
        try {
            List<String> list = s3Adapter.getFilePathsFromFolder(pathToFile).stream()
                    // ignore paths that represent the parent folder itself
                    .filter(path -> !path.equals(pathToFile))
                    .toList();
            if (list.isEmpty()) {
                return Optional.empty();
            }
            if (list.size() > 1) {
                log.warn("Achtung: mehr als eine Datei gefunden: {}", pathToFile);
            }
            return Optional.of(list.getFirst());
        } catch (FileSystemAccessException e) {
            log.error("Fehler beim Auslesen des Folders: {}", pathToFile);
            throw e;
        }
    }

    private String buildPathToLageplan(final String lageplaeneBasePath, String mstId) {
        return lageplaeneBasePath + (lageplaeneBasePath.endsWith(SEPARATOR) ? mstId : SEPARATOR + mstId) + SEPARATOR;
    }
}
