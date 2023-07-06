package com.lukk.sky.notify.domain.service;

public interface NotificationTransmissionService {

    void notifyClient(String message, String partition, String topic, String groupId, String timestamp, String offset);
}
