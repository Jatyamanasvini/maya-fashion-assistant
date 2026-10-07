package com.maya.fashionassistant.controller;

import com.maya.fashionassistant.dto.wardrobe.WardrobeItemRequest;
import com.maya.fashionassistant.dto.wardrobe.WardrobeItemResponse;
import com.maya.fashionassistant.dto.wardrobe.WardrobeUploadResponse;
import com.maya.fashionassistant.security.UserPrincipal;
import com.maya.fashionassistant.service.WardrobeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/wardrobe")
@Tag(name = "Wardrobe", description = "Endpoints for digital wardrobe management, photo ingestion, and item retrieval")
@SecurityRequirement(name = "bearerAuth")
public class WardrobeController {

    private final WardrobeService wardrobeService;

    public WardrobeController(WardrobeService wardrobeService) {
        this.wardrobeService = wardrobeService;
    }

    @PostMapping(value = "/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload wardrobe photo", description = "Ingests a photo, processes it via Multimodal Vision AI, and stores identified wardrobe items")
    public ResponseEntity<WardrobeUploadResponse> uploadPhoto(
            @RequestParam("photo") MultipartFile photo,
            @AuthenticationPrincipal UserPrincipal principal) {
        WardrobeUploadResponse response = wardrobeService.uploadAndProcessPhoto(principal.getId(), photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/items")
    @Operation(summary = "Get user wardrobe items", description = "Returns all clothing and accessory items owned by the authenticated user")
    public ResponseEntity<List<WardrobeItemResponse>> getItems(@AuthenticationPrincipal UserPrincipal principal) {
        List<WardrobeItemResponse> items = wardrobeService.getUserItems(principal.getId());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/items/{id}")
    @Operation(summary = "Get wardrobe item by ID", description = "Returns a single wardrobe item owned by the user, verifying ownership")
    public ResponseEntity<WardrobeItemResponse> getItemById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        WardrobeItemResponse item = wardrobeService.getItemById(principal.getId(), id);
        return ResponseEntity.ok(item);
    }

    @PostMapping("/items")
    @Operation(summary = "Manually create wardrobe item", description = "Adds a new wardrobe item to the user's wardrobe")
    public ResponseEntity<WardrobeItemResponse> createItem(
            @Valid @RequestBody WardrobeItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        WardrobeItemResponse created = wardrobeService.createItem(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Update wardrobe item", description = "Updates details of a wardrobe item owned by the user")
    public ResponseEntity<WardrobeItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody WardrobeItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        WardrobeItemResponse updated = wardrobeService.updateItem(principal.getId(), id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Delete wardrobe item", description = "Removes a wardrobe item owned by the user")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        wardrobeService.deleteItem(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/images/{id}")
    @Operation(summary = "Get protected wardrobe item image", description = "Securely streams the image for an item only if owned by the user")
    public ResponseEntity<Resource> getItemImage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        Resource resource = wardrobeService.getItemImageResource(principal.getId(), id);
        String contentType = Files.probeContentType(Paths.get(resource.getFile().getAbsolutePath()));
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/sources/{id}/image")
    @Operation(summary = "Get protected source photo image", description = "Securely streams the original uploaded photo only if owned by the user")
    public ResponseEntity<Resource> getSourceImage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        Resource resource = wardrobeService.getSourceImageResource(principal.getId(), id);
        String contentType = Files.probeContentType(Paths.get(resource.getFile().getAbsolutePath()));
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
