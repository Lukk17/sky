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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @IsUser
    @Override
    public ResponseEntity<BookingPage> getBookedOffers(@PageableDefault(size = 20) Pageable pageable) {
        String userEmail = SecurityUtils.currentUserEmail();

        return ResponseEntity.ok(BookingContractMapper.toPage(
                bookingService.getBookedOffersForUser(userEmail, pageable)));
    }

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

    @IsUser
    @Override
    public ResponseEntity<Void> removeBooking(UUID bookingId) {
        String userEmail = SecurityUtils.currentUserEmail();
        log.info("Removing booking with ID: {} by user: {}", bookingId, userEmail);

        bookingService.removeBooking(bookingId, userEmail);

        return ResponseEntity.noContent().build();
    }
}
