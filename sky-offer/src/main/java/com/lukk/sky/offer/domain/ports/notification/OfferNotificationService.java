package com.lukk.sky.offer.domain.ports.notification;

import com.lukk.sky.offer.adapters.dto.KafkaPayloadModel;

public interface OfferNotificationService {
    void sendMessage(KafkaPayloadModel message);
}
