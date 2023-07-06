package com.lukk.sky.booking.domain.ports.notification;


import com.lukk.sky.booking.adapters.dto.KafkaPayloadModel;

public interface BookingNotificationService {
    void sendMessage(KafkaPayloadModel message);
}
