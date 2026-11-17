package com.kartify.api.account.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kartify.api.account.dto.AddressRequest;
import com.kartify.api.account.dto.AddressResponse;
import com.kartify.api.account.dto.AddressUpdateRequest;
import com.kartify.api.exception.ResourceNotFoundException;
import com.kartify.api.user.entity.User;
import com.kartify.api.user.entity.UserAddress;
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

    /**
     * Get all user address
     * 
     * @param userId
     * @return AddressResponse contains list of addresses
     * @throws ResourceNotFoundException if no user found
     */
    public List<AddressResponse> getAddresses(Long userId){
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<UserAddress> addresses = userAddressRepository.findByUserId(user.getId());

        return addresses.stream()
            .map(address -> toResponse(address))
            .toList();
    }

    
    /**
     * Get address by id and user_id
     * 
     * Query both address id and user id to ensure that the address is from
     * the currently authenticated user
     * 
     * @param userId
     * @param id
     * @return AddressResponse contains the information of the address
     * @throws ResourceNotFoundException if no address found on the current auth user
     */
    public AddressResponse getAddress(Long userId, Long id){
    
        UserAddress address = userAddressRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        return toResponse(address);
    }

    
    /**
     * Create new address
     * 
     * Include all the address information, set the current auth user
     * to that address and set primary address
     * 
     * @param userId
     * @param payload contains required address information
     * @return AddressResponse
     * @throws ResourceNotFoundException if no user found
     */
    public AddressResponse createAddress(Long userId, AddressRequest payload){

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        UserAddress userAddress = new UserAddress();
        userAddress.setUser(user);
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
        
        List<UserAddress> existingAddresses = userAddressRepository.findByUserId(user.getId());

        // Check if their's already have primary address in the current existing address of the user
        if (existingAddresses.isEmpty()) {
            userAddress.setIsDefault(true);
        } else if (Boolean.TRUE.equals(payload.isDefault())) {
            unsetCurrentDefault(user.getId());
            userAddress.setIsDefault(true);
        } else {
            userAddress.setIsDefault(false);
        }

        UserAddress userAddressCreated = userAddressRepository.save(userAddress);

        return toResponse(userAddressCreated);

    }

    
    /**
     * Update address
     * 
     * Update all the important address fields aside from primary
     * because setting default address is separate
     * 
     * @param userId
     * @param addressId
     * @param payload contains all the updated fields
     * @return AddressResponse
     * @throws ResourceNotFoundException if no user found
     * @throws ResourceNotFoundException if no address found
     */
    public AddressResponse updateAddress(Long userId, Long addressId, AddressUpdateRequest payload){

        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserAddress userAddress = userAddressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        
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

        UserAddress userAddressUpdated = userAddressRepository.save(userAddress);

        return toResponse(userAddressUpdated);

    }

    
    /**
     * Deleted address
     * 
     * Check both user id and address id to ensure that the deleted address is
     * from that user.
     * 
     * If the default address is deleted it switch the default to the first address found
     * 
     * @param userId
     * @param addressId
     * @return string message that the address is deleted
     * @throws ResourceNotFoundException if no address found
    */
    public String deleteAddress(Long userId, Long addressId){

        UserAddress userAddress = userAddressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // --- Check if the delete address is the default address ---
        if(userAddress.getIsDefault()){
            userAddressRepository.findFirstByUserIdAndIdNot(userId, addressId)
                .ifPresent(address -> {
                    address.setIsDefault(true);
                    userAddressRepository.save(address);
                });
        }

        userAddressRepository.delete(userAddress);

        return "Address Deleted";
    }

    
    /**
     * Set default address
     * 
     * Unset the current default address first and set the new default to the
     * selected address
     * 
     * @param userId
     * @param addressId
     * @return address response
     * @throws ResourceNotFoundException if no user found
     * @throws ResourceNotFoundException if no address found
     */
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {

        userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        unsetCurrentDefault(userId);
        address.setIsDefault(true);

        UserAddress userAddressUpdated = userAddressRepository.save(address);
        
        return toResponse(userAddressUpdated);
    }

    
    /**
     * Unset the current default address
     * 
     * Find first the default and update it to false
     * 
     * @param userId
     */
    private void unsetCurrentDefault(Long userId) {
        userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
            .ifPresent(current -> {
                current.setIsDefault(false);
                userAddressRepository.save(current);
            });
    }

    
    private AddressResponse toResponse(UserAddress payload){
        return new AddressResponse(
            payload.getId(),
            payload.getLabel(),
            payload.getType(),
            payload.getRecipientName(),
            payload.getPhone(),
            payload.getAddressLine1(),
            payload.getAddressLine2(),
            payload.getBarangay(),
            payload.getCity(),
            payload.getProvince(),
            payload.getRegion(),
            payload.getPostalCode(),
            payload.getCountry(),
            payload.getIsDefault()
        );
    }

}
