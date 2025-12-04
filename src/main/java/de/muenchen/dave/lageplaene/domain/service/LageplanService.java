package de.muenchen.dave.lageplaene.domain.service;

import de.muenchen.dave.errorhandling.ResourceNotFoundException;
import de.muenchen.dave.lageplaene.api.dto.DocumentDto;
import de.muenchen.refarch.integration.s3.adapter.out.s3.S3Adapter;
import de.muenchen.refarch.integration.s3.domain.exception.FileSystemAccessException;
import de.muenchen.refarch.integration.s3.domain.model.FileMetadata;
import io.minio.http.Method;

import java.util.Comparator;
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
     * Liefert den aktuellsten Lageplan für eine gegebene Messstelle zurück.
     *
     * @param mstId zur Ermittlung des Speicherorts des Lageplans.
     * @return die Presigned-URL zum holen des aktuellsten Lageplans.
     * @throws FileSystemAccessException
     * @throws ResourceNotFoundException
     */
    public DocumentDto getNewestLageplanForGivenMessstelleId(final String mstId) throws FileSystemAccessException, ResourceNotFoundException {
        final String pathToLageplan = buildPathToLageplan(lageplaeneBasePath, mstId);
        final Optional<String> filePath = getFilePathOfNewestFileInFolderAndSubfolder(pathToLageplan);
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
     *
     * @param mstId zur Ermittlung des Speicherorts des Lageplans.
     * @return true falls ein Lageplan exitiert andernfalls false.
     * @throws FileSystemAccessException
     * @throws ResourceNotFoundException
     */
    public Boolean lageplanForGivenMessstelleIdExists(final String mstId) throws FileSystemAccessException {
        final String pathToLageplan = buildPathToLageplan(lageplaeneBasePath, mstId);
        final Optional<String> filePath = getFilePathOfNewestFileInFolderAndSubfolder(pathToLageplan);
        return filePath.isPresent();
    }

    protected Optional<String> getFilePathOfNewestFileInFolderAndSubfolder(final String pathToFile) throws FileSystemAccessException {
        try {
            return s3Adapter.getMetadataOfFilesFromFolder(pathToFile).stream()
                    .max(Comparator.comparing(FileMetadata::lastModified))
                    .map(FileMetadata::pathToFile);
        } catch (FileSystemAccessException exception) {
            log.error("Fehler beim Auslesen des Folders: {}", pathToFile);
            throw exception;
        }

    }

    private String buildPathToLageplan(final String lageplaeneBasePath, String mstId) {
        return lageplaeneBasePath + (lageplaeneBasePath.endsWith(SEPARATOR) ? mstId : SEPARATOR + mstId) + SEPARATOR;
    }
}
