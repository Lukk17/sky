package com.lukk.sky.notify.adapters.inbound;

import com.lukk.sky.notify.domain.service.NotificationTransmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static com.lukk.sky.notify.config.Constants.KAFKA_BOOKING_TOPIC;
import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class KafkaListenersTest {

    public static final String TEST_PARTITION = "1";
    public static final String TEST_OFFSET = "0";
    public static final String TEST_CONSUMER_GROUP_ID = "first-consumer-group";
    public static LocalDateTime TEST_DATE = LocalDateTime.of(2201, 6, 20, 16, 35, 47);

    @Mock
    NotificationTransmissionService notificationTransmissionService;

    @Mock
    Acknowledgment acknowledgment;

    @InjectMocks
    KafkaListeners kafkaListeners;

    @Test
    public void testOfferListener_acksAfterSuccessfulNotify() {
        String offerMessage = "This is an offer message.";
        kafkaListeners.offerListener(offerMessage, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET, acknowledgment);

        verify(notificationTransmissionService).notifyClient(offerMessage, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);
        verify(acknowledgment).acknowledge();
    }

    @Test
    public void testBookingListener_acksAfterSuccessfulNotify() {
        String bookingMessage = "This is a booking message.";
        kafkaListeners.bookingListener(bookingMessage, TEST_PARTITION, KAFKA_BOOKING_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET, acknowledgment);

        verify(notificationTransmissionService).notifyClient(bookingMessage, TEST_PARTITION, KAFKA_BOOKING_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);
        verify(acknowledgment).acknowledge();
    }

    @Test
    public void testOfferListener_doesNotAckIfNotifyThrows() {
        String offerMessage = "boom";
        doThrow(new RuntimeException("ws failed"))
                .when(notificationTransmissionService)
                .notifyClient(offerMessage, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                        TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);

        try {
            kafkaListeners.offerListener(offerMessage, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                    TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET, acknowledgment);
        } catch (RuntimeException expected) {
            // The container's DefaultErrorHandler is what actually retries + routes to DLT;
            // here we only verify the listener itself never acked the failed message.
        }
        verify(acknowledgment, never()).acknowledge();
    }
}
