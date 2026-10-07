package com.maya.fashionassistant.service;

import com.maya.fashionassistant.dto.chat.ChatMessageResponse;
import com.maya.fashionassistant.dto.chat.ConversationResponse;
import com.maya.fashionassistant.entity.ChatMessage;
import com.maya.fashionassistant.entity.Conversation;
import com.maya.fashionassistant.entity.MessageRole;
import com.maya.fashionassistant.exception.ResourceNotFoundException;
import com.maya.fashionassistant.repository.ChatMessageRepository;
import com.maya.fashionassistant.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ConversationResponse createConversation(Long userId, String title) {
        Conversation conversation = new Conversation(userId, title);
        Conversation saved = conversationRepository.save(conversation);
        return toResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(c -> toResponse(c, List.of()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        List<ChatMessageResponse> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());

        return toResponse(conversation, messages);
    }

    @Transactional
    public ChatMessage saveMessage(Long conversationId, MessageRole role, String content) {
        ChatMessage message = new ChatMessage(conversationId, role, content);
        return chatMessageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long userId, Long conversationId) {
        // Ownership is enforced through the conversation lookup
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    private ConversationResponse toResponse(Conversation conversation, List<ChatMessageResponse> messages) {
        ConversationResponse response = new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
        response.setMessages(messages);
        return response;
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
