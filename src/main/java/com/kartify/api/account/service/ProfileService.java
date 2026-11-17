package com.kartify.api.account.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kartify.api.account.dto.ProfilePictureResponse;
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

@Service
public class ProfileService {

    @Value("${imagekit.public-url}")
    private String imagekitUrl;

    protected final UserRepository userRepository;
    protected final UserFileRepository userFileRepository;
    protected final FileStorage fileStorage;

    public ProfileService(UserRepository userRepository, UserFileRepository userFileRepository, FileStorage fileStorage){
        this.userRepository = userRepository;
        this.userFileRepository = userFileRepository;
        this.fileStorage = fileStorage;
    }

    
    /**
     * Create or Update profile information
     * 
     * 
     * @param userId
     * @param payload contains the required fields
     * @return profile response
     * @throws ResourceNotFoundException if user not found
     */
    public ProfileResponse upsertProfile(Long userId, ProfileRequest payload)
    {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Link the new user details to the user if don't have user detail
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

    
    /**
     * Get user user details
     * 
     * @param userId
     * @return profile response
     * @throws ResourceNotFoundException if no user found
     */
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

    
    /**
     * Upload profile picture
     * 
     * Upload image to the file storage and save the metadata
     * to the database.
     * 
     * @param userId
     * @param file contains the Uploaded file object
     * @return profile picture response
     * @throws ResourceNotFoundException if no user found
    */
    @Transactional
    public ProfilePictureResponse uploadProfilePicture(Long userId, MultipartFile file) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if already have profile picture if don't have, create new one
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

        return new ProfilePictureResponse(
            imagekitUrl + "/profile/" + fileResponse.fileName(),
            fileResponse
        );
    }

}
