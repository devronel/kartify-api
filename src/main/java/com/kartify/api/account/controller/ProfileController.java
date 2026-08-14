package com.kartify.api.account.controller;

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
import com.kartify.api.shared.dto.ApiResponse;
import com.kartify.api.shared.dto.UploadedFileResponse;

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
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody ProfileRequest request){
        ProfileResponse userProfile = profileService.upsertProfile(principal.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile Updated Successfully", userProfile));
    }

    @PostMapping(value = "/profile-picture")
    public ResponseEntity<ApiResponse<UploadedFileResponse>> uploadProfilePicture(@AuthenticationPrincipal CustomUserDetails principal, @RequestParam("file") MultipartFile file) {
        UploadedFileResponse fileResponse = profileService.uploadProfilePicture(principal.getUser().getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Profile Picture Uploaded Successfully", fileResponse));
    }

}
