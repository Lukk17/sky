package com.lukk.sky.message.domain.ports.repository;

import com.lukk.sky.message.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findAllByReceiverEmail(String receiverEmail, Pageable pageable);

    Page<Message> findAllBySenderEmail(String senderEmail, Pageable pageable);

}
