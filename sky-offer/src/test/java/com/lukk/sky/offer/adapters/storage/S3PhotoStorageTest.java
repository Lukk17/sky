package com.lukk.sky.offer.adapters.storage;

import com.lukk.sky.offer.config.propertyBind.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("S3PhotoStorage unit tests — no real S3 endpoint, all AWS SDK calls mocked")
@ExtendWith(MockitoExtension.class)
class S3PhotoStorageTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3PhotoStorage photoStorage;

    private static final String BUCKET = "sky-offers-test";

    @BeforeEach
    void setUp() {
        S3Properties props = new S3Properties(
                "http://localhost:9000",
                "us-east-1",
                BUCKET,
                "minioadmin",
                "minioadmin",
                true,
                Duration.ofMinutes(15)
        );
        photoStorage = new S3PhotoStorage(s3Client, s3Presigner, props);
    }

    @Test
    @DisplayName("upload() calls S3Client.putObject with correct bucket, key prefix and content-type")
    void upload_whenValidArgs_thenCallsPutObjectWithCorrectParams() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = photoStorage.upload("bytes".getBytes(), "image/jpeg", "hotel.jpg");

        assertThat(key).startsWith("offers/").endsWith("-hotel.jpg");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest captured = captor.getValue();
        assertThat(captured.bucket()).isEqualTo(BUCKET);
        assertThat(captured.key()).isEqualTo(key);
        assertThat(captured.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("upload() sanitizes filename — special characters replaced with underscore")
    void upload_whenFilenameHasSpecialChars_thenSanitizesFilename() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = photoStorage.upload("bytes".getBytes(), "image/png", "my hotel & spa.png");

        assertThat(key).endsWith("-my_hotel___spa.png");
    }

    @Test
    @DisplayName("upload() falls back to 'photo' when filename is blank")
    void upload_whenFilenameBlank_thenUsesDefaultPhotoSuffix() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = photoStorage.upload("bytes".getBytes(), "image/jpeg", "   ");

        assertThat(key).endsWith("-photo");
    }

    @Test
    @DisplayName("presignedUrl() returns presigned URL string for a valid key")
    void presignedUrl_whenValidKey_thenReturnsUrl() throws MalformedURLException {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("http://localhost:9000/sky-offers-test/offers/abc.jpg?X-Amz-Signature=sig").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = photoStorage.presignedUrl("offers/abc.jpg");

        assertThat(url).contains("offers/abc.jpg");
        assertThat(url).contains("X-Amz-Signature=sig");
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("presignedUrl() returns null when key is null")
    void presignedUrl_whenKeyIsNull_thenReturnsNull() {
        String url = photoStorage.presignedUrl(null);

        assertThat(url).isNull();
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("presignedUrl() returns null when key is blank")
    void presignedUrl_whenKeyIsBlank_thenReturnsNull() {
        String url = photoStorage.presignedUrl("   ");

        assertThat(url).isNull();
        verifyNoInteractions(s3Presigner);
    }
}
