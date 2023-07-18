package com.lukk.sky.message.domain.ports.repository;

import com.lukk.sky.message.domain.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findAllByReceiverEmail(String receiverEmail);

    List<Message> findAllBySenderEmail(String senderEmail);

}
