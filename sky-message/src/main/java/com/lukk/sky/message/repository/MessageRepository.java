package com.lukk.sky.message.repository;

import com.lukk.sky.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findAllByReceiverEmail(String receiverEmail);

    List<Message> findAllBySenderEmail(String senderEmail);

}
