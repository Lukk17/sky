package com.lukk.sky.booking.adapters.inbound.api;

import com.lukk.sky.booking.domain.ports.inbound.BookingView;
import com.lukk.sky.contract.dto.BookingDTO;
import com.lukk.sky.contract.dto.BookingPage;
import com.lukk.sky.contract.dto.PageableObject;
import com.lukk.sky.contract.dto.SortObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_FORMAT;

final class BookingContractMapper {

    private BookingContractMapper() {
    }

    static BookingDTO toDto(BookingView view) {
        BookingDTO target = new BookingDTO();
        target.setId(view.id());
        target.setOfferId(view.offerId());
        target.setBookedDate(LocalDate.parse(view.bookedDate(), DATE_FORMAT));
        target.setBookingUser(view.bookingUser());
        target.setOwnerEmail(view.ownerEmail());
        return target;
    }

    static BookingPage toPage(Page<BookingView> source) {
        BookingPage target = new BookingPage();
        target.setContent(toContent(source.getContent()));
        target.setEmpty(source.isEmpty());
        target.setFirst(source.isFirst());
        target.setLast(source.isLast());
        target.setNumber(source.getNumber());
        target.setNumberOfElements(source.getNumberOfElements());
        target.setPageable(toPageableObject(source.getPageable()));
        target.setSize(source.getSize());
        target.setSort(toSortObject(source.getSort()));
        target.setTotalElements(source.getTotalElements());
        target.setTotalPages(source.getTotalPages());
        return target;
    }

    private static List<BookingDTO> toContent(List<BookingView> source) {
        return source.stream()
                .map(BookingContractMapper::toDto)
                .toList();
    }

    private static PageableObject toPageableObject(Pageable source) {
        PageableObject target = new PageableObject();
        target.setOffset(source.getOffset());
        target.setPageNumber(source.getPageNumber());
        target.setPageSize(source.getPageSize());
        target.setPaged(source.isPaged());
        target.setSort(toSortObject(source.getSort()));
        target.setUnpaged(source.isUnpaged());
        return target;
    }

    private static SortObject toSortObject(Sort source) {
        SortObject target = new SortObject();
        target.setEmpty(source.isEmpty());
        target.setSorted(source.isSorted());
        target.setUnsorted(source.isUnsorted());
        return target;
    }
}
