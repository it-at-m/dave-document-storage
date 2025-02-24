package de.muenchen.dave.lageplaene.domain.service;

import de.muenchen.dave.errorhandling.ResourceNotFoundException;
import de.muenchen.dave.lageplaene.api.dto.DocumentDto;
import de.muenchen.refarch.integration.s3.adapter.out.s3.S3Adapter;
import de.muenchen.refarch.integration.s3.domain.exception.FileSystemAccessException;
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

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
    void testGetUrl_WithExistingFile() throws FileSystemAccessException, ResourceNotFoundException {

        final String mstId = "4000";
        final String presignedUrl = "https://" + mstId + ".pdf";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;
        final String filePath = BASE_PATH + mstId + LageplanService.SEPARATOR + mstId + ".pdf";

        Mockito.when(s3Adapter.getFilePathsFromFolder(parentFolder)).thenReturn(Set.of(parentFolder, filePath));
        Mockito.when(s3Adapter.getPresignedUrl(filePath, Method.GET, EXPIRATION)).thenReturn(presignedUrl);

        DocumentDto result = lageplanService.getLageplan(mstId);

        assertThat(result, notNullValue());
        assertThat(result.getUrl(), is(presignedUrl));

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilePathsFromFolder(parentFolder);
        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getPresignedUrl(
                        filePath,
                        Method.GET,
                        EXPIRATION);

    }

    @Test
    void testGetUrl_WithExistingMultipleFiles() throws FileSystemAccessException, ResourceNotFoundException {

        final String mstId = "4001";
        final String presignedUrl = "https://" + mstId + ".pdf";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;
        final String filePath = BASE_PATH + mstId + LageplanService.SEPARATOR + mstId + ".pdf";

        Mockito.when(s3Adapter.getFilePathsFromFolder(parentFolder)).thenReturn(Set.of(parentFolder, filePath, parentFolder + mstId + ".png"));
        Mockito.when(s3Adapter.getPresignedUrl(filePath, Method.GET, EXPIRATION)).thenReturn(presignedUrl);
        Mockito.when(s3Adapter.getPresignedUrl(parentFolder + mstId + ".png", Method.GET, EXPIRATION)).thenReturn("https://" + mstId + ".png");

        DocumentDto result = lageplanService.getLageplan(mstId);

        assertThat(result, notNullValue());
        assertThat(result.getUrl(), notNullValue());

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilePathsFromFolder(parentFolder);
        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getPresignedUrl(
                        anyString(),
                        eq(Method.GET),
                        eq(EXPIRATION));

    }

    @Test
    void testGetUrl_WithMissingFile() throws FileSystemAccessException {

        final String mstId = "4002";
        final String parentFolder = BASE_PATH + mstId + LageplanService.SEPARATOR;
        final String filePath = BASE_PATH + mstId + LageplanService.SEPARATOR + mstId + ".pdf";

        Mockito.when(s3Adapter.getFilePathsFromFolder(parentFolder)).thenReturn(Collections.emptySet());

        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> lageplanService.getLageplan(mstId),
                "Kein Dokument gefunden: " + parentFolder);

        Mockito
                .verify(s3Adapter, Mockito.times(1))
                .getFilePathsFromFolder(parentFolder);
        Mockito
                .verify(s3Adapter, Mockito.never())
                .getPresignedUrl(
                        filePath,
                        Method.GET,
                        EXPIRATION);

    }
}
