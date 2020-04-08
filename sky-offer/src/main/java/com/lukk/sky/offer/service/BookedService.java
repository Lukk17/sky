package com.lukk.sky.offer.service;

import com.lukk.sky.offer.entity.Booked;

import java.util.List;

public interface BookedService {

    List<Booked> findAllByUser(String userEmail);

    Booked addBooked(Booked booked);
}
