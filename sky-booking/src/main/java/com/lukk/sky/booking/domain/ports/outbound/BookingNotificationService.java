package com.lukk.sky.booking.domain.ports.outbound;

import com.lukk.sky.booking.domain.ports.inbound.BookingView;

public interface BookingNotificationService {

    void publishCreated(BookingView booking, String userEmail);

    void publishRemoved(String removalMessage, String userEmail);
}
