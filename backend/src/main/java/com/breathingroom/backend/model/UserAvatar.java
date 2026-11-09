package com.breathingroom.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One small profile picture per user, kept in its own table so signing in never loads the image. */
@Entity
@Table(name = "user_avatar")
public class UserAvatar {

    @Id
    private Long userId;

    @Column(nullable = false, length = 40)
    private String contentType;

    @Column(nullable = false)
    private byte[] data;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserAvatar() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
