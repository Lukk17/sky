package com.lukk.sky.booking.adapters.inbound.api;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.contract.Page;
import com.lukk.sky.booking.adapters.dto.contract.PageableObject;
import com.lukk.sky.booking.adapters.dto.contract.SortObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;

final class BookingPageMapper {

    private BookingPageMapper() {
    }

    static Page toPage(org.springframework.data.domain.Page<BookingDTO> source) {
        Page target = new Page();
        target.setContent(new ArrayList<Object>(source.getContent()));
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
