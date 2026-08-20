package com.backend.water_management_system.internal_chat.repository;

import com.backend.water_management_system.internal_chat.entity.Conversation;
import com.backend.water_management_system.internal_chat.entity.ConversationParticipant;
import com.backend.water_management_system.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
/** Persistence access for conversation membership and per-user read state. */
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    /** Finds one user's membership in a conversation. */
    Optional<ConversationParticipant> findByConversationAndUser(Conversation conversation, User user);

    /** Returns all members of a conversation. */
    List<ConversationParticipant> findByConversation(Conversation conversation);

    /** Finds the participant other than the supplied current user. */
    @Query("select cp from InternalChatConversationParticipant cp where cp.conversation = :conversation and cp.user <> :user")
    Optional<ConversationParticipant> findOtherParticipant(@Param("conversation") Conversation conversation,
            @Param("user") User user);

    /** Checks authorization for conversation operations. */
    boolean existsByConversationAndUser(Conversation conversation, User user);
}
