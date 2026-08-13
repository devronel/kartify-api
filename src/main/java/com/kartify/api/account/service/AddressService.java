package com.kartify.api.account.service;

import org.springframework.stereotype.Service;

import com.kartify.api.account.dto.AddressRequest;
import com.kartify.api.account.dto.AddressResponse;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserAddress;
import com.kartify.api.user.enums.UserAddressType;
import com.kartify.api.user.repository.UserAddressRepository;
import com.kartify.api.user.repository.UserRepository;

@Service
public class AddressService {

    protected final UserRepository userRepository;
    protected final UserAddressRepository userAddressRepository;

    public AddressService(UserRepository userRepository, UserAddressRepository userAddressRepository){
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
    }

    public AddressResponse createAddress(Long userId, AddressRequest payload){

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserAddress userAddress = new UserAddress();
        user.addAddress(userAddress);
        userAddress.setLabel(payload.label());
        userAddress.setType(payload.type());
        userAddress.setRecipientName(payload.recipientName());
        userAddress.setPhone(payload.phone());
        userAddress.setAddressLine1(payload.addressLine1());
        userAddress.setAddressLine2(payload.addressLine2());
        userAddress.setBarangay(payload.barangay());
        userAddress.setCity(payload.city());
        userAddress.setProvince(payload.province());
        userAddress.setRegion(payload.region());
        userAddress.setPostalCode(payload.postalCode());
        userAddress.setCountry(payload.country());

        Boolean hasExistingAddresses = userAddressRepository.findByUserId(user.getId()).isEmpty();

        if (!hasExistingAddresses) {
            userAddress.setIsDefault(true);
        } else if (Boolean.TRUE.equals(payload.isDefault())) {
            unsetCurrentDefault(user.getId());
            userAddress.setIsDefault(true);
        } else {
            userAddress.setIsDefault(false);
        }

        UserAddress userAddressCreated = userAddressRepository.save(userAddress);

        return new AddressResponse(
            userAddressCreated.getType()
        );

    }

    private void unsetCurrentDefault(Long userId) {
        userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
            .ifPresent(current ->{
                current.setIsDefault(false);
                userAddressRepository.save(current);
            });
    }

}
