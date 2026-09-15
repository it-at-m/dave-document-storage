package de.muenchen.dave.lageplaene.domain.service;

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

    private final S3OutPort s3OutPort;
    private final String lageplaeneBasePath;
    private final Integer expirationInMinutes;

    public LageplanService(
            final S3OutPort s3OutPort,
            @Value("${de.muenchen.dave.document-storage.lageplaene.base-path}") final String basePath,
            @Value("${de.muenchen.dave.document-storage.lageplaene.expiration-in-minutes}") final Integer expirationInMinutes) {
        this.s3OutPort = s3OutPort;
        this.lageplaeneBasePath = basePath;
        this.expirationInMinutes = expirationInMinutes;
    }

    /**
     * Liefert den aktuellen Lageplan für eine gegebene Messstelle zurück.
     *
     * @param mstId zur Ermittlung des Speicherorts des Lageplans.
     * @return Optional<PresignedURL> zum Holen des aktuellen Lageplans.
     * @throws S3Exception falls ein Fehler beim Zugriff auf den S3-Bucket auftritt.
     */
    public Optional<DocumentDto> getNewestLageplanForGivenMessstelleId(final String mstId) throws S3Exception {
        final String pathToLageplan = buildPathToLageplan(lageplaeneBasePath, mstId);
        final Optional<FileReference> filePath = lageplanForGivenMessstelleIdExists(mstId, pathToLageplan);
        final Duration expiration = Duration.ofMinutes(expirationInMinutes);
        if (filePath.isPresent()) {
            final PresignedUrl url = s3OutPort.getPresignedUrl(filePath.get(), PresignedUrl.Action.GET, expiration);
            return Optional.of(new DocumentDto(url.url().toExternalForm()));
        } else {
            log.error("Kein Lageplan für Messstelle {} unter {} gefunden", mstId, pathToLageplan);
            return Optional.empty();
        }
    }

    /**
     * Liefert zurück, ob für eine bestimmte Messstelle ein Lageplan existiert.
     *
     * @param mstId zur Ermittlung des Speicherorts des Lageplans.
     * @return Optional<FileReference>.
     * @throws S3Exception falls ein Fehler beim Auslesen der Dateien aus dem S3-Bucket auftritt.
     */
    public Optional<FileReference> lageplanForGivenMessstelleIdExists(final String mstId, final String pathToLageplan) throws S3Exception {
        final FileReference fileReference = new FileReference(bucket, pathToLageplan);
        return getFilePathOfNewestFileInFolderAndSubfolder(fileReference);
    }

    /**
     * Sucht die neueste Datei in einem gegebenen Ordner und dessen Unterordnern.
     * Die neueste Datei wird anhand des Zeitstempels der letzten Änderung (lastModified)
     * ermittelt.
     *
     * @param fileReference der Startordner (Bucket und Pfad) für die Suche nach der neuesten Datei.
     * @return ein {@link Optional}, das die {@link FileReference} der neuesten Datei enthält,
     *         oder {@link Optional#empty()}, falls keine Datei gefunden wurde.
     * @throws S3Exception falls ein Fehler beim Auslesen der Dateien aus dem S3-Bucket auftritt.
     */
    protected Optional<FileReference> getFilePathOfNewestFileInFolderAndSubfolder(final FileReference fileReference) throws S3Exception {
        try {
            Optional<String> path = s3OutPort.getFilesWithPrefix(fileReference.bucket(), fileReference.path(), true).files().stream()
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
