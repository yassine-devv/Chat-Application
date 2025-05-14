package com.example.chat_app.repository;

import com.example.chat_app.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatId(Long chatId);

    @Query(value = "SELECT * FROM messages WHERE chat = :chatId ORDER BY id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Message> findByChatIdWithPagination(@Param("chatId") Long chatId, @Param("limit") int limit, @Param("offset") int offset);

}
