package com.backend.water_management_system.internal_chat.entity;

import com.backend.water_management_system.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "InternalChatConversationParticipant")
@Table(name = "internal_chat_conversation_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_internal_chat_participant_conversation_user", columnNames = { "conversation_id",
                "user_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/** Links a user to a conversation and stores that user's read position. */
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column(nullable = false)
    private LocalDateTime lastReadAt;

    /** Time this conversation was hidden by this participant, or null when visible. */
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        // A new participant has read everything that existed before joining.
        LocalDateTime now = LocalDateTime.now();
        this.joinedAt = now;
        this.lastReadAt = now;
    }
}
