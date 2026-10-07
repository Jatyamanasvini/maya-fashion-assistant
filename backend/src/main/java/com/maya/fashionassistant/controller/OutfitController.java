package com.maya.fashionassistant.controller;

import com.maya.fashionassistant.dto.outfit.OutfitResponse;
import com.maya.fashionassistant.security.UserPrincipal;
import com.maya.fashionassistant.service.OutfitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/outfits")
@Tag(name = "Outfits", description = "Endpoints for retrieving saved outfits and their protected visualizations")
@SecurityRequirement(name = "bearerAuth")
public class OutfitController {

    private final OutfitService outfitService;

    public OutfitController(OutfitService outfitService) {
        this.outfitService = outfitService;
    }

    @GetMapping
    @Operation(summary = "List the user's outfits")
    public ResponseEntity<List<OutfitResponse>> getOutfits(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(outfitService.getOutfits(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single outfit owned by the user")
    public ResponseEntity<OutfitResponse> getOutfit(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(outfitService.getOutfit(principal.getId(), id));
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Get protected generated outfit visualization")
    public ResponseEntity<Resource> getOutfitImage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        Resource resource = outfitService.getGeneratedImage(principal.getId(), id);
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
