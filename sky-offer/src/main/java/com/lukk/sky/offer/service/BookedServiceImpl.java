package com.lukk.sky.offer.service;


import com.lukk.sky.offer.entity.Booked;
import com.lukk.sky.offer.repository.BookedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookedServiceImpl implements BookedService {

    private final BookedRepository bookedRepository;

    @Override
    public List<Booked> findAllByUser(String userEmail) {
        return bookedRepository.findAllByUserEmail(userEmail);
    }

    @Override
    public Booked addBooked(Booked booked) {
        return bookedRepository.save(booked);
    }
}
