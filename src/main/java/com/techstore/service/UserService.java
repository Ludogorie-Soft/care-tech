package com.techstore.service;

import com.techstore.dto.UserResponseDTO;
import com.techstore.dto.request.ChangePasswordDTO;
import com.techstore.dto.request.UserAddressDTO;
import com.techstore.dto.request.UserCompanyDTO;
import com.techstore.dto.request.UserProfileUpdateDTO;
import com.techstore.dto.request.UserRequestDTO;
import com.techstore.dto.response.UserAddressResponseDTO;
import com.techstore.dto.response.UserCompanyResponseDTO;
import com.techstore.entity.User;
import com.techstore.entity.UserAddress;
import com.techstore.entity.UserCompany;
import com.techstore.exception.BusinessLogicException;
import com.techstore.exception.DuplicateResourceException;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.exception.ValidationException;
import com.techstore.repository.UserAddressRepository;
import com.techstore.repository.UserCompanyRepository;
import com.techstore.repository.UserRepository;
import com.techstore.util.ExceptionHelper;
import com.techstore.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityHelper securityHelper;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable) {
        log.debug("Fetching all active users - Page: {}, Size: {}", pageable.getPageNumber(), pageable.getPageSize());

        return ExceptionHelper.wrapDatabaseOperation(() ->
                        userRepository.findByActiveTrue(pageable).map(this::convertToResponseDTO),
                "fetch all users"
        );
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        log.debug("Fetching user with id: {}", id);

        validateUserId(id);

        User user = findUserByIdOrThrow(id);
        return convertToResponseDTO(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);

        validateEmail(email);

        User user = ExceptionHelper.findOrThrow(
                userRepository.findByEmail(email.trim().toLowerCase()).orElse(null),
                "User",
                "email: " + email
        );

        return convertToResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        log.info("Creating new user with email: {}", requestDTO.getEmail());

        String context = ExceptionHelper.createErrorContext(
                "createUser", "User", null, requestDTO.getEmail());

        return ExceptionHelper.wrapDatabaseOperation(() -> {
            validateUserRequest(requestDTO, true);

            checkForDuplicateUser(requestDTO);

            User user = createUserFromRequest(requestDTO);

            log.info("User created successfully with id: {} and email: {}",
                    user.getId(), user.getEmail());

            return convertToResponseDTO(user);

        }, context);
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO) {
        log.info("Updating user with id: {}", id);

        String context = ExceptionHelper.createErrorContext("updateUser", "User", id, null);

        return ExceptionHelper.wrapDatabaseOperation(() -> {
            validateUserId(id);
            validateUserRequest(requestDTO, false);

            User existingUser = findUserByIdOrThrow(id);

            checkForUserConflicts(requestDTO, id);

            updateUserFromRequest(existingUser, requestDTO);
            User updatedUser = userRepository.save(existingUser);

            log.info("User updated successfully with id: {}", id);
            return convertToResponseDTO(updatedUser);

        }, context);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        String context = ExceptionHelper.createErrorContext("deleteUser", "User", id, null);

        ExceptionHelper.wrapDatabaseOperation(() -> {
            validateUserId(id);

            User user = findUserByIdOrThrow(id);

            validateUserDeletion(user);

            user.setActive(false);
            userRepository.save(user);

            log.info("User soft deleted successfully with id: {}", id);
            return null;
        }, context);
    }

    public void permanentDeleteUser(Long id) {
        log.warn("Permanently deleting user with id: {}", id);

        String context = ExceptionHelper.createErrorContext("permanentDeleteUser", "User", id, null);

        ExceptionHelper.wrapDatabaseOperation(() -> {
            validateUserId(id);

            User user = findUserByIdOrThrow(id);

            validatePermanentUserDeletion(user);

            userRepository.deleteById(id);

            log.warn("User permanently deleted with id: {}", id);
            return null;
        }, context);
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }

        return !userRepository.existsByEmail(email.trim().toLowerCase());
    }

    public void changeUserStatus(Long id, boolean active) {
        log.info("Changing user status - ID: {}, Active: {}", id, active);

        String context = ExceptionHelper.createErrorContext("changeUserStatus", "User", id, "active: " + active);

        ExceptionHelper.wrapDatabaseOperation(() -> {
            validateUserId(id);

            User user = findUserByIdOrThrow(id);

            if (user.getActive().equals(active)) {
                throw new BusinessLogicException(
                        String.format("User is already %s", active ? "active" : "inactive"));
            }

            user.setActive(active);
            userRepository.save(user);

            log.info("User status changed successfully - ID: {}, New status: {}", id, active);
            return null;
        }, context);
    }

    // ========== PROFILE METHODS ==========

    @Transactional(readOnly = true)
    public UserResponseDTO getCurrentUserProfile() {
        User user = securityHelper.getCurrentUser();
        return convertToProfileResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO updateCurrentUserProfile(UserProfileUpdateDTO dto) {
        User user = securityHelper.getCurrentUser();

        if (StringUtils.hasText(dto.getFirstName())) {
            user.setFirstName(dto.getFirstName().trim());
        } else {
            user.setFirstName(null);
        }

        if (StringUtils.hasText(dto.getLastName())) {
            user.setLastName(dto.getLastName().trim());
        } else {
            user.setLastName(null);
        }

        if (StringUtils.hasText(dto.getPhone())) {
            user.setPhone(dto.getPhone().trim());
        } else {
            user.setPhone(null);
        }

        userRepository.save(user);
        log.info("Profile updated for user id: {}", user.getId());
        return convertToProfileResponseDTO(user);
    }

    @Transactional
    public void changePassword(ChangePasswordDTO dto) {
        User user = securityHelper.getCurrentUser();

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessLogicException("Current password is incorrect");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessLogicException("New password must differ from current password");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user id: {}", user.getId());
    }

    // ========== ADDRESS METHODS ==========

    @Transactional(readOnly = true)
    public List<UserAddressResponseDTO> getCurrentUserAddresses() {
        Long userId = securityHelper.getCurrentUserId();
        return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(this::toAddressResponse).collect(Collectors.toList());
    }

    @Transactional
    public UserAddressResponseDTO addAddress(UserAddressDTO dto) {
        User user = securityHelper.getCurrentUser();

        UserAddress address = new UserAddress();
        address.setUser(user);
        applyAddressDTO(address, dto);

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            userAddressRepository.clearDefaultForUser(user.getId(), 0L);
        }

        address = userAddressRepository.save(address);

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            userAddressRepository.clearDefaultForUser(user.getId(), address.getId());
        }

        log.info("Address added for user id: {}", user.getId());
        return toAddressResponse(address);
    }

    @Transactional
    public UserAddressResponseDTO updateAddress(Long addressId, UserAddressDTO dto) {
        Long userId = securityHelper.getCurrentUserId();

        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        applyAddressDTO(address, dto);

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            userAddressRepository.clearDefaultForUser(userId, addressId);
        }

        address = userAddressRepository.save(address);
        log.info("Address {} updated for user id: {}", addressId, userId);
        return toAddressResponse(address);
    }

    @Transactional
    public void deleteAddress(Long addressId) {
        Long userId = securityHelper.getCurrentUserId();

        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        userAddressRepository.delete(address);
        log.info("Address {} deleted for user id: {}", addressId, userId);
    }

    // ========== COMPANY METHODS ==========

    @Transactional(readOnly = true)
    public List<UserCompanyResponseDTO> getCurrentUserCompanies() {
        Long userId = securityHelper.getCurrentUserId();
        return userCompanyRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream().map(this::toCompanyResponse).collect(Collectors.toList());
    }

    @Transactional
    public UserCompanyResponseDTO addCompany(UserCompanyDTO dto) {
        User user = securityHelper.getCurrentUser();

        UserCompany company = new UserCompany();
        company.setUser(user);
        applyCompanyDTO(company, dto);

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            userCompanyRepository.clearDefaultForUser(user.getId(), 0L);
        }

        company = userCompanyRepository.save(company);

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            userCompanyRepository.clearDefaultForUser(user.getId(), company.getId());
        }

        log.info("Company added for user id: {}", user.getId());
        return toCompanyResponse(company);
    }

    @Transactional
    public UserCompanyResponseDTO updateCompany(Long companyId, UserCompanyDTO dto) {
        Long userId = securityHelper.getCurrentUserId();

        UserCompany company = userCompanyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));

        applyCompanyDTO(company, dto);

        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            userCompanyRepository.clearDefaultForUser(userId, companyId);
        }

        company = userCompanyRepository.save(company);
        log.info("Company {} updated for user id: {}", companyId, userId);
        return toCompanyResponse(company);
    }

    @Transactional
    public void deleteCompany(Long companyId) {
        Long userId = securityHelper.getCurrentUserId();

        UserCompany company = userCompanyRepository.findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + companyId));

        userCompanyRepository.delete(company);
        log.info("Company {} deleted for user id: {}", companyId, userId);
    }

    // ========== PRIVATE VALIDATION METHODS ==========

    private void validateUserId(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("User ID must be a positive number");
        }
    }

    private void validateUserRequest(UserRequestDTO requestDTO, boolean isCreate) {
        if (requestDTO == null) {
            throw new ValidationException("User request cannot be null");
        }

        validateEmail(requestDTO.getEmail());

        if (isCreate || StringUtils.hasText(requestDTO.getPassword())) {
            validatePassword(requestDTO.getPassword());
        }

        validateRole(requestDTO.getRole());

        validateOptionalFields(requestDTO);
    }

    private void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ValidationException("Email is required");
        }

        String trimmed = email.trim().toLowerCase();

        if (trimmed.length() > 200) {
            throw new ValidationException("Email cannot exceed 200 characters");
        }

        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException("Invalid email format");
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new ValidationException("Password is required");
        }

        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }

        if (password.length() > 100) {
            throw new ValidationException("Password cannot exceed 100 characters");
        }

        String[] weakPasswords = {"password", "12345678", "password123", "admin123"};
        if (Arrays.stream(weakPasswords).anyMatch(weak -> weak.equalsIgnoreCase(password))) {
            throw new ValidationException("Password is too common. Please choose a stronger password.");
        }
    }

    private void validateRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new ValidationException("Role is required");
        }

        try {
            User.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Invalid role. Valid roles are: " + Arrays.toString(User.Role.values()));
        }
    }

    private void validateOptionalFields(UserRequestDTO requestDTO) {
        if (StringUtils.hasText(requestDTO.getFirstName()) && requestDTO.getFirstName().length() > 100) {
            throw new ValidationException("First name cannot exceed 100 characters");
        }

        if (StringUtils.hasText(requestDTO.getLastName()) && requestDTO.getLastName().length() > 100) {
            throw new ValidationException("Last name cannot exceed 100 characters");
        }
    }

    private void validateUserDeletion(User user) {
        if (user.isSuperAdmin()) {
            throw new BusinessLogicException("Cannot delete super admin users");
        }

        validateUserDataDependencies(user);
    }

    private void validatePermanentUserDeletion(User user) {
        validateUserDeletion(user);

        if (user.getActive()) {
            throw new BusinessLogicException("User must be deactivated before permanent deletion");
        }
    }

    private void validateUserDataDependencies(User user) {
        if (user.getCartItems() != null && !user.getCartItems().isEmpty()) {
            log.info("User {} has {} cart items that will be deleted", user.getId(), user.getCartItems().size());
        }

        if (user.getFavorites() != null && !user.getFavorites().isEmpty()) {
            log.info("User {} has {} favorites that will be deleted", user.getId(), user.getFavorites().size());
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    private User findUserByIdOrThrow(Long id) {
        return ExceptionHelper.findOrThrow(
                userRepository.findById(id).orElse(null),
                "User",
                id
        );
    }

    private void checkForDuplicateUser(UserRequestDTO requestDTO) {
        if (userRepository.existsByEmail(requestDTO.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException(
                    "User with email '" + requestDTO.getEmail() + "' already exists");
        }
    }

    private void checkForUserConflicts(UserRequestDTO requestDTO, Long userId) {
        if (userRepository.existsByEmailAndIdNot(requestDTO.getEmail().trim().toLowerCase(), userId)) {
            throw new DuplicateResourceException(
                    "User with email '" + requestDTO.getEmail() + "' already exists");
        }
    }

    private User createUserFromRequest(UserRequestDTO requestDTO) {
        User user = new User();

        user.setEmail(requestDTO.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setFirstName(StringUtils.hasText(requestDTO.getFirstName()) ? requestDTO.getFirstName().trim() : null);
        user.setLastName(StringUtils.hasText(requestDTO.getLastName()) ? requestDTO.getLastName().trim() : null);
        user.setRole(User.Role.valueOf(requestDTO.getRole().toUpperCase()));
        user.setActive(requestDTO.getActive() != null ? requestDTO.getActive() : true);
        user.setEmailVerified(true);
        user = userRepository.save(user);
        return user;
    }

    private void updateUserFromRequest(User user, UserRequestDTO requestDTO) {
        user.setEmail(requestDTO.getEmail().trim().toLowerCase());

        if (StringUtils.hasText(requestDTO.getPassword())) {
            user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        }

        user.setFirstName(StringUtils.hasText(requestDTO.getFirstName()) ? requestDTO.getFirstName().trim() : null);
        user.setLastName(StringUtils.hasText(requestDTO.getLastName()) ? requestDTO.getLastName().trim() : null);
        user.setRole(User.Role.valueOf(requestDTO.getRole().toUpperCase()));

        if (requestDTO.getActive() != null) {
            user.setActive(requestDTO.getActive());
        }
    }

    private UserResponseDTO convertToResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .fullName(user.getFullName())
                .build();
    }

    private UserResponseDTO convertToProfileResponseDTO(User user) {
        List<UserAddressResponseDTO> addresses = userAddressRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream().map(this::toAddressResponse).collect(Collectors.toList());

        List<UserCompanyResponseDTO> companies = userCompanyRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream().map(this::toCompanyResponse).collect(Collectors.toList());

        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .active(user.getActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .fullName(user.getFullName())
                .addresses(addresses)
                .companies(companies)
                .build();
    }

    private void applyAddressDTO(UserAddress address, UserAddressDTO dto) {
        address.setLabel(StringUtils.hasText(dto.getLabel()) ? dto.getLabel().trim() : null);
        address.setType(dto.getType());
        address.setAddressLine(StringUtils.hasText(dto.getAddressLine()) ? dto.getAddressLine().trim() : null);
        address.setCity(StringUtils.hasText(dto.getCity()) ? dto.getCity().trim() : null);
        address.setPostalCode(StringUtils.hasText(dto.getPostalCode()) ? dto.getPostalCode().trim() : null);
        address.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        address.setCityId(StringUtils.hasText(dto.getCityId()) ? dto.getCityId().trim() : null);
        address.setOfficeId(StringUtils.hasText(dto.getOfficeId()) ? dto.getOfficeId().trim() : null);
    }

    private void applyCompanyDTO(UserCompany company, UserCompanyDTO dto) {
        company.setCompanyName(dto.getCompanyName().trim());
        company.setEik(StringUtils.hasText(dto.getEik()) ? dto.getEik().trim() : null);
        company.setVatNumber(StringUtils.hasText(dto.getVatNumber()) ? dto.getVatNumber().trim() : null);
        company.setMol(StringUtils.hasText(dto.getMol()) ? dto.getMol().trim() : null);
        company.setSeat(StringUtils.hasText(dto.getSeat()) ? dto.getSeat().trim() : null);
        company.setManagementAddress(StringUtils.hasText(dto.getManagementAddress()) ? dto.getManagementAddress().trim() : null);
        company.setCity(StringUtils.hasText(dto.getCity()) ? dto.getCity().trim() : null);
        company.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
    }

    private UserAddressResponseDTO toAddressResponse(UserAddress a) {
        return UserAddressResponseDTO.builder()
                .id(a.getId())
                .label(a.getLabel())
                .type(a.getType())
                .addressLine(a.getAddressLine())
                .city(a.getCity())
                .postalCode(a.getPostalCode())
                .isDefault(a.getIsDefault())
                .cityId(a.getCityId())
                .officeId(a.getOfficeId())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private UserCompanyResponseDTO toCompanyResponse(UserCompany c) {
        return UserCompanyResponseDTO.builder()
                .id(c.getId())
                .companyName(c.getCompanyName())
                .eik(c.getEik())
                .vatNumber(c.getVatNumber())
                .mol(c.getMol())
                .seat(c.getSeat())
                .managementAddress(c.getManagementAddress())
                .city(c.getCity())
                .isDefault(c.getIsDefault())
                .createdAt(c.getCreatedAt())
                .build();
    }
}