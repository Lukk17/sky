package com.lukk.sky.message.adapters.inbound.api;

import com.lukk.sky.contract.dto.MessageDTO;
import com.lukk.sky.contract.dto.MessagePage;
import com.lukk.sky.contract.dto.PageableObject;
import com.lukk.sky.contract.dto.SortObject;
import com.lukk.sky.message.domain.ports.inbound.MessageView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

final class MessageContractMapper {

    private MessageContractMapper() {
    }

    static MessageDTO toDto(MessageView view) {
        MessageDTO target = new MessageDTO();
        target.setId(view.id());
        target.setText(view.text());
        target.setReceiverEmail(view.receiverEmail());
        target.setSenderEmail(view.senderEmail());
        target.setCreatedTime(view.createdTime());
        target.setRead(view.read());
        return target;
    }

    static MessagePage toPage(Page<MessageView> source) {
        MessagePage target = new MessagePage();
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

    private static List<MessageDTO> toContent(List<MessageView> source) {
        return source.stream()
                .map(MessageContractMapper::toDto)
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
