package com.backend.water_management_system.internal_chat.repository;

import com.backend.water_management_system.internal_chat.entity.Conversation;
import com.backend.water_management_system.internal_chat.entity.Message;
import com.backend.water_management_system.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
/** Persistence queries for message history, previews, and unread counts. */
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Loads a conversation page newest-first while fetching sender and conversation
     * data.
     */
    @Query("select m from InternalChatMessage m join fetch m.sender s join fetch m.conversation c where c = :conversation order by m.createdAt desc")
    Page<Message> findByConversationOrderByCreatedAtDesc(@Param("conversation") Conversation conversation,
            Pageable pageable);

    /**
     * Loads the latest messages with sender data available for response mapping.
     */
    @Query("select m from InternalChatMessage m join fetch m.sender s where m.conversation = :conversation order by m.createdAt desc")
    Page<Message> findLatestByConversation(@Param("conversation") Conversation conversation, Pageable pageable);

    /**
     * Counts messages sent by the other participant after the current user's read
     * marker.
     */
    @Query("select count(m) from InternalChatMessage m where m.conversation = :conversation and m.createdAt > :lastReadAt and m.sender <> :user")
    long countUnreadByConversationAndUser(@Param("conversation") Conversation conversation,
            @Param("lastReadAt") LocalDateTime lastReadAt,
            @Param("user") User user);

    /** Returns exactly one newest message used for conversation previews. */
    Optional<Message> findTopByConversationOrderByCreatedAtDesc(Conversation conversation);
}
