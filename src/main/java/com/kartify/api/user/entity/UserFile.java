package com.kartify.api.user.entity;

import com.kartify.api.shared.BaseEntity;
import com.kartify.api.user.enums.FileType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_files", indexes = {
    @Index(name = "idx_user_file_user", columnList = "user_id"),
    @Index(name = "idx_user_file_filename", columnList = "filename")
})
public class UserFile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long size;

    @Column
    private String extension;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType type;

    // --- Constructor ---
    public UserFile() {}
    public UserFile(User user, String filename, String name, Long size, String extension, String mimeType, FileType type) {
        this.user = user;
        this.filename = filename;
        this.name = name;
        this.size = size;
        this.extension = extension;
        this.mimeType = mimeType;
        this.type = type;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public FileType getType() { return type; }
    public void setType(FileType type) { this.type = type; }
}
