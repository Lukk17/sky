package com.lukk.sky.offer.config;

import com.lukk.sky.offer.config.propertyBind.S3Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("S3Config tests: the client addresses the API endpoint and the presigner the presign endpoint")
class S3ConfigTest {

    private static final String ENDPOINT = "http://floci-service:4566";
    private static final String PRESIGN_ENDPOINT = "http://s3.localhost:5777";
    private static final String BUCKET = "sky-offers";
    private static final String KEY = "offers/canary.png";

    private final S3Config s3Config = new S3Config();

    @Test
    @DisplayName("s3Presigner_whenPathStyleAccessEnabled_thenPresignedUrlCarriesBucketInPathNotHost")
    void s3Presigner_whenPathStyleAccessEnabled_thenPresignedUrlCarriesBucketInPathNotHost() {
        // given
        S3Properties props = properties(true);
        S3Configuration serviceConfiguration = s3Config.s3ServiceConfiguration(props);
        S3Presigner presigner = s3Config.s3Presigner(props, serviceConfiguration);

        // when
        URI url = presignGetObject(presigner);

        // then
        assertThat(url.getHost()).isEqualTo("s3.localhost");
        assertThat(url.getPort()).isEqualTo(5777);
        assertThat(url.getPath()).isEqualTo("/" + BUCKET + "/" + KEY);
        assertThat(url.getQuery()).contains("X-Amz-Signature=");
    }

    @Test
    @DisplayName("s3Presigner_whenPathStyleAccessDisabled_thenPresignedUrlCarriesBucketInHost")
    void s3Presigner_whenPathStyleAccessDisabled_thenPresignedUrlCarriesBucketInHost() {
        // given
        S3Properties props = properties(false);
        S3Configuration serviceConfiguration = s3Config.s3ServiceConfiguration(props);
        S3Presigner presigner = s3Config.s3Presigner(props, serviceConfiguration);

        // when
        URI url = presignGetObject(presigner);

        // then
        assertThat(url.getHost()).isEqualTo(BUCKET + ".s3.localhost");
        assertThat(url.getPath()).isEqualTo("/" + KEY);
    }

    @Test
    @DisplayName("s3Client_whenPathStyleAccessEnabled_thenObjectUrlCarriesBucketInPathNotHost")
    void s3Client_whenPathStyleAccessEnabled_thenObjectUrlCarriesBucketInPathNotHost() {
        // given
        S3Properties props = properties(true);
        S3Configuration serviceConfiguration = s3Config.s3ServiceConfiguration(props);
        S3Client client = s3Config.s3Client(props, serviceConfiguration);

        // when
        URL url = client.utilities().getUrl(GetUrlRequest.builder().bucket(BUCKET).key(KEY).build());

        // then
        assertThat(url.getHost()).isEqualTo("floci-service");
        assertThat(url.getPath()).isEqualTo("/" + BUCKET + "/" + KEY);
    }

    @Test
    @DisplayName("s3Client_whenPathStyleAccessDisabled_thenObjectUrlCarriesBucketInHost")
    void s3Client_whenPathStyleAccessDisabled_thenObjectUrlCarriesBucketInHost() {
        // given
        S3Properties props = properties(false);
        S3Configuration serviceConfiguration = s3Config.s3ServiceConfiguration(props);
        S3Client client = s3Config.s3Client(props, serviceConfiguration);

        // when
        URL url = client.utilities().getUrl(GetUrlRequest.builder().bucket(BUCKET).key(KEY).build());

        // then
        assertThat(url.getHost()).isEqualTo(BUCKET + ".floci-service");
        assertThat(url.getPath()).isEqualTo("/" + KEY);
    }

    @Test
    @DisplayName("s3Presigner_whenPresignEndpointBlank_thenPresignedUrlFallsBackToTheApiEndpoint")
    void s3Presigner_whenPresignEndpointBlank_thenPresignedUrlFallsBackToTheApiEndpoint() {
        // given
        S3Properties props = properties(true, "   ");
        S3Configuration serviceConfiguration = s3Config.s3ServiceConfiguration(props);
        S3Presigner presigner = s3Config.s3Presigner(props, serviceConfiguration);

        // when
        URI url = presignGetObject(presigner);

        // then
        assertThat(url.getHost()).isEqualTo("floci-service");
        assertThat(url.getPort()).isEqualTo(4566);
    }

    @Test
    @DisplayName("s3Presigner_whenPresignEndpointNull_thenPresignedUrlFallsBackToTheApiEndpoint")
    void s3Presigner_whenPresignEndpointNull_thenPresignedUrlFallsBackToTheApiEndpoint() {
        // given
        S3Properties props = properties(true, null);
        S3Configuration serviceConfiguration = s3Config.s3ServiceConfiguration(props);
        S3Presigner presigner = s3Config.s3Presigner(props, serviceConfiguration);

        // when
        URI url = presignGetObject(presigner);

        // then
        assertThat(url.getHost()).isEqualTo("floci-service");
        assertThat(url.getPort()).isEqualTo(4566);
    }

    @Test
    @DisplayName("s3Client_whenPresignEndpointSet_thenTheUploadClientStillTargetsTheApiEndpoint")
    void s3Client_whenPresignEndpointSet_thenTheUploadClientStillTargetsTheApiEndpoint() {
        // given
        S3Properties props = properties(true, PRESIGN_ENDPOINT);
        S3Configuration serviceConfiguration = s3Config.s3ServiceConfiguration(props);
        S3Client client = s3Config.s3Client(props, serviceConfiguration);

        // when
        URL url = client.utilities().getUrl(GetUrlRequest.builder().bucket(BUCKET).key(KEY).build());

        // then
        assertThat(url.getHost()).isEqualTo("floci-service");
        assertThat(url.getPort()).isEqualTo(4566);
    }

    private URI presignGetObject(S3Presigner presigner) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(GetObjectRequest.builder().bucket(BUCKET).key(KEY).build())
                .build();

        return URI.create(presigner.presignGetObject(request).url().toString());
    }

    private static S3Properties properties(boolean pathStyleAccess) {
        return properties(pathStyleAccess, PRESIGN_ENDPOINT);
    }

    private static S3Properties properties(boolean pathStyleAccess, String presignEndpoint) {
        return new S3Properties(
                ENDPOINT,
                presignEndpoint,
                "us-east-1",
                BUCKET,
                "root",
                "localdev",
                pathStyleAccess,
                Duration.ofMinutes(15)
        );
    }
}
