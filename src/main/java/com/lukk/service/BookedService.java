package com.lukk.service;

import com.lukk.entity.Booked;
import com.lukk.entity.User;

import java.util.List;

public interface BookedService {

    List<Booked> findAllByUser(User user);

    Booked addBooked(Booked booked);
}
