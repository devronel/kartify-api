package com.kartify.api.account.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.account.dto.ProfileRequest;
import com.kartify.api.account.dto.ProfileResponse;
import com.kartify.api.contract.FileStorage;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.shared.dto.UploadedFileResponse;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserDetail;
import com.kartify.api.user.entity.UserFile;
import com.kartify.api.user.enums.FileType;
import com.kartify.api.user.repository.UserFileRepository;
import com.kartify.api.user.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ProfileService {

    protected final UserRepository userRepository;
    protected final UserFileRepository userFileRepository;
    protected final FileStorage fileStorage;

    public ProfileService(UserRepository userRepository, UserFileRepository userFileRepository, FileStorage fileStorage){
        this.userRepository = userRepository;
        this.userFileRepository = userFileRepository;
        this.fileStorage = fileStorage;
    }

    // --- Create Or Update User Detail ---
    public ProfileResponse upsertProfile(Long userId, ProfileRequest payload)
    {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserDetail detail = user.getUserDetail();
        if (detail == null) {
            detail = new UserDetail();
            detail.setUser(user);
            user.setUserDetail(detail);
        }

        detail.setFirstName(payload.firstName());
        detail.setLastName(payload.lastName());
        detail.setPhone(payload.phone());
        detail.setDateOfBirth(payload.dateOfBirth());
        detail.setGender(payload.gender());

        userRepository.save(user);

        return new ProfileResponse(
            detail.getFirstName(),
            detail.getLastName(),
            detail.getPhone(),
            detail.getDateOfBirth(),
            detail.getGender() 
        );
    }

    // --- Get User Detail ---
    public ProfileResponse getProfileInformation(Long userId){

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDetail detail = user.getUserDetail();
        
        return new ProfileResponse(
            detail.getFirstName(),
            detail.getLastName(),
            detail.getPhone(),
            detail.getDateOfBirth(),
            detail.getGender() 
        );
        
    }

    // --- Upload profile picture ---
    @Transactional
    public UploadedFileResponse uploadProfilePicture(Long userId, MultipartFile file) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        UserFile userFile = userFileRepository
            .findByUserIdAndType(userId, FileType.PROFILE_PICTURE)
            .orElseGet(() -> {
                UserFile newFile = new UserFile();
                newFile.setType(FileType.PROFILE_PICTURE);
                user.addFile(newFile);
                return newFile;
            });

        UploadedFileResponse fileResponse = fileStorage.upload(file, "/profile");

        userFile.setFilename(fileResponse.fileName());
        userFile.setName(fileResponse.originalName());
        userFile.setSize(fileResponse.size());
        userFile.setExtension(fileResponse.extension());
        userFile.setMimeType(fileResponse.mimeType());

        userFileRepository.save(userFile);

        return fileResponse;
    }

}
