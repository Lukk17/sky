package com.lukk.sky.message.adapters.inbound.api;

import com.lukk.sky.common.openapi.ApiCommonErrorResponses;
import com.lukk.sky.common.openapi.ApiSecuredErrorResponses;
import com.lukk.sky.common.openapi.ApiUnsupportedMediaTypeResponse;
import com.lukk.sky.common.security.IsUser;
import com.lukk.sky.common.security.SecurityUtils;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.adapters.dto.contract.Page;
import com.lukk.sky.message.adapters.inbound.api.contract.MessagesApi;
import com.lukk.sky.message.domain.ports.inbound.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;

@ApiCommonErrorResponses
@ApiSecuredErrorResponses
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(version = "v1")
public class MessageController implements MessagesApi {

    private final MessageService messageService;

    @ApiUnsupportedMediaTypeResponse
    @IsUser
    @Override
    public ResponseEntity<MessageDTO> sendMessage(MessageDTO message) {
        String userEmail = SecurityUtils.currentUserEmail();

        message.setSenderEmail(userEmail);
        message.setCreatedTime(DATE_TIME_FORMAT.format(Instant.now()));
        log.info("Sending message from {} to {}", userEmail, message.getReceiverEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.send(message));
    }

    @IsUser
    @Override
    public ResponseEntity<Page> getReceivedMessages(@PageableDefault(size = 20) Pageable pageable) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Getting received messages for user: {}", userEmail);

        return ResponseEntity.ok(MessagePageMapper.toPage(
                messageService.getReceivedMessages(userEmail, pageable)));
    }

    @IsUser
    @Override
    public ResponseEntity<Page> getSentMessages(@PageableDefault(size = 20) Pageable pageable) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Getting sent messages for user: {}", userEmail);

        return ResponseEntity.ok(MessagePageMapper.toPage(
                messageService.getSentMessages(userEmail, pageable)));
    }

    @IsUser
    @Override
    public ResponseEntity<Void> deleteMessage(UUID messageId) {
        String userEmail = SecurityUtils.currentUserEmail();

        log.info("Removing message with ID: {}", messageId);
        messageService.remove(messageId, userEmail);

        return ResponseEntity.noContent().build();
    }
}
