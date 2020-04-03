package com.lukk.sky.message.repository;

import com.lukk.sky.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

//    List<Message> findAllByReceiver(User user);
//
//    List<Message> findAllBySender(User user);

}
