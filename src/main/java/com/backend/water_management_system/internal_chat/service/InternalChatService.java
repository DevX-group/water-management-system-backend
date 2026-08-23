package com.backend.water_management_system.internal_chat.service;

import com.backend.water_management_system.internal_chat.dto.ConversationResponse;
import com.backend.water_management_system.internal_chat.dto.CreateConversationRequest;
import com.backend.water_management_system.internal_chat.dto.InternalChatUserResponse;
import com.backend.water_management_system.internal_chat.dto.MessageResponse;
import com.backend.water_management_system.internal_chat.dto.SendMessageRequest;
import com.backend.water_management_system.internal_chat.entity.Conversation;
import com.backend.water_management_system.internal_chat.entity.ConversationParticipant;
import com.backend.water_management_system.internal_chat.entity.Message;
import com.backend.water_management_system.internal_chat.exceptions.InternalChatAccessDeniedException;
import com.backend.water_management_system.internal_chat.exceptions.InternalChatNotFoundException;
import com.backend.water_management_system.internal_chat.exceptions.InternalChatValidationException;
import com.backend.water_management_system.internal_chat.repository.ConversationParticipantRepository;
import com.backend.water_management_system.internal_chat.repository.ConversationRepository;
import com.backend.water_management_system.internal_chat.repository.MessageRepository;
import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.Role;
import com.backend.water_management_system.user.enums.UserStatus;
import com.backend.water_management_system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InternalChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;

    public InternalChatService(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ConversationParticipantRepository conversationParticipantRepository,
            MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.messageRepository = messageRepository;
    }

    /** Returns active non-customer staff users filtered by role and search text. */
    public List<InternalChatUserResponse> searchEligibleUsers(UUID currentUserId, Role role, String search) {
        User currentUser = findUserById(currentUserId);
        ensureEligibleInternalChatUser(currentUser);

        // Keep filtering in one place so the user picker and conversation rules agree.
        List<User> eligibleUsers = userRepository.findAll().stream()
                .filter(user -> isEligibleInternalChatUser(user))
                .filter(user -> !user.getId().equals(currentUserId))
                .filter(user -> role == null || user.getRole() == role)
                .filter(user -> matchesSearch(user, search))
                .sorted(Comparator.comparing(u -> u.getFullName() == null ? "" : u.getFullName(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        return eligibleUsers.stream()
                .map(InternalChatUserResponse::from)
                .toList();
    }

    @Transactional
    public ConversationResponse createConversation(UUID currentUserId, CreateConversationRequest request) {
        User currentUser = findUserById(currentUserId);
        ensureEligibleInternalChatUser(currentUser);

        User targetUser = findUserById(request.targetUserId());
        validateTargetForConversation(currentUser, targetUser);

        Optional<Conversation> existing = findDirectConversation(currentUser, targetUser);
        if (existing.isPresent()) {
            // Direct conversations are unique per pair, so repeated creation is idempotent.
            return buildConversationResponse(currentUser, existing.get());
        }

        Conversation conversation = new Conversation();
        conversation = conversationRepository.save(conversation);

        ConversationParticipant currentParticipant = ConversationParticipant.builder()
                .conversation(conversation)
                .user(currentUser)
                .build();
        ConversationParticipant targetParticipant = ConversationParticipant.builder()
                .conversation(conversation)
                .user(targetUser)
                .build();
        conversationParticipantRepository.saveAll(List.of(currentParticipant, targetParticipant));
        conversation.setParticipants(new ArrayList<>(List.of(currentParticipant, targetParticipant)));

        return buildConversationResponse(currentUser, conversation);
    }

    @Transactional(readOnly = true)
    /**
     * Returns the current user's conversations with optional participant filters.
     */
    public List<ConversationResponse> getConversationList(UUID currentUserId, Role role, String search) {
        User currentUser = findUserById(currentUserId);
        ensureEligibleInternalChatUser(currentUser);

        List<Conversation> conversations = conversationRepository.findAllByUserOrderByUpdatedAtDesc(currentUser);
        List<ConversationResponse> responses = new ArrayList<>();

        for (Conversation conversation : conversations) {
            User otherUser = findOtherParticipant(conversation, currentUser);
            if (otherUser == null) {
                continue;
            }
            if (role != null && otherUser.getRole() != role) {
                continue;
            }
            if (search != null && !search.isBlank() && !matchesSearch(otherUser, search)) {
                continue;
            }
            responses.add(buildConversationResponse(currentUser, conversation));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    /**
     * Returns a page of messages in chronological display order after
     * authorization.
     */
    public List<MessageResponse> getMessages(UUID currentUserId, UUID conversationId, int page, int size) {
        User currentUser = findUserById(currentUserId);
        ensureEligibleInternalChatUser(currentUser);
        Conversation conversation = findConversationById(conversationId);
        ensureParticipant(conversation, currentUser);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Message> messages = messageRepository.findByConversationOrderByCreatedAtDesc(conversation, pageable);
        User otherParticipant = findOtherParticipant(conversation, currentUser);
        ConversationParticipant otherMembership = conversationParticipantRepository
                .findByConversationAndUser(conversation, otherParticipant)
                .orElse(null);
        LocalDateTime otherLastReadAt = otherMembership == null ? null : otherMembership.getLastReadAt();

        return messages.stream().sorted(Comparator.comparing(Message::getCreatedAt))
                .map(message -> MessageResponse.from(message, isReadByOtherParticipant(message, currentUser,
                        otherLastReadAt)))
                .toList();
    }

    @Transactional
    /** Validates, stores, and returns a message sent through REST or WebSocket. */
    public MessageResponse sendMessage(UUID currentUserId, UUID conversationId, SendMessageRequest request) {
        User currentUser = findUserById(currentUserId);
        ensureEligibleInternalChatUser(currentUser);

        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new InternalChatValidationException("Message content cannot be blank.");
        }
        String normalized = request.content().trim();
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            throw new InternalChatValidationException("Message content is too long.");
        }

        Conversation conversation = findConversationById(conversationId);
        ensureParticipant(conversation, currentUser);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(currentUser)
                .content(normalized)
                .build();
        Message saved = messageRepository.save(message);

        // Updating the parent makes the conversation list reflect the new message.
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return MessageResponse.from(saved, false);
    }

    @Transactional
    /** Advances the current user's read marker for a conversation. */
    public void markConversationAsRead(UUID currentUserId, UUID conversationId) {
        User currentUser = findUserById(currentUserId);
        ensureEligibleInternalChatUser(currentUser);

        Conversation conversation = findConversationById(conversationId);
        ConversationParticipant participant = conversationParticipantRepository
                .findByConversationAndUser(conversation, currentUser)
                .orElseThrow(
                        () -> new InternalChatAccessDeniedException("You are not a participant in this conversation."));

        participant.setLastReadAt(LocalDateTime.now());
        conversationParticipantRepository.save(participant);
    }

    /**
     * Returns the timestamp used to notify the sender about a newly read
     * conversation.
     */
    @Transactional(readOnly = true)
    public LocalDateTime getLastReadAt(UUID currentUserId, UUID conversationId) {
        User currentUser = findUserById(currentUserId);
        Conversation conversation = findConversationById(conversationId);
        return conversationParticipantRepository.findByConversationAndUser(conversation, currentUser)
                .map(ConversationParticipant::getLastReadAt)
                .orElseThrow(
                        () -> new InternalChatAccessDeniedException("You are not a participant in this conversation."));
    }

    @Transactional(readOnly = true)
    /**
     * Counts messages from the other participant that follow the user's read
     * marker.
     */
    public long countUnread(UUID currentUserId, UUID conversationId) {
        User currentUser = findUserById(currentUserId);
        ensureEligibleInternalChatUser(currentUser);
        Conversation conversation = findConversationById(conversationId);
        ensureParticipant(conversation, currentUser);
        ConversationParticipant participant = conversationParticipantRepository
                .findByConversationAndUser(conversation, currentUser)
                .orElseThrow(
                        () -> new InternalChatAccessDeniedException("You are not a participant in this conversation."));
        return messageRepository.countUnreadByConversationAndUser(conversation, participant.getLastReadAt(),
                currentUser);
    }

    /** Returns the authenticated application user used by transport adapters. */
    public User getCurrentUser(UUID currentUserId) {
        return findUserById(currentUserId);
    }

    /** Loads a user or raises the internal-chat not-found error. */
    public User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InternalChatNotFoundException("User not found."));
    }

    /** Returns the other authorized participant for a direct conversation. */
    @Transactional(readOnly = true)
    public User getOtherParticipant(UUID currentUserId, UUID conversationId) {
        User currentUser = findUserById(currentUserId);
        ensureEligibleInternalChatUser(currentUser);
        Conversation conversation = findConversationById(conversationId);
        ensureParticipant(conversation, currentUser);
        User otherParticipant = findOtherParticipant(conversation, currentUser);
        if (otherParticipant == null) {
            throw new InternalChatAccessDeniedException("Conversation member not found.");
        }
        return otherParticipant;
    }

    /** Defines the staff roles and account state allowed to use internal chat. */
    private boolean isEligibleInternalChatUser(User user) {
        return user != null && user.getStatus() == UserStatus.ACTIVE && user.getRole() != Role.CUSTOMER
                && (user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.SYSTEM_ADMIN
                        || user.getRole() == Role.CUSTOMER_HANDLER || user.getRole() == Role.METER_READER);
    }

    /**
     * Rejects inactive customers and other users before any chat operation
     * proceeds.
     */
    private void ensureEligibleInternalChatUser(User user) {
        if (!isEligibleInternalChatUser(user)) {
            throw new AccessDeniedException("User is not eligible for internal chat.");
        }
    }

    /** Validates that a conversation target is different, active, and eligible. */
    private void validateTargetForConversation(User currentUser, User targetUser) {
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new InternalChatValidationException("You cannot start a conversation with yourself.");
        }
        if (!isEligibleInternalChatUser(targetUser)) {
            throw new InternalChatValidationException("Target user is not eligible for internal chat.");
        }
        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new InternalChatValidationException("Target user account is not active.");
        }
    }

    /** Looks up the direct conversation shared by two users. */
    private Optional<Conversation> findDirectConversation(User userOne, User userTwo) {
        return conversationRepository.findDirectConversationBetween(userOne, userTwo);
    }

    /** Returns the participant on the opposite side of a direct conversation. */
    private User findOtherParticipant(Conversation conversation, User currentUser) {
        return conversationParticipantRepository.findOtherParticipant(conversation, currentUser)
                .map(ConversationParticipant::getUser)
                .orElse(null);
    }

    private boolean isReadByOtherParticipant(Message message, User currentUser, LocalDateTime otherLastReadAt) {
        return message.getSender().getId().equals(currentUser.getId())
                && otherLastReadAt != null
                && !otherLastReadAt.isBefore(message.getCreatedAt());
    }

    /**
     * Enforces conversation membership before reading or writing conversation data.
     */
    private void ensureParticipant(Conversation conversation, User user) {
        boolean isParticipant = conversationParticipantRepository.existsByConversationAndUser(conversation, user);
        if (!isParticipant) {
            throw new InternalChatAccessDeniedException("You are not a participant in this conversation.");
        }
    }

    /** Loads a conversation or raises the internal-chat not-found error. */
    private Conversation findConversationById(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new InternalChatNotFoundException("Conversation not found."));
    }

    /** Matches a staff name or UUID against the optional search term. */
    private boolean matchesSearch(User user, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String term = search.trim();
        return user.getFullName() != null && user.getFullName().toLowerCase().contains(term.toLowerCase())
                || user.getId() != null && user.getId().toString().contains(term);
    }

    /**
     * Builds the list-item response, including preview and current-user unread
     * state.
     */
    private ConversationResponse buildConversationResponse(User currentUser, Conversation conversation) {
        User otherUser = findOtherParticipant(conversation, currentUser);
        if (otherUser == null) {
            throw new InternalChatAccessDeniedException("Conversation member not found.");
        }

        ConversationParticipant participant = conversationParticipantRepository
                .findByConversationAndUser(conversation, currentUser)
                .orElseThrow(
                        () -> new InternalChatAccessDeniedException("You are not a participant in this conversation."));

        Message latestMessage = messageRepository.findTopByConversationOrderByCreatedAtDesc(conversation).orElse(null);
        String preview = latestMessage == null ? "" : latestMessage.getContent();
        LocalDateTime latestTime = latestMessage == null ? null : latestMessage.getCreatedAt();
        long unread = latestMessage == null ? 0
                : messageRepository.countUnreadByConversationAndUser(conversation, participant.getLastReadAt(),
                        currentUser);

        return ConversationResponse.from(conversation, otherUser, preview, latestTime, unread);
    }
}
