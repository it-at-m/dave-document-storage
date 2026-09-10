package de.muenchen.dave.lageplaene.domain.service;

import de.muenchen.dave.errorhandling.ResourceNotFoundException;
import de.muenchen.dave.lageplaene.api.dto.DocumentDto;
import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileMetadata;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileReference;
import de.muenchen.oss.refarch.integration.s3.domain.model.PresignedUrl;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LageplanService {

    @Value("${refarch.s3.bucket-name}")
    private String bucket;

    static final String SEPARATOR = "/";

    private final S3OutPort s3Adapter;
    private final String lageplaeneBasePath;
    private final Integer expirationInMinutes;

    public LageplanService(
            final S3OutPort s3Adapter,
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
     * @throws S3Exception
     * @throws ResourceNotFoundException
     */
    public DocumentDto getNewestLageplanForGivenMessstelleId(final String mstId) throws S3Exception, ResourceNotFoundException {
        final String pathToLageplan = buildPathToLageplan(lageplaeneBasePath, mstId);
        final Optional<FileReference> filePath = getFilePathOfNewestFileInFolderAndSubfolder(new FileReference(bucket, pathToLageplan));
        final Duration expiration = Duration.ofMinutes(expirationInMinutes);
        if (filePath.isPresent()) {
            final PresignedUrl url = s3Adapter.getPresignedUrl(filePath.get(), PresignedUrl.Action.GET, expiration);
            return new DocumentDto(url.url().toExternalForm());
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
     * @throws S3Exception
     * @throws ResourceNotFoundException
     */
    public Boolean lageplanForGivenMessstelleIdExists(final String mstId) throws S3Exception {
        final String pathToLageplan = buildPathToLageplan(lageplaeneBasePath, mstId);
        final FileReference fileReference = new FileReference(bucket, pathToLageplan);
        final Optional<FileReference> filePath = getFilePathOfNewestFileInFolderAndSubfolder(fileReference);
        return filePath.isPresent();
    }

    protected Optional<FileReference> getFilePathOfNewestFileInFolderAndSubfolder(final FileReference fileReference) throws S3Exception {
        try {

            Optional<String> path = s3Adapter.getFilesWithPrefix(fileReference.bucket(), fileReference.path(), true).files().stream()
                    .max(Comparator.comparing(FileMetadata::lastModified))
                    .map(FileMetadata::path);
            return path.map(s -> new FileReference(fileReference.bucket(), s));

        } catch (S3Exception exception) {
            log.error("Fehler beim Auslesen des Folders: {}", fileReference.path());
            throw exception;
        }
    }

    private String buildPathToLageplan(final String lageplaeneBasePath, String mstId) {
        return lageplaeneBasePath + (lageplaeneBasePath.endsWith(SEPARATOR) ? mstId : SEPARATOR + mstId) + SEPARATOR;
    }
}
