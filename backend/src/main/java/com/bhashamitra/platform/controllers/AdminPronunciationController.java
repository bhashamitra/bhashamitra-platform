package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.CreatePronunciationRequest;
import com.bhashamitra.platform.controllers.dto.PronunciationDto;
import com.bhashamitra.platform.controllers.dto.UpdatePronunciationRequest;
import com.bhashamitra.platform.models.Pronunciation;
import com.bhashamitra.platform.services.AudioUtil;
import com.bhashamitra.platform.services.PronunciationService;
import com.bhashamitra.platform.services.S3Service;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.util.List;

import static com.bhashamitra.platform.security.ActorUtil.actor;

@RestController
@RequestMapping("/api/admin/pronunciations")
public class AdminPronunciationController {

    private final PronunciationService service;
    private final S3Service s3Service;

    public AdminPronunciationController(PronunciationService service, S3Service s3Service) {
        this.service = service;
        this.s3Service = s3Service;
    }

    // --------------------
    // READ
    // --------------------

    @GetMapping("/{id}")
    public ResponseEntity<PronunciationDto> getById(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(toDto(service.getById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List pronunciations for an owner.
     * ownerType + ownerId are required.
     */
    @GetMapping
    public ResponseEntity<List<PronunciationDto>> listByOwner(
            @RequestParam("ownerType") String ownerType,
            @RequestParam("ownerId") String ownerId
    ) {
        try {
            List<Pronunciation> out = service.listByOwner(ownerType, ownerId);
            return ResponseEntity.ok(out.stream().map(AdminPronunciationController::toDto).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // CREATE
    // --------------------

    /**
     * Create pronunciation with audio file upload.
     * Accepts multipart/form-data with audio file, metadata, and owner information.
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createWithFile(
            @RequestParam("ownerType") String ownerType,
            @RequestParam("ownerId") String ownerId,
            @RequestParam(value = "speaker", required = false) String speaker,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam(value = "durationMs", required = false) Integer durationMs,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestHeader(value = "Content-Length", required = false) Long contentLength,
            Authentication auth) {
        
        String act = actor(auth);

        try {
            // Validate Content-Length header (non-negotiable backend validation)
            if (contentLength != null && contentLength > AudioUtil.MAX_FILE_SIZE_BYTES) {
                return ResponseEntity.status(HttpStatus.valueOf(413))
                        .body("File size exceeds maximum allowed size (1 MB)");
            }

            // Validate file is present
            if (audioFile == null || audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Audio file is required");
            }

            // Validate file size (from MultipartFile)
            long fileSize = audioFile.getSize();
            AudioUtil.validateFileSize(fileSize);

            // Validate content type
            String contentType = audioFile.getContentType();
            AudioUtil.validateContentType(contentType != null ? contentType : "");

            // Validate duration if provided (entity-specific limits)
            if (durationMs != null) {
                AudioUtil.validateDuration(durationMs, ownerType);
            }

            // Upload file to S3
            String audioUri = s3Service.uploadAudioFile(
                    audioFile.getInputStream(),
                    audioFile.getOriginalFilename(),
                    contentType,
                    ownerType,
                    ownerId,
                    fileSize
            );

            // Create pronunciation record
            PronunciationService.CreateRequest svcReq = new PronunciationService.CreateRequest(
                    ownerType,
                    ownerId,
                    speaker,
                    region,
                    audioUri,
                    durationMs,
                    isPrimary != null ? isPrimary : false
            );

            Pronunciation created = service.create(svcReq, act);
            return ResponseEntity.ok(toDto(created));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create pronunciation: " + e.getMessage());
        }
    }

    /**
     * Create pronunciation with existing audio URI (backward compatibility).
     * Accepts JSON with audioUri already pointing to S3 or external URL.
     */
    @PostMapping(consumes = "application/json")
    public ResponseEntity<?> create(@Valid @RequestBody CreatePronunciationRequest req,
                                   Authentication auth) {
        String act = actor(auth);

        PronunciationService.CreateRequest svcReq = new PronunciationService.CreateRequest(
                req.ownerType(),
                req.ownerId(),
                req.speaker(),
                req.region(),
                req.audioUri(),
                req.durationMs(),
                req.isPrimary()
        );

        try {
            // Validate duration if provided (entity-specific limits)
            if (req.durationMs() != null) {
                AudioUtil.validateDuration(req.durationMs(), req.ownerType());
            }

            Pronunciation created = service.create(svcReq, act);
            return ResponseEntity.ok(toDto(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --------------------
    // UPDATE
    // --------------------

    @PutMapping("/{id}")
    public ResponseEntity<PronunciationDto> update(@PathVariable("id") String id,
                                                   @Valid @RequestBody UpdatePronunciationRequest req,
                                                   Authentication auth) {
        String act = actor(auth);

        PronunciationService.UpdateRequest svcReq = new PronunciationService.UpdateRequest(
                req.speaker(),
                req.region(),
                req.audioUri(),
                req.durationMs(),
                req.isPrimary()
        );

        try {
            Pronunciation updated = service.update(id, svcReq, act);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // DELETE
    // --------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id, Authentication auth) {
        try {
            service.delete(id, actor(auth));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --------------------
    // AUDIO PLAYBACK
    // --------------------

    /**
     * Streams audio file from S3 for playback.
     * Requires authentication (admin/editor only).
     */
    @GetMapping("/{id}/audio")
    public ResponseEntity<?> streamAudio(@PathVariable("id") String id, Authentication auth) {
        try {
            Pronunciation pronunciation = service.getById(id);
            
            // Download file from S3
            ResponseInputStream<GetObjectResponse> s3Object = s3Service.downloadAudioFile(pronunciation.getAudioUri());
            GetObjectResponse response = s3Object.response();
            
            // Determine content type from S3 metadata or default
            String contentType = response.contentType();
            if (contentType == null || contentType.isEmpty()) {
                // Try to infer from file extension
                String audioUri = pronunciation.getAudioUri().toLowerCase();
                if (audioUri.endsWith(".webm")) {
                    contentType = "audio/webm";
                } else if (audioUri.endsWith(".mp3")) {
                    contentType = "audio/mpeg";
                } else if (audioUri.endsWith(".wav")) {
                    contentType = "audio/wav";
                } else {
                    contentType = "audio/webm"; // Default
                }
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(response.contentLength());
            headers.setCacheControl("private, max-age=3600"); // Cache for 1 hour
            
            InputStreamResource resource = new InputStreamResource(s3Object);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to stream audio: " + e.getMessage());
        }
    }

    // --------------------
    // Helpers
    // --------------------

    private static PronunciationDto toDto(Pronunciation p) {
        return new PronunciationDto(
                p.getId(),
                p.getOwnerType(),
                p.getOwnerId(),
                p.getSpeaker(),
                p.getRegion(),
                p.getAudioUri(),
                p.getDurationMs(),
                p.getIsPrimary()
        );
    }
}
