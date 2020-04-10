package com.lukk.sky.offer.service;

import com.lukk.sky.offer.H2TestProfileJPAConfig;
import com.lukk.sky.offer.SkyOfferApplication;
import com.lukk.sky.offer.entity.Booked;
import com.lukk.sky.offer.repository.BookedRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.lukk.sky.offer.Assemblers.BookingAssembler.getEmptyBookedList;
import static com.lukk.sky.offer.Assemblers.BookingAssembler.getPopulatedBooked;
import static com.lukk.sky.offer.Assemblers.UserAssembler.TEST_USER_EMAIL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        SkyOfferApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
public class BookedServiceImplTest {

    @Autowired
    BookedService bookedService;

    @MockBean
    BookedRepository bookedRepository;

    @Test
    public void whenFindAllByUser_thenResultAllUserBooked() {
        //Given
        List<Booked> expected = getEmptyBookedList();
        when(bookedRepository.findAllByUserEmail(TEST_USER_EMAIL)).thenReturn(expected);

        //When
        List<Booked> actual = bookedService.findAllByUser(TEST_USER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenAddBooked_thenAddBooked() {
        //Given
        Booked expected = getPopulatedBooked(TEST_USER_EMAIL, 1L);

        when(bookedRepository.save(expected)).thenReturn(expected);

        //When
        Booked actual = bookedService.addBooked(expected);

        //Then
        assertEquals(expected, actual);
    }
}
