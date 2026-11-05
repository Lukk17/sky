package com.lukk.sky.booking.adapters.inbound.api;

import com.lukk.sky.booking.domain.ports.inbound.BookingService;
import com.lukk.sky.booking.domain.ports.inbound.BookingView;
import com.lukk.sky.common.openapi.ApiCommonErrorResponses;
import com.lukk.sky.common.openapi.ApiConflictResponse;
import com.lukk.sky.common.openapi.ApiDependencyBadGatewayResponse;
import com.lukk.sky.common.openapi.ApiDependencyUnavailableResponse;
import com.lukk.sky.common.openapi.ApiSecuredErrorResponses;
import com.lukk.sky.common.openapi.ApiUnsupportedMediaTypeResponse;
import com.lukk.sky.common.security.IsUser;
import com.lukk.sky.common.security.SecurityUtils;
import com.lukk.sky.contract.api.BookingsApi;
import com.lukk.sky.contract.dto.BookingDTO;
import com.lukk.sky.contract.dto.BookingPage;
import com.lukk.sky.contract.dto.BookingPayload;
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
public class BookingController implements BookingsApi {

    private final BookingService bookingService;

    @ApiResponse(
            responseCode = "200",
            description = "Found user bookings",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingPage.class)))
    @IsUser
    @Override
    public ResponseEntity<BookingPage> getBookedOffers(@PageableDefault(size = 20) Pageable pageable) {
        String userEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(BookingContractMapper.toPage(
                bookingService.getBookedOffersForUser(userEmail, pageable)));
    }

    @ApiResponse(
            responseCode = "201",
            description = "Booking created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingDTO.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No offer with that id exists in sky-offer",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiConflictResponse
    @ApiUnsupportedMediaTypeResponse
    @ApiDependencyBadGatewayResponse
    @ApiDependencyUnavailableResponse
    @IsUser
    @Override
    public ResponseEntity<BookingDTO> bookOffer(BookingPayload bookingPayload) {
        log.info("Starting to book offer with payload: {}", bookingPayload);

        String bookingUser = SecurityUtils.currentUserEmail();
        BookingView bookingView = bookingService.bookOffer(
                bookingPayload.getOfferId(), bookingPayload.getDateToBook(), bookingUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(BookingContractMapper.toDto(bookingView));
    }

    @ApiResponse(
            responseCode = "404",
            description = "Booking not found",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    @IsUser
    @Override
    @RequestMapping(
            method = RequestMethod.DELETE,
            value = BookingsApi.PATH_REMOVE_BOOKING,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<Void> removeBooking(UUID bookingId) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Removing booking with ID: {} by user: {}", bookingId, userEmail);

        bookingService.removeBooking(bookingId, userEmail);

        return ResponseEntity.noContent().build();
    }
}
