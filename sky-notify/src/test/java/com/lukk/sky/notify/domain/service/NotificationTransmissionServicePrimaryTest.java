package com.lukk.sky.notify.domain.service;

import com.lukk.sky.common.kafka.KafkaPayloadModel;
import com.lukk.sky.notify.domain.ports.NotificationPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("NotificationTransmissionServicePrimary: routing through the outbound port")
@ExtendWith(MockitoExtension.class)
class NotificationTransmissionServicePrimaryTest {

    private static final String PARTITION = "1";
    private static final String TOPIC = "sky.offer";
    private static final String GROUP_ID = "skyGroup";
    private static final String TIMESTAMP = "2201-06-20T16:35:47";
    private static final String OFFSET = "7";

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private NotificationTransmissionServicePrimary service;

    @Test
    @DisplayName("publishes through the port to the user carried by the payload")
    void notifyClient_whenPayloadCarriesUser_thenPublishesThroughPortToThatUser() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("offer-data", TIMESTAMP, "user@test.com");

        // when
        service.notifyClient(payload, PARTITION, TOPIC, GROUP_ID, TIMESTAMP, OFFSET);

        // then
        verify(notificationPublisher).publish("user@test.com", payload, PARTITION, TOPIC, GROUP_ID, TIMESTAMP, OFFSET);
    }

    @Test
    @DisplayName("drops the event when the payload carries no user")
    void notifyClient_whenUserInfoMissing_thenDropsWithoutPublishing() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("offer-data", TIMESTAMP, null);

        // when
        service.notifyClient(payload, PARTITION, TOPIC, GROUP_ID, TIMESTAMP, OFFSET);

        // then
        verifyNoInteractions(notificationPublisher);
    }

    @Test
    @DisplayName("drops the event when the payload carries a blank user")
    void notifyClient_whenUserInfoBlank_thenDropsWithoutPublishing() {
        // given
        KafkaPayloadModel payload = new KafkaPayloadModel("offer-data", TIMESTAMP, "   ");

        // when
        service.notifyClient(payload, PARTITION, TOPIC, GROUP_ID, TIMESTAMP, OFFSET);

        // then
        verify(notificationPublisher, never()).publish(anyString(), any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("drops the event when there is no payload at all")
    void notifyClient_whenPayloadNull_thenDropsWithoutPublishing() {
        // when
        service.notifyClient(null, PARTITION, TOPIC, GROUP_ID, TIMESTAMP, OFFSET);

        // then
        verifyNoInteractions(notificationPublisher);
    }
}
