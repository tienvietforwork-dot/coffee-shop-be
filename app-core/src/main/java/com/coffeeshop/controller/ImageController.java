package com.coffeeshop.controller;

import com.coffeeshop.dto.response.ImageResponse;
import com.coffeeshop.entity.Image;
import com.coffeeshop.security.Perm;
import com.coffeeshop.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping(value = "/api/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Perm.CAN_MENU_EDIT)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse upload(@RequestPart("file") MultipartFile file) {
        return imageService.upload(file);
    }

    /** Public (menu photos); an image never changes once uploaded, so browsers may cache it for good. */
    @GetMapping("/api/public/images/{id}")
    public ResponseEntity<byte[]> get(@PathVariable Long id) {
        Image image = imageService.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image.getData());
    }
}
