package de.muenchen.dave.lageplaene.domain.service;

import de.muenchen.dave.errorhandling.ResourceNotFoundException;
import de.muenchen.dave.lageplaene.api.dto.DocumentDto;
import de.muenchen.refarch.integration.s3.adapter.out.s3.S3Adapter;
import de.muenchen.refarch.integration.s3.domain.exception.FileSystemAccessException;
import de.muenchen.refarch.integration.s3.domain.model.FileMetadata;
import io.minio.http.Method;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LageplanServiceTest {

    private static final String BASE_PATH = "DAVe/Messstellen/Lageplaene/";
    private static final Integer EXPIRATION = 30;

    @Mock
    private S3Adapter s3Adapter;

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
    void testGetNewestLageplanForGivenMessstelleId_WithExistingFile() throws FileSystemAccessException, ResourceNotFoundException {

        final String mstId = "4001";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;
        final String presignedUrl = "https://the-presigned-url-to-file.pdf";

        final var fileMetadata1 = new FileMetadata(
                parentFolder + mstId + "1.pdf",
                999L,
                "etag",
                LocalDateTime.of(2025, 1, 1, 12, 0, 0));

        Mockito.when(s3Adapter.getMetadataOfFilesFromFolder(parentFolder)).thenReturn(List.of(fileMetadata1));
        Mockito.when(s3Adapter.getPresignedUrl(parentFolder + mstId + "1.pdf", Method.GET, EXPIRATION)).thenReturn(presignedUrl);

        DocumentDto result = lageplanService.getNewestLageplanForGivenMessstelleId(mstId);
        DocumentDto expected = new DocumentDto(presignedUrl);
        Assertions.assertEquals(expected, result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getMetadataOfFilesFromFolder(parentFolder);
        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getPresignedUrl(
                        eq(parentFolder + mstId + "1.pdf"),
                        eq(Method.GET),
                        eq(EXPIRATION));
    }

    @Test
    void testGetNewestLageplanForGivenMessstelleId_WithExistingMultipleFiles() throws FileSystemAccessException, ResourceNotFoundException {

        final String mstId = "4001";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;
        final String presignedUrl = "https://the-presigned-url-to-file.pdf";

        final var fileMetadata1 = new FileMetadata(
                parentFolder + mstId + "1.pdf",
                999L,
                "etag",
                LocalDateTime.of(2025, 1, 1, 12, 0, 0));
        final var fileMetadata2 = new FileMetadata(
                parentFolder + mstId + "2.pdf",
                999L,
                "etag",
                LocalDateTime.of(2025, 1, 1, 13, 0, 0));
        final var fileMetadata3 = new FileMetadata(
                parentFolder + mstId + "3.pdf",
                999L,
                "etag",
                LocalDateTime.of(2025, 1, 1, 11, 0, 0));

        Mockito.when(s3Adapter.getMetadataOfFilesFromFolder(parentFolder)).thenReturn(List.of(fileMetadata1, fileMetadata2, fileMetadata3));
        Mockito.when(s3Adapter.getPresignedUrl(parentFolder + mstId + "2.pdf", Method.GET, EXPIRATION)).thenReturn(presignedUrl);

        DocumentDto result = lageplanService.getNewestLageplanForGivenMessstelleId(mstId);
        DocumentDto expected = new DocumentDto(presignedUrl);
        Assertions.assertEquals(expected, result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getMetadataOfFilesFromFolder(parentFolder);
        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getPresignedUrl(
                        eq(parentFolder + mstId + "2.pdf"),
                        eq(Method.GET),
                        eq(EXPIRATION));
    }

    @Test
    void testGetNewestLageplanForGivenMessstelleId_WithMissingFile() throws FileSystemAccessException {

        final String mstId = "4001";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        Mockito.when(s3Adapter.getMetadataOfFilesFromFolder(parentFolder)).thenReturn(List.of());

        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> lageplanService.getNewestLageplanForGivenMessstelleId(mstId),
                "Kein Dokument gefunden: " + parentFolder);
        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getMetadataOfFilesFromFolder(parentFolder);
        Mockito
                .verify(s3Adapter, Mockito.never())
                .getPresignedUrl(
                        anyString(),
                        any(Method.class),
                        any(Integer.class));
    }

    @Test
    void testLageplanForGivenMessstelleIdExists_WithExistingFile() throws FileSystemAccessException {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        final var fileMetadata1 = new FileMetadata(
                parentFolder + "file1.pdf",
                999L,
                "etag",
                LocalDateTime.of(2025, 1, 1, 12, 0, 0));

        Mockito.when(s3Adapter.getMetadataOfFilesFromFolder(parentFolder)).thenReturn(List.of(fileMetadata1));

        final var result = lageplanService.lageplanForGivenMessstelleIdExists(mstId);

        Assertions.assertTrue(result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getMetadataOfFilesFromFolder(parentFolder);
    }

    @Test
    void testLageplanForGivenMessstelleIdExists_WithMissingFiles() throws FileSystemAccessException {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        Mockito.when(s3Adapter.getMetadataOfFilesFromFolder(parentFolder)).thenReturn(List.of());

        final var result = lageplanService.lageplanForGivenMessstelleIdExists(mstId);

        Assertions.assertFalse(result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getMetadataOfFilesFromFolder(parentFolder);
    }

    @Test
    void testGetFilePathOfNewestFileInFolderAndSubfolder_WithExistingFile() throws FileSystemAccessException {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        final var fileMetadata1 = new FileMetadata(
                parentFolder + "file1.pdf",
                999L,
                "etag",
                LocalDateTime.of(2025, 1, 1, 12, 0, 0));

        Mockito.when(s3Adapter.getMetadataOfFilesFromFolder(parentFolder)).thenReturn(List.of(fileMetadata1));

        final var result = lageplanService.getFilePathOfNewestFileInFolderAndSubfolder(parentFolder);

        Assertions.assertEquals(Optional.of(parentFolder + "file1.pdf"), result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getMetadataOfFilesFromFolder(parentFolder);
    }

    @Test
    void testGetFilePathOfNewestFileInFolderAndSubfolder_WithExistingMultipleFiles() throws FileSystemAccessException {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        final var fileMetadata1 = new FileMetadata(
                parentFolder + "file1.pdf",
                999L,
                "etag",
                LocalDateTime.of(2025, 1, 1, 12, 0, 0));
        final var fileMetadata2 = new FileMetadata(
                parentFolder + "file2.pdf",
                999L,
                "etag",
                LocalDateTime.of(2025, 1, 1, 13, 0, 0));
        final var fileMetadata3 = new FileMetadata(
                parentFolder + "file3.pdf",
                999L,
                "etag",
                LocalDateTime.of(2025, 1, 1, 11, 0, 0));

        Mockito.when(s3Adapter.getMetadataOfFilesFromFolder(parentFolder)).thenReturn(List.of(fileMetadata1, fileMetadata2, fileMetadata3));

        final var result = lageplanService.getFilePathOfNewestFileInFolderAndSubfolder(parentFolder);

        Assertions.assertEquals(Optional.of(parentFolder + "file2.pdf"), result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getMetadataOfFilesFromFolder(parentFolder);
    }

    @Test
    void testGetFilePathOfNewestFileInFolderAndSubfolder_WithMissingFiles() throws FileSystemAccessException {
        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;

        Mockito.when(s3Adapter.getMetadataOfFilesFromFolder(parentFolder)).thenReturn(List.of());

        final var result = lageplanService.getFilePathOfNewestFileInFolderAndSubfolder(parentFolder);

        Assertions.assertEquals(Optional.empty(), result);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getMetadataOfFilesFromFolder(parentFolder);
    }
}
