package com.lukk.sky.message.domain.service;

import com.lukk.sky.message.assemblers.MessageAssembler;
import com.lukk.sky.message.domain.exception.MessageAccessDeniedException;
import com.lukk.sky.message.domain.exception.MessageNotFoundException;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.inbound.MessageView;
import com.lukk.sky.message.domain.ports.inbound.SendMessageCommand;
import com.lukk.sky.message.domain.ports.outbound.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;
import static com.lukk.sky.message.assemblers.MessageAssembler.CREATED;
import static com.lukk.sky.message.assemblers.MessageAssembler.IS_READ;
import static com.lukk.sky.message.assemblers.MessageAssembler.RECEIVER_EMAIL;
import static com.lukk.sky.message.assemblers.MessageAssembler.SENDER_EMAIL;
import static com.lukk.sky.message.assemblers.MessageAssembler.TEST_MESSAGE_ID;
import static com.lukk.sky.message.assemblers.MessageAssembler.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("MessageServicePrimary: domain service unit tests")
class MessageServicePrimaryTest {

    @Mock
    MessageRepository messageRepository;

    @Mock
    Clock clock;

    @InjectMocks
    private MessageServicePrimary messageService;

    @Test
    @DisplayName("send() builds the entity from the command, stamps the clock and returns the saved view")
    void send_whenValidCommand_thenPersistAndReturnView() {
        // given
        SendMessageCommand command = MessageAssembler.getSendMessageCommand();
        Message saved = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        ArgumentCaptor<Message> persisted = ArgumentCaptor.forClass(Message.class);
        when(clock.instant()).thenReturn(CREATED);
        when(messageRepository.save(persisted.capture())).thenReturn(saved);

        // when
        MessageView actual = messageService.send(command);

        // then
        Message entity = persisted.getValue();
        assertEquals(SENDER_EMAIL, entity.getSenderEmail());
        assertEquals(RECEIVER_EMAIL, entity.getReceiverEmail());
        assertEquals(TEXT, entity.getText());
        assertEquals(CREATED, entity.getCreatedTime());
        assertFalse(entity.isRead());

        assertEquals(TEST_MESSAGE_ID, actual.id());
        assertEquals(TEXT, actual.text());
        assertEquals(RECEIVER_EMAIL, actual.receiverEmail());
        assertEquals(SENDER_EMAIL, actual.senderEmail());
        assertEquals(DATE_TIME_FORMAT.format(CREATED), actual.createdTime());
        assertEquals(IS_READ, actual.read());
    }

    @Test
    @DisplayName("send() to an address no identity realm holds still persists the message")
    void send_whenReceiverIsUnknownToAnyRealm_thenPersistAndReturnView() {
        // given
        SendMessageCommand command =
                new SendMessageCommand(SENDER_EMAIL, "nobody@test", TEXT);
        Message saved = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        saved.setReceiverEmail("nobody@test");
        when(clock.instant()).thenReturn(CREATED);
        when(messageRepository.save(any())).thenReturn(saved);

        // when
        MessageView actual = messageService.send(command);

        // then
        assertEquals("nobody@test", actual.receiverEmail());
        verify(messageRepository).save(any());
    }

    @Test
    @DisplayName("deleteMessage() when caller is the receiver deletes the message from the repository")
    void deleteMessage_whenCallerIsReceiver_thenDeleteFromRepository() {
        // given
        Message expected = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        ArgumentCaptor<Message> valueCapture = ArgumentCaptor.forClass(Message.class);
        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(expected));
        doNothing().when(messageRepository).delete(valueCapture.capture());

        // when
        messageService.deleteMessage(TEST_MESSAGE_ID, RECEIVER_EMAIL);

        // then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    @DisplayName("deleteMessage() when caller is neither sender nor receiver throws MessageAccessDeniedException")
    void deleteMessage_whenCallerIsNeitherSenderNorReceiver_thenThrowMessageAccessDeniedException() {
        // given
        Message expected = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(expected));

        // when / then
        assertThrows(MessageAccessDeniedException.class,
                () -> messageService.deleteMessage(TEST_MESSAGE_ID, "NOT_EXISTING"));
    }

    @Test
    @DisplayName("deleteMessage() when message does not exist throws MessageNotFoundException")
    void deleteMessage_whenMessageDoesNotExist_thenThrowMessageNotFoundException() {
        // given
        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.empty());

        // when / then
        assertThrows(MessageNotFoundException.class,
                () -> messageService.deleteMessage(TEST_MESSAGE_ID, RECEIVER_EMAIL));

        verify(messageRepository).findById(TEST_MESSAGE_ID);
    }

    @Test
    @DisplayName("deleteMessage() when caller is the sender deletes the message from the repository")
    void deleteMessage_whenCallerIsSender_thenDeleteFromRepository() {
        // given
        Message expected = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        ArgumentCaptor<Message> valueCapture = ArgumentCaptor.forClass(Message.class);
        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(expected));
        doNothing().when(messageRepository).delete(valueCapture.capture());

        // when
        messageService.deleteMessage(TEST_MESSAGE_ID, SENDER_EMAIL);

        // then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    @DisplayName("getReceivedMessages() returns a page of views for all messages addressed to the given user")
    void getReceivedMessages_whenUserHasMessages_thenReturnPagedViews() {
        // given
        List<MessageView> expected = MessageAssembler.getMessageViews();
        List<Message> messages = MessageAssembler.getMessages();
        Pageable pageable = PageRequest.of(0, 20);
        when(messageRepository.findAllByReceiverEmail(RECEIVER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(messages, pageable, messages.size()));

        // when
        Page<MessageView> actual = messageService.getReceivedMessages(RECEIVER_EMAIL, pageable);

        // then
        assertEquals(2, actual.getTotalElements());
        assertEquals(expected, actual.getContent());
    }

    @Test
    @DisplayName("getSentMessages() returns a page of views for all messages sent by the given user")
    void getSentMessages_whenUserHasSentMessages_thenReturnPagedViews() {
        // given
        List<MessageView> expected = MessageAssembler.getMessageViews();
        List<Message> messages = MessageAssembler.getMessages();
        Pageable pageable = PageRequest.of(0, 20);
        when(messageRepository.findAllBySenderEmail(SENDER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(messages, pageable, messages.size()));

        // when
        Page<MessageView> actual = messageService.getSentMessages(SENDER_EMAIL, pageable);

        // then
        assertEquals(2, actual.getTotalElements());
        assertEquals(expected, actual.getContent());
    }
}
