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

    private final String bucket;

    static final String SEPARATOR = "/";

    private final S3OutPort s3OutPort;
    private final String lageplaeneBasePath;
    private final Integer expirationInMinutes;

    public LageplanService(
            final S3OutPort s3OutPort,
            @Value("${refarch.s3.bucket-name}") final String bucket,
            @Value("${de.muenchen.dave.document-storage.lageplaene.base-path}") final String basePath,
            @Value("${de.muenchen.dave.document-storage.lageplaene.expiration-in-minutes}") final Integer expirationInMinutes) {
        this.bucket = bucket;
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
        final Optional<FileReference> filePath = lageplanForGivenMessstelleIdExists(mstId);
        if (filePath.isPresent()) {
            final PresignedUrl url = s3OutPort.getPresignedUrl(filePath.get(), PresignedUrl.Action.GET, Duration.ofMinutes(expirationInMinutes));
            return Optional.of(new DocumentDto(url.url().toExternalForm()));
        } else {
            log.error("Kein Lageplan für Messstelle {} unter {} gefunden", mstId, buildPathToLageplan(lageplaeneBasePath, mstId));
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
    public Optional<FileReference> lageplanForGivenMessstelleIdExists(final String mstId) throws S3Exception {
        final FileReference fileReference = new FileReference(bucket, buildPathToLageplan(lageplaeneBasePath, mstId));
        return getFilePathOfNewestFileInFolderAndSubfolder(fileReference);
    }

    /**
     * Sucht die neueste Datei in einem gegebenen Ordner und dessen Unterordnern.
     * Die neueste Datei wird anhand des Zeitstempels der letzten Änderung (lastModified)
     * ermittelt.
     *
     * <p>
     * <strong>Wichtig / Hinweis zur Paginierung:</strong><br>
     * Diese Implementierung verwendet s3OutPort.getFilesWithPrefix(...).files() direkt und betrachtet
     * nur die von dieser einzelnen Aufruf‑Seite zurückgegebenen Objekte. Das verwendete S3‑Adapter‑API
     * (getFilesWithPrefix) liefert standardmäßig nur eine Seite der Ergebnisse (maximal 1000 Objekte
     * pro Seite). Wenn mehr als 1000 Dateien unter dem angegebenen Präfix existieren, kann die Antwort
     * paginiert sein (response.isTruncated() == true) — spätere Seiten werden hier nicht abgefragt.
     * In diesem Fall ist das Ergebnis unvollständig und die tatsächlich neueste Datei (auf einer
     * späteren
     * Seite) wird möglicherweise übersehen.
     * </p>
     *
     * <p>
     * Empfehlung: Vor dem Bestimmen des neuesten Objekts alle Seiten aggregieren (ggf. über
     * continuationToken / nextContinuationToken iterieren) oder die Port/Adapter‑API erweitern, sodass
     * eine Methode zur Verfügung steht, die alle Dateien über alle Seiten zusammenfasst.
     * </p>
     *
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

    public String buildPathToLageplan(final String lageplaeneBasePath, String mstId) {
        return lageplaeneBasePath + (lageplaeneBasePath.endsWith(SEPARATOR) ? mstId : SEPARATOR + mstId) + SEPARATOR;
    }
}
