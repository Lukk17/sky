package com.lukk.sky.message.domain.ports.inbound;

public record SendMessageCommand(String senderEmail, String receiverEmail, String text) {
}
