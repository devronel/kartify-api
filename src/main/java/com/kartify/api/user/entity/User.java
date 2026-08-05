package com.kartify.api.user.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.kartify.api.shared.BaseEntity;
import com.kartify.api.user.enums.Role;
import com.kartify.api.user.enums.UserStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserDetail detail;

    @OneToMany(
        mappedBy = "user",           // Refers to the 'user' field in UserFile entity
        cascade = CascadeType.ALL,   // Saving/deleting a User cascades to their files
        orphanRemoval = true         // Removes UserFile from DB if removed from this list
    )
    private List<UserFile> files = new ArrayList<>();

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    // --- Constructors ---
    public User() {}
    public User(String email, String password, UserStatus status) {
        this.email = email;
        this.password = password;
        this.status = status;
    }
    
    // --- Getter and Setter ---
    public Long getId() { return id; }

    public UserDetail getUserDetail() { return detail; }
    public void setUserDetail(UserDetail detail) {
        if (detail == null) {
            if (this.detail != null) {
                this.detail.setUser(null);
            }
        } else {
            detail.setUser(this);
        }
        this.detail = detail;
    }

    public List<UserFile> getFiles() { return files; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public String getFullName() {
        return this.getUserDetail().getFirstName() + " " + this.getUserDetail().getLastName(); 
    }
}