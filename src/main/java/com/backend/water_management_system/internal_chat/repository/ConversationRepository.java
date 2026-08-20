package com.backend.water_management_system.internal_chat.repository;

import com.backend.water_management_system.internal_chat.entity.Conversation;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
/**
 * Persistence queries for direct conversations and participant-based filtering.
 */
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** Returns conversations for a user, newest activity first. */
    @Query("select c from InternalChatConversation c join c.participants p where p.user = :user order by c.updatedAt desc")
    List<Conversation> findAllByUserOrderByUpdatedAtDesc(@Param("user") User user);

    /**
     * Finds the existing direct conversation shared by the two users, if one
     * exists.
     */
    @Query("select c from InternalChatConversation c join c.participants p1 join c.participants p2 " +
            "where p1.user = :userOne and p2.user = :userTwo")
    Optional<Conversation> findDirectConversationBetween(@Param("userOne") User userOne,
            @Param("userTwo") User userTwo);

    /**
     * Finds conversations where the requested user is paired with a different user.
     */
    @Query(value = "select c from InternalChatConversation c join c.participants p where p.user = :user and p.user <> :otherUser")
    List<Conversation> findByParticipant(@Param("user") User user, @Param("otherUser") User otherUser);

    /**
     * Restricts a conversation lookup to conversations in which the user
     * participates.
     */
    @Query("select c from InternalChatConversation c join c.participants cp where cp.user = :user and c.id = :conversationId")
    Optional<Conversation> findByIdAndParticipant(@Param("conversationId") UUID conversationId,
            @Param("user") User user);

    /** Filters a user's conversations by the role of the other participant. */
    @Query(value = "select c from InternalChatConversation c join c.participants cp where cp.user = :user " +
            "and exists (select 1 from InternalChatConversationParticipant other where other.conversation = c and other.user <> :user and other.user.role = :role) "
            +
            "order by c.updatedAt desc")
    List<Conversation> findByUserAndOtherParticipantRole(@Param("user") User user, @Param("role") Role role);

    /** Filters a user's conversations by the other participant's display name. */
    @Query(value = "select c from InternalChatConversation c join c.participants cp where cp.user = :user " +
            "and exists (select 1 from InternalChatConversationParticipant other where other.conversation = c and other.user <> :user and lower(other.user.fullName) like lower(concat('%', :search, '%')) ) "
            +
            "order by c.updatedAt desc")
    List<Conversation> findByUserAndOtherParticipantNameLike(@Param("user") User user, @Param("search") String search);

    /**
     * Filters a user's conversations by text contained in the other participant's
     * UUID.
     */
    @Query(value = "select c from InternalChatConversation c join c.participants cp where cp.user = :user " +
            "and exists (select 1 from InternalChatConversationParticipant other where other.conversation = c and other.user <> :user and cast(other.user.id as string) like concat('%', :search, '%')) "
            +
            "order by c.updatedAt desc")
    List<Conversation> findByUserAndOtherParticipantIdLike(@Param("user") User user, @Param("search") String search);
}
