package com.lukk.sky.offer.adapters.outbound.storage;

import com.lukk.sky.offer.config.propertyBind.S3Properties;
import com.lukk.sky.offer.domain.exception.OfferException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    @DisplayName("upload_whenValidArgs_thenCallsPutObjectWithCorrectParams")
    void upload_whenValidArgs_thenCallsPutObjectWithCorrectParams() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        String key = photoStorage.upload(stream, bytes.length, "image/jpeg", "hotel.jpg");

        assertThat(key).startsWith("offers/").endsWith("-hotel.jpg");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest captured = captor.getValue();
        assertThat(captured.bucket()).isEqualTo(BUCKET);
        assertThat(captured.key()).isEqualTo(key);
        assertThat(captured.contentType()).isEqualTo("image/jpeg");
        assertThat(captured.contentLength()).isEqualTo(bytes.length);
    }

    @Test
    @DisplayName("upload_whenFilenameHasSpecialChars_thenSanitizesFilename")
    void upload_whenFilenameHasSpecialChars_thenSanitizesFilename() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        String key = photoStorage.upload(stream, bytes.length, "image/png", "my hotel & spa.png");

        assertThat(key).endsWith("-my_hotel___spa.png");
    }

    @Test
    @DisplayName("upload_whenFilenameBlank_thenUsesDefaultPhotoSuffix")
    void upload_whenFilenameBlank_thenUsesDefaultPhotoSuffix() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        String key = photoStorage.upload(stream, bytes.length, "image/jpeg", "   ");

        assertThat(key).endsWith("-photo");
    }

    @Test
    @DisplayName("upload_whenS3Throws_thenRethrowsAsOfferException")
    void upload_whenS3Throws_thenRethrowsAsOfferException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkException.create("S3 unavailable", new RuntimeException()));

        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        assertThatThrownBy(() -> photoStorage.upload(stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(OfferException.class)
                .hasMessageContaining("Photo upload failed");
    }

    @Test
    @DisplayName("presignedUrl_whenValidKey_thenReturnsUrl")
    void presignedUrl_whenValidKey_thenReturnsUrl() throws MalformedURLException {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(
                URI.create("http://localhost:9000/sky-offers-test/offers/abc.jpg?X-Amz-Signature=sig").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = photoStorage.presignedUrl("offers/abc.jpg");

        assertThat(url).contains("offers/abc.jpg");
        assertThat(url).contains("X-Amz-Signature=sig");
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("presignedUrl_whenKeyIsNull_thenReturnsNull")
    void presignedUrl_whenKeyIsNull_thenReturnsNull() {
        String url = photoStorage.presignedUrl(null);

        assertThat(url).isNull();
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("presignedUrl_whenKeyIsBlank_thenReturnsNull")
    void presignedUrl_whenKeyIsBlank_thenReturnsNull() {
        String url = photoStorage.presignedUrl("   ");

        assertThat(url).isNull();
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("presignedUrl_whenS3Throws_thenRethrowsAsOfferException")
    void presignedUrl_whenS3Throws_thenRethrowsAsOfferException() {
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(SdkException.create("S3 presign error", new RuntimeException()));

        assertThatThrownBy(() -> photoStorage.presignedUrl("offers/abc.jpg"))
                .isInstanceOf(OfferException.class)
                .hasMessageContaining("Presign URL generation failed");
    }
}
