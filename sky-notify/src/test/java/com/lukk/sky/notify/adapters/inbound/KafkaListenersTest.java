package com.lukk.sky.notify.adapters.inbound;

import com.lukk.sky.notify.domain.service.NotificationTransmissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static com.lukk.sky.notify.config.Constants.KAFKA_BOOKING_TOPIC;
import static com.lukk.sky.notify.config.Constants.KAFKA_OFFER_TOPIC;
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

    @InjectMocks
    KafkaListeners kafkaListeners;

    @Test
    public void testOfferListener() {
        String offerMessage = "This is an offer message.";
        kafkaListeners.offerListener(offerMessage, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);

        // Assert that the notification was sent to the notification service.
        verify(notificationTransmissionService).notifyClient(offerMessage, TEST_PARTITION, KAFKA_OFFER_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);
    }

    @Test
    public void testBookingListener() {
        String bookingMessage = "This is a booking message.";
        kafkaListeners.bookingListener(bookingMessage, TEST_PARTITION, KAFKA_BOOKING_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);

        // Assert that the notification was sent to the notification service.
        verify(notificationTransmissionService).notifyClient(bookingMessage, TEST_PARTITION, KAFKA_BOOKING_TOPIC,
                TEST_CONSUMER_GROUP_ID, TEST_DATE.toString(), TEST_OFFSET);
    }
}
