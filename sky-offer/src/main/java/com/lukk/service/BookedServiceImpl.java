package com.lukk.service;


import com.lukk.entity.Booked;
import com.lukk.repository.BookedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookedServiceImpl implements BookedService {

    private final BookedRepository bookedRepository;

//    @Override
//    public List<Booked> findAllByUser(User user) {
//        return bookedRepository.findAllByUser(user);
//    }

    @Override
    public Booked addBooked(Booked booked) {
        return bookedRepository.save(booked);
    }
}
