package com.kartify.api.account.controller;

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
import com.kartify.api.security.CustomUserDetails;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
public class ProfileController {

    protected final ProfileService profileService;

    public ProfileController(ProfileService profileService){
        this.profileService = profileService;
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody ProfileRequest request){
        ProfileResponse userProfile = profileService.upsertProfile(principal.getUser().getId(), request);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping(value = "/profile-picture")
    public ResponseEntity<String> uploadProfilePicture(@RequestParam("file") MultipartFile file) {

        String fileName = file.getOriginalFilename();
        Long fileSize = file.getSize();

        System.out.println("File: " + fileName);
        System.out.println("Size: " + fileSize);

        return ResponseEntity.ok("Okay");
    }

}
