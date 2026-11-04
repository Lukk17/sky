package com.lukk.sky.message.adapters.inbound.api;

import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.adapters.dto.contract.Page;
import com.lukk.sky.message.adapters.dto.contract.PageableObject;
import com.lukk.sky.message.adapters.dto.contract.SortObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;

final class MessagePageMapper {

    private MessagePageMapper() {
    }

    static Page toPage(org.springframework.data.domain.Page<MessageDTO> source) {
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
