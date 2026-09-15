package de.muenchen.dave.lageplaene.domain.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import de.muenchen.dave.errorhandling.ResourceNotFoundException;
import de.muenchen.dave.lageplaene.api.dto.DocumentDto;
import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileMetadata;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileReference;
import de.muenchen.oss.refarch.integration.s3.domain.model.ListResult;
import de.muenchen.oss.refarch.integration.s3.domain.model.PresignedUrl;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LageplanServiceTest {

    private static final String bucket = null;
    private static final String BASE_PATH = "DAVe/Messstellen/Lageplaene/";
    private static final Integer EXPIRATION = 30;

    @Mock
    private S3OutPort s3Adapter;

    private LageplanService lageplanService;

    @BeforeEach
    public void beforeEach() {
        lageplanService = new LageplanService(
                s3Adapter,
                BASE_PATH,
                EXPIRATION);
        Mockito.reset(s3Adapter);
    }

    @Test
    void testGetNewestLageplanForGivenMessstelleId_WithExistingFile() throws S3Exception, MalformedURLException {

        final String mstId = "4001";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;
        final String presignedUrl = "https://the-presigned-url-to-file.pdf";

        final var fileMetadata1 = new FileMetadata(
                parentFolder + mstId + "1.pdf",
                999L,
                "etag",
                createInstant(2025, 1, 1, 12, 0, 0));

        ListResult listResult = new ListResult(List.of(fileMetadata1), List.of(parentFolder), false, null);
        Mockito.when(s3Adapter.getFilesWithPrefix(bucket, parentFolder, true)).thenReturn(listResult);

        FileReference fileReference = new FileReference(bucket, parentFolder + mstId + "1.pdf");
        final Duration expiration = Duration.ofMinutes(EXPIRATION);
        PresignedUrl presignedUrlObj = new PresignedUrl(new URL(presignedUrl), parentFolder, PresignedUrl.Action.GET);
        Mockito.when(s3Adapter.getPresignedUrl(fileReference, PresignedUrl.Action.GET, expiration)).thenReturn(presignedUrlObj);

        DocumentDto result = lageplanService.getNewestLageplanForGivenMessstelleId(mstId).orElseGet(() -> new DocumentDto(""));
        DocumentDto expected = new DocumentDto(presignedUrl);
        Assertions.assertEquals(expected, result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilesWithPrefix(bucket, parentFolder, true);
        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getPresignedUrl(
                        eq(fileReference),
                        eq(PresignedUrl.Action.GET),
                        eq(expiration));
    }

    @Test
    void testGetNewestLageplanForGivenMessstelleId_WithExistingMultipleFiles() throws S3Exception, MalformedURLException {

        final String mstId = "4001";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;
        final String presignedUrl = "https://the-presigned-url-to-file.pdf";

        final var fileMetadata1 = new FileMetadata(
                parentFolder + mstId + "1.pdf",
                999L,
                "etag",
                createInstant(2025, 1, 1, 12, 0, 0));
        final var fileMetadata2 = new FileMetadata(
                parentFolder + mstId + "2.pdf",
                999L,
                "etag",
                createInstant(2025, 1, 1, 13, 0, 0));
        final var fileMetadata3 = new FileMetadata(
                parentFolder + mstId + "3.pdf",
                999L,
                "etag",
                createInstant(2025, 1, 1, 11, 0, 0));

        ListResult listResult = new ListResult(List.of(fileMetadata1, fileMetadata2, fileMetadata3), List.of(parentFolder), false, null);
        Mockito.when(s3Adapter.getFilesWithPrefix(bucket, parentFolder, true)).thenReturn(listResult);

        FileReference fileReference = new FileReference(bucket, fileMetadata2.path());
        final Duration expiration = Duration.ofMinutes(EXPIRATION);
        PresignedUrl presignedUrlObj = new PresignedUrl(new URL(presignedUrl), fileMetadata2.path(), PresignedUrl.Action.GET);
        Mockito.when(s3Adapter.getPresignedUrl(fileReference, PresignedUrl.Action.GET, expiration)).thenReturn(presignedUrlObj);

        DocumentDto result = lageplanService.getNewestLageplanForGivenMessstelleId(mstId).orElseGet(() -> new DocumentDto(""));
        DocumentDto expected = new DocumentDto(presignedUrl);
        Assertions.assertEquals(expected, result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilesWithPrefix(bucket, parentFolder, true);
        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getPresignedUrl(
                        eq(fileReference),
                        eq(PresignedUrl.Action.GET),
                        eq(expiration));
    }

    @Test
    void testGetNewestLageplanForGivenMessstelleId_WithMissingFile() throws S3Exception {

        final String mstId = "4001";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        Mockito.when(s3Adapter.getFilesWithPrefix(bucket, parentFolder, true)).thenReturn(new ListResult(List.of(), List.of(), false, null));

        Assertions.assertTrue(
                lageplanService.getNewestLageplanForGivenMessstelleId(mstId).isEmpty());
        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilesWithPrefix(bucket, parentFolder, true);
        Mockito
                .verify(s3Adapter, Mockito.never())
                .getPresignedUrl(
                        any(FileReference.class),
                        any(PresignedUrl.Action.class),
                        any(Duration.class));
    }

    @Test
    void testLageplanForGivenMessstelleIdExists_WithExistingFile() throws S3Exception {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        final var fileMetadata1 = new FileMetadata(
                parentFolder + "file1.pdf",
                999L,
                "etag",
                createInstant(2025, 1, 1, 12, 0, 0));

        ListResult listResult = new ListResult(List.of(fileMetadata1), List.of(parentFolder), false, null);
        Mockito.when(s3Adapter.getFilesWithPrefix(bucket, parentFolder, true)).thenReturn(listResult);

        Assertions.assertTrue(lageplanService.lageplanForGivenMessstelleIdExists(mstId, parentFolder).isPresent());

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilesWithPrefix(bucket, parentFolder, true);
    }

    @Test
    void testLageplanForGivenMessstelleIdExists_WithMissingFiles() throws S3Exception {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        Mockito.when(s3Adapter.getFilesWithPrefix(bucket, parentFolder, true)).thenReturn(new ListResult(List.of(), List.of(), false, null));

        Assertions.assertFalse(lageplanService.lageplanForGivenMessstelleIdExists(mstId, parentFolder).isPresent());

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilesWithPrefix(bucket, parentFolder, true);
    }

    @Test
    void testGetFilePathOfNewestFileInFolderAndSubfolder_WithExistingFile() throws S3Exception {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        final var fileMetadata1 = new FileMetadata(
                parentFolder + "file1.pdf",
                999L,
                "etag",
                createInstant(2025, 1, 1, 12, 0, 0));

        ListResult listResult = new ListResult(List.of(fileMetadata1), List.of(parentFolder), false, null);
        Mockito.when(s3Adapter.getFilesWithPrefix(bucket, parentFolder, true)).thenReturn(listResult);

        FileReference fileReference = new FileReference(bucket, parentFolder);
        final var result = lageplanService.getFilePathOfNewestFileInFolderAndSubfolder(fileReference);

        Assertions.assertEquals(Optional.of(new FileReference(bucket, fileMetadata1.path())), result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilesWithPrefix(bucket, parentFolder, true);
    }

    @Test
    void testGetFilePathOfNewestFileInFolderAndSubfolder_WithExistingMultipleFiles() throws S3Exception {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        final var fileMetadata1 = new FileMetadata(
                parentFolder + "file1.pdf",
                999L,
                "etag",
                createInstant(2025, 1, 1, 12, 0, 0));
        final var fileMetadata2 = new FileMetadata(
                parentFolder + "file2.pdf",
                999L,
                "etag",
                createInstant(2025, 1, 1, 13, 0, 0));
        final var fileMetadata3 = new FileMetadata(
                parentFolder + "file3.pdf",
                999L,
                "etag",
                createInstant(2025, 1, 1, 11, 0, 0));

        ListResult listResult = new ListResult(List.of(fileMetadata1, fileMetadata2, fileMetadata3), List.of(parentFolder), false, null);
        Mockito.when(s3Adapter.getFilesWithPrefix(bucket, parentFolder, true)).thenReturn(listResult);

        FileReference fileReference = new FileReference(bucket, parentFolder);
        final var result = lageplanService.getFilePathOfNewestFileInFolderAndSubfolder(fileReference);

        Assertions.assertEquals(Optional.of(new FileReference(bucket, parentFolder + "file2.pdf")), result);

        Mockito.verify(s3Adapter, Mockito.times(1))
                .getFilesWithPrefix(bucket, parentFolder, true);
    }

    @Test
    void testGetFilePathOfNewestFileInFolderAndSubfolder_WithMissingFiles() throws S3Exception {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        Mockito.when(s3Adapter.getFilesWithPrefix(bucket, parentFolder, true)).thenReturn(new ListResult(List.of(), List.of(), false, null));

        FileReference fileReference = new FileReference(bucket, parentFolder);
        final var result = lageplanService.getFilePathOfNewestFileInFolderAndSubfolder(fileReference);

        Assertions.assertEquals(Optional.empty(), result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilesWithPrefix(bucket, parentFolder, true);
    }

    private Instant createInstant(int year, int month, int day, int hour, int minute, int second) {
        return LocalDateTime.of(year, month, day, hour, minute, second).atZone(ZoneId.of("UTC")).toInstant();
    }
}
