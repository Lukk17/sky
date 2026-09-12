package com.lukk.sky.offer.adapters.outbound.storage;

import com.lukk.sky.offer.config.propertyBind.S3Properties;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.PhotoStorageBadResponseException;
import com.lukk.sky.offer.domain.exception.PhotoStorageUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("S3PhotoStorage unit tests: no real S3 endpoint, all AWS SDK calls mocked")
@ExtendWith(MockitoExtension.class)
class S3PhotoStorageTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3PhotoStorage photoStorage;

    private static final String BUCKET = "sky-offers-test";
    private static final UUID OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String OFFER_PREFIX = "offers/" + OFFER_ID + "/";

    @BeforeEach
    void setUp() {
        S3Properties props = new S3Properties(
                "http://localhost:9000",
                "",
                "us-east-1",
                BUCKET,
                "test-access-key",
                "test-secret-key",
                true,
                Duration.ofMinutes(15)
        );
        photoStorage = new S3PhotoStorage(s3Client, s3Presigner, props);
    }

    @Test
    @DisplayName("upload_whenValidArgs_thenCallsPutObjectWithCorrectParams")
    void upload_whenValidArgs_thenCallsPutObjectWithCorrectParams() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when
        String key = photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg");

        // then
        assertThat(key).startsWith(OFFER_PREFIX).endsWith("-hotel.jpg");
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
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when
        String key = photoStorage.upload(OFFER_ID, stream, bytes.length, "image/png", "my hotel & spa.png");

        // then
        assertThat(key).endsWith("-my_hotel___spa.png");
    }

    @Test
    @DisplayName("upload_whenFilenameBlank_thenUsesDefaultPhotoSuffix")
    void upload_whenFilenameBlank_thenUsesDefaultPhotoSuffix() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when
        String key = photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "   ");

        // then
        assertThat(key).endsWith("-photo");
    }

    @Test
    @DisplayName("upload_whenFilenameIsNull_thenUsesDefaultPhotoSuffix")
    void upload_whenFilenameIsNull_thenUsesDefaultPhotoSuffix() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when
        String key = photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", null);

        // then
        assertThat(key).startsWith(OFFER_PREFIX).endsWith("-photo");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().key()).isEqualTo(key);
    }

    @Test
    @DisplayName("upload_whenTheStoreRefusesTheConnection_thenThrowsPhotoStorageUnavailableException")
    void upload_whenTheStoreRefusesTheConnection_thenThrowsPhotoStorageUnavailableException() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.builder()
                        .message("Unable to execute HTTP request: Connection refused")
                        .cause(new ConnectException("Connection refused"))
                        .build());
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when / then
        assertThatThrownBy(() -> photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(PhotoStorageUnavailableException.class)
                .isNotInstanceOf(OfferException.class)
                .hasMessage("Photo upload failed. The object store is unavailable.");
    }

    @Test
    @DisplayName("upload_whenTheConnectionTimesOut_thenThrowsPhotoStorageUnavailableException")
    void upload_whenTheConnectionTimesOut_thenThrowsPhotoStorageUnavailableException() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.builder()
                        .message("Unable to execute HTTP request: connect timed out")
                        .cause(new SocketTimeoutException("connect timed out"))
                        .build());
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when / then
        assertThatThrownBy(() -> photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(PhotoStorageUnavailableException.class)
                .hasMessage("Photo upload failed. The object store is unavailable.");
    }

    @Test
    @DisplayName("upload_whenTheReadTimesOut_thenThrowsPhotoStorageUnavailableException")
    void upload_whenTheReadTimesOut_thenThrowsPhotoStorageUnavailableException() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.builder()
                        .message("Unable to execute HTTP request: Read timed out")
                        .cause(new SocketTimeoutException("Read timed out"))
                        .build());
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when / then
        assertThatThrownBy(() -> photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(PhotoStorageUnavailableException.class)
                .hasMessage("Photo upload failed. The object store is unavailable.");
    }

    @Test
    @DisplayName("upload_whenTheStoreAnswers500_thenThrowsPhotoStorageUnavailableException")
    void upload_whenTheStoreAnswers500_thenThrowsPhotoStorageUnavailableException() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(s3Error(500, "InternalError", "We encountered an internal error."));
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when / then
        assertThatThrownBy(() -> photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(PhotoStorageUnavailableException.class)
                .hasMessage("Photo upload failed. The object store is unavailable.");
    }

    @Test
    @DisplayName("upload_whenTheStoreThrottlesWith429_thenThrowsPhotoStorageUnavailableException")
    void upload_whenTheStoreThrottlesWith429_thenThrowsPhotoStorageUnavailableException() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(s3Error(429, "SlowDown", "Please reduce your request rate."));
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when / then
        assertThatThrownBy(() -> photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(PhotoStorageUnavailableException.class)
                .hasMessage("Photo upload failed. The object store is unavailable.");
    }

    @Test
    @DisplayName("upload_whenTheStoreAnswers403_thenThrowsPhotoStorageBadResponseException")
    void upload_whenTheStoreAnswers403_thenThrowsPhotoStorageBadResponseException() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(s3Error(403, "AccessDenied", "Access Denied"));
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when / then
        assertThatThrownBy(() -> photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(PhotoStorageBadResponseException.class)
                .isNotInstanceOf(OfferException.class)
                .hasMessage("Photo upload failed. The object store refused the request.");
    }

    @Test
    @DisplayName("upload_whenTheStoreAnswers401_thenThrowsPhotoStorageBadResponseException")
    void upload_whenTheStoreAnswers401_thenThrowsPhotoStorageBadResponseException() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(s3Error(401, "InvalidAccessKeyId", "The access key id does not exist."));
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when / then
        assertThatThrownBy(() -> photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(PhotoStorageBadResponseException.class)
                .hasMessage("Photo upload failed. The object store refused the request.");
    }

    @Test
    @DisplayName("upload_whenTheBucketDoesNotExist_thenThrowsPhotoStorageBadResponseException")
    void upload_whenTheBucketDoesNotExist_thenThrowsPhotoStorageBadResponseException() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(NoSuchBucketException.builder()
                        .statusCode(404)
                        .message("The specified bucket does not exist")
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorCode("NoSuchBucket")
                                .serviceName("S3")
                                .build())
                        .build());
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when / then
        assertThatThrownBy(() -> photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(PhotoStorageBadResponseException.class)
                .hasMessage("Photo upload failed. The object store refused the request.");
    }

    @Test
    @DisplayName("upload_whenTheSdkFailsWithoutAnswerOrTransport_thenThrowsPhotoStorageUnavailableException")
    void upload_whenTheSdkFailsWithoutAnswerOrTransport_thenThrowsPhotoStorageUnavailableException() {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkException.create("S3 unavailable", new RuntimeException()));
        byte[] bytes = "bytes".getBytes();
        InputStream stream = new ByteArrayInputStream(bytes);

        // when / then
        assertThatThrownBy(() -> photoStorage.upload(OFFER_ID, stream, bytes.length, "image/jpeg", "hotel.jpg"))
                .isInstanceOf(PhotoStorageUnavailableException.class)
                .hasMessage("Photo upload failed. The object store is unavailable.");
    }

    @Test
    @DisplayName("presignedUrl_whenValidKey_thenReturnsUrl")
    void presignedUrl_whenValidKey_thenReturnsUrl() throws MalformedURLException {
        // given
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(
                URI.create("http://localhost:9000/sky-offers-test/offers/abc.jpg?X-Amz-Signature=sig").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        // when
        String url = photoStorage.presignedUrl("offers/abc.jpg");

        // then
        assertThat(url).contains("offers/abc.jpg");
        assertThat(url).contains("X-Amz-Signature=sig");
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("presignedUrl_whenKeyIsNull_thenReturnsNull")
    void presignedUrl_whenKeyIsNull_thenReturnsNull() {
        // when
        String url = photoStorage.presignedUrl(null);

        // then
        assertThat(url).isNull();
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("presignedUrl_whenKeyIsBlank_thenReturnsNull")
    void presignedUrl_whenKeyIsBlank_thenReturnsNull() {
        // when
        String url = photoStorage.presignedUrl("   ");

        // then
        assertThat(url).isNull();
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("presignedUrl_whenTheSignerFails_thenThrowsPhotoStorageUnavailableException")
    void presignedUrl_whenTheSignerFails_thenThrowsPhotoStorageUnavailableException() {
        // given
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenThrow(SdkException.create("S3 presign error", new RuntimeException()));

        // when / then
        assertThatThrownBy(() -> photoStorage.presignedUrl("offers/abc.jpg"))
                .isInstanceOf(PhotoStorageUnavailableException.class)
                .isNotInstanceOf(OfferException.class)
                .hasMessage("Photo address could not be signed. The object store is unavailable.");
    }

    @Test
    @DisplayName("delete_whenKeyBelongsToThatOffer_thenCallsDeleteObjectForThatBucketAndKey")
    void delete_whenKeyBelongsToThatOffer_thenCallsDeleteObjectForThatBucketAndKey() {
        // given
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());
        String key = OFFER_PREFIX + "abc.jpg";

        // when
        photoStorage.delete(OFFER_ID, key);

        // then
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().key()).isEqualTo(key);
    }

    @Test
    @DisplayName("delete_whenKeyBelongsToAnotherOffer_thenDoesNotCallS3")
    void delete_whenKeyBelongsToAnotherOffer_thenDoesNotCallS3() {
        // when
        photoStorage.delete(OFFER_ID, "offers/" + OTHER_OFFER_ID + "/victim.png");

        // then
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("delete_whenKeyWasNotStoredByThisService_thenDoesNotCallS3")
    void delete_whenKeyWasNotStoredByThisService_thenDoesNotCallS3() {
        // when
        photoStorage.delete(OFFER_ID, "https://images.example.com/photo.jpeg");

        // then
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("delete_whenKeyIsNull_thenDoesNotCallS3")
    void delete_whenKeyIsNull_thenDoesNotCallS3() {
        // when
        photoStorage.delete(OFFER_ID, null);

        // then
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("delete_whenKeyIsBlank_thenDoesNotCallS3")
    void delete_whenKeyIsBlank_thenDoesNotCallS3() {
        // when
        photoStorage.delete(OFFER_ID, "   ");

        // then
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("delete_whenTheStoreIsUnreachable_thenThrowsPhotoStorageUnavailableException")
    void delete_whenTheStoreIsUnreachable_thenThrowsPhotoStorageUnavailableException() {
        // given
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.builder()
                        .message("Unable to execute HTTP request: Connection refused")
                        .cause(new ConnectException("Connection refused"))
                        .build());

        // when / then
        assertThatThrownBy(() -> photoStorage.delete(OFFER_ID, OFFER_PREFIX + "abc.jpg"))
                .isInstanceOf(PhotoStorageUnavailableException.class)
                .isNotInstanceOf(OfferException.class)
                .hasMessage("Photo delete failed. The object store is unavailable.");
    }

    @Test
    @DisplayName("delete_whenTheStoreAnswers403_thenThrowsPhotoStorageBadResponseException")
    void delete_whenTheStoreAnswers403_thenThrowsPhotoStorageBadResponseException() {
        // given
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(s3Error(403, "AccessDenied", "Access Denied"));

        // when / then
        assertThatThrownBy(() -> photoStorage.delete(OFFER_ID, OFFER_PREFIX + "abc.jpg"))
                .isInstanceOf(PhotoStorageBadResponseException.class)
                .hasMessage("Photo delete failed. The object store refused the request.");
    }

    private static S3Exception s3Error(int statusCode, String errorCode, String message) {
        return (S3Exception) S3Exception.builder()
                .statusCode(statusCode)
                .message(message)
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode(errorCode)
                        .serviceName("S3")
                        .build())
                .build();
    }
}
