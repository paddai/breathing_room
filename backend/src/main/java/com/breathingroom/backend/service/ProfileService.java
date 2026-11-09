package com.breathingroom.backend.service;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.logic.ImageCheck;
import com.breathingroom.backend.model.AppUser;
import com.breathingroom.backend.model.UserAvatar;
import com.breathingroom.backend.repository.AppUserRepository;
import com.breathingroom.backend.repository.UserAvatarRepository;
import com.breathingroom.backend.validation.FieldValidationException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** The signed-in person's profile: display name and picture. */
@Service
public class ProfileService {

    private final AppUserRepository users;
    private final UserAvatarRepository avatars;

    public ProfileService(AppUserRepository users, UserAvatarRepository avatars) {
        this.users = users;
        this.avatars = avatars;
    }

    private AppUser user(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again."));
    }

    public Dtos.ProfileDto get(Long userId) {
        AppUser u = user(userId);
        return new Dtos.ProfileDto(u.getId(), u.getEmail(), u.getDisplayName(), avatars.existsById(userId));
    }

    @Transactional
    public Dtos.ProfileDto update(Long userId, Dtos.ProfileUpdate req) {
        String name = req == null || req.displayName() == null ? "" : req.displayName().replaceAll("\\p{Cntrl}", " ").trim().replaceAll("\\s+", " ");
        if (name.length() > 60) throw new FieldValidationException(Map.of("displayName", "Use 60 characters or fewer."));
        AppUser u = user(userId);
        u.setDisplayName(name.isEmpty() ? null : name);
        users.save(u);
        return get(userId);
    }

    @Transactional
    public void saveAvatar(Long userId, Dtos.AvatarUpload req) {
        if (req == null || req.data() == null || req.data().isBlank()) throw new FieldValidationException(Map.of("picture", "Choose a picture first."));
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(req.data());
        } catch (IllegalArgumentException e) {
            throw new FieldValidationException(Map.of("picture", "That picture could not be read. Try another one."));
        }
        Optional<String> problem = ImageCheck.problem(bytes);
        if (problem.isPresent()) throw new FieldValidationException(Map.of("picture", problem.get()));

        user(userId);
        UserAvatar a = avatars.findById(userId).orElseGet(UserAvatar::new);
        a.setUserId(userId);
        a.setContentType(ImageCheck.detect(bytes).orElseThrow()); // from the file itself, never from the browser's claim
        a.setData(bytes);
        a.setUpdatedAt(LocalDateTime.now());
        avatars.save(a);
    }

    public Optional<UserAvatar> avatar(Long userId) {
        return avatars.findById(userId);
    }

    @Transactional
    public void deleteAvatar(Long userId) {
        avatars.findById(userId).ifPresent(avatars::delete);
    }
}
