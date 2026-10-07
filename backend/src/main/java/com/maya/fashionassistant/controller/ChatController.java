package com.maya.fashionassistant.controller;

import com.maya.fashionassistant.dto.chat.ChatMessageRequest;
import com.maya.fashionassistant.dto.chat.ChatReplyResponse;
import com.maya.fashionassistant.dto.chat.ConversationRequest;
import com.maya.fashionassistant.dto.chat.ConversationResponse;
import com.maya.fashionassistant.security.UserPrincipal;
import com.maya.fashionassistant.service.ConversationService;
import com.maya.fashionassistant.service.StylingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Conversational styling endpoints: conversations, messages, and outfit recommendations")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ConversationService conversationService;
    private final StylingService stylingService;

    public ChatController(ConversationService conversationService, StylingService stylingService) {
        this.conversationService = conversationService;
        this.stylingService = stylingService;
    }

    @GetMapping("/conversations")
    @Operation(summary = "List the user's conversations")
    public ResponseEntity<List<ConversationResponse>> listConversations(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(conversationService.listConversations(principal.getId()));
    }

    @PostMapping("/conversations")
    @Operation(summary = "Create a new conversation")
    public ResponseEntity<ConversationResponse> createConversation(
            @Valid @RequestBody ConversationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ConversationResponse response = conversationService.createConversation(principal.getId(), request.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Get a conversation with its messages")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(conversationService.getConversation(principal.getId(), id));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "Send a message and receive a styled recommendation",
            description = "Processes a user message, styles an outfit from the user's real wardrobe, and returns a reply with the outfit, hairstyle, weather context, and visualization.")
    public ResponseEntity<ChatReplyResponse> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Enforce conversation ownership before processing
        conversationService.getConversation(principal.getId(), id);
        ChatReplyResponse reply = stylingService.handleMessage(
                principal.getId(), id, request.getMessage(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(reply);
    }
}
