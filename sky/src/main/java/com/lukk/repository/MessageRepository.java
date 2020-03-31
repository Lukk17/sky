package com.lukk.repository;

import com.lukk.entity.Message;
import com.lukk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findAllByReceiver(User user);

    List<Message> findAllBySender(User user);

}
