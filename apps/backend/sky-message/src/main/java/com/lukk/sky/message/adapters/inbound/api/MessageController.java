package com.lukk.sky.message.adapters.inbound.api;

import com.lukk.sky.common.openapi.ApiCommonErrorResponses;
import com.lukk.sky.common.openapi.ApiSecuredErrorResponses;
import com.lukk.sky.common.openapi.ApiUnsupportedMediaTypeResponse;
import com.lukk.sky.common.security.IsUser;
import com.lukk.sky.common.security.SecurityUtils;
import com.lukk.sky.contract.api.MessagesApi;
import com.lukk.sky.contract.dto.MessageDTO;
import com.lukk.sky.contract.dto.MessagePage;
import com.lukk.sky.message.domain.ports.inbound.MessageService;
import com.lukk.sky.message.domain.ports.inbound.MessageView;
import com.lukk.sky.message.domain.ports.inbound.SendMessageCommand;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@ApiCommonErrorResponses
@ApiSecuredErrorResponses
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(version = "v1")
public class MessageController implements MessagesApi {

    private final MessageService messageService;

    @ApiResponse(
            responseCode = "201",
            description = "Message sent",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageDTO.class)))
    @ApiUnsupportedMediaTypeResponse
    @IsUser
    @Override
    public ResponseEntity<MessageDTO> sendMessage(MessageDTO messageDTO) {
        String userEmail = SecurityUtils.currentUserEmail();

        log.info("Sending message from {} to {}", userEmail, messageDTO.getReceiverEmail());
        MessageView message = messageService.send(
                new SendMessageCommand(userEmail, messageDTO.getReceiverEmail(), messageDTO.getText()));

        return ResponseEntity.status(HttpStatus.CREATED).body(MessageContractMapper.toDto(message));
    }

    @ApiResponse(
            responseCode = "200",
            description = "Messages pulled",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessagePage.class)))
    @IsUser
    @Override
    public ResponseEntity<MessagePage> getReceivedMessages(@PageableDefault(size = 20) Pageable pageable) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Getting received messages for user: {}", userEmail);

        return ResponseEntity.ok(MessageContractMapper.toPage(
                messageService.getReceivedMessages(userEmail, pageable)));
    }

    @ApiResponse(
            responseCode = "200",
            description = "Messages pulled",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessagePage.class)))
    @IsUser
    @Override
    public ResponseEntity<MessagePage> getSentMessages(@PageableDefault(size = 20) Pageable pageable) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Getting sent messages for user: {}", userEmail);

        return ResponseEntity.ok(MessageContractMapper.toPage(
                messageService.getSentMessages(userEmail, pageable)));
    }

    @ApiResponse(
            responseCode = "404",
            description = "Message not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @IsUser
    @Override
    @RequestMapping(
            method = RequestMethod.DELETE,
            value = MessagesApi.PATH_DELETE_MESSAGE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<Void> deleteMessage(UUID messageId) {
        String userEmail = SecurityUtils.currentUserEmail();

        log.info("Removing message with ID: {}", messageId);
        messageService.deleteMessage(messageId, userEmail);

        return ResponseEntity.noContent().build();
    }
}
