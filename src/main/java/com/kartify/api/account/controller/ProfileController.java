package com.kartify.api.account.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.account.dto.ProfileRequest;
import com.kartify.api.account.dto.ProfileResponse;
import com.kartify.api.account.service.ProfileService;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.security.CustomUserDetails;
import com.kartify.api.shared.dto.UploadedFileResponse;

import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
public class ProfileController {

    protected final ProfileService profileService;
    protected final FileStorage fileStorage;

    public ProfileController(ProfileService profileService, FileStorage fileStorage){
        this.profileService = profileService;
        this.fileStorage = fileStorage;
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody ProfileRequest request){
        ProfileResponse userProfile = profileService.upsertProfile(principal.getUser().getId(), request);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping(value = "/profile-picture")
    public ResponseEntity<UploadedFileResponse> uploadProfilePicture(@RequestParam("file") MultipartFile file) {

        UploadedFileResponse fileResponse = fileStorage.upload(file);

        return ResponseEntity.ok(fileResponse);
    }

}
