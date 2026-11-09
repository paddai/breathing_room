package com.breathingroom.backend.controller;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.model.UserAvatar;
import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.service.ProfileService;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public Dtos.ProfileDto get() {
        return profiles.get(CurrentUser.id());
    }

    @PutMapping
    public Dtos.ProfileDto update(@RequestBody Dtos.ProfileUpdate body) {
        return profiles.update(CurrentUser.id(), body);
    }

    /** Body: { "contentType": "image/jpeg", "data": "<base64>" }. The type is re-checked from the file itself. */
    @PutMapping("/avatar")
    public ResponseEntity<Void> saveAvatar(@RequestBody Dtos.AvatarUpload body) {
        profiles.saveAvatar(CurrentUser.id(), body);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> avatar() {
        Optional<UserAvatar> a = profiles.avatar(CurrentUser.id());
        if (a.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.get().getContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .header("Cache-Control", "private, no-cache")
                .body(a.get().getData());
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> deleteAvatar() {
        profiles.deleteAvatar(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
