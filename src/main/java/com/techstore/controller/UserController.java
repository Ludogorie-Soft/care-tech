package com.techstore.controller;

import com.techstore.dto.UserResponseDTO;
import com.techstore.dto.request.ChangePasswordDTO;
import com.techstore.dto.request.MessageToAdmin;
import com.techstore.dto.request.UserAddressDTO;
import com.techstore.dto.request.UserCompanyDTO;
import com.techstore.dto.request.UserProfileUpdateDTO;
import com.techstore.dto.request.UserRequestDTO;
import com.techstore.dto.response.UserAddressResponseDTO;
import com.techstore.dto.response.UserCompanyResponseDTO;
import com.techstore.service.EmailService;
import com.techstore.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserResponseDTO> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping(value = "/email/{email}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        UserResponseDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO requestDTO) {
        UserResponseDTO createdUser = userService.createUser(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PutMapping(value = "/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO requestDTO) {

        log.info("Updating user with id: {}", id);
        UserResponseDTO updatedUser = userService.updateUser(id, requestDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getCurrentUserProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> updateCurrentUserProfile(
            @Valid @RequestBody UserProfileUpdateDTO dto) {
        return ResponseEntity.ok(userService.updateCurrentUserProfile(dto));
    }

    @PutMapping("/profile/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return ResponseEntity.noContent().build();
    }

    // ========== ADDRESSES ==========

    @GetMapping("/profile/addresses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserAddressResponseDTO>> getAddresses() {
        return ResponseEntity.ok(userService.getCurrentUserAddresses());
    }

    @PostMapping("/profile/addresses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserAddressResponseDTO> addAddress(@Valid @RequestBody UserAddressDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addAddress(dto));
    }

    @PutMapping("/profile/addresses/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserAddressResponseDTO> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody UserAddressDTO dto) {
        return ResponseEntity.ok(userService.updateAddress(id, dto));
    }

    @DeleteMapping("/profile/addresses/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        userService.deleteAddress(id);
        return ResponseEntity.noContent().build();
    }

    // ========== COMPANIES ==========

    @GetMapping("/profile/companies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserCompanyResponseDTO>> getCompanies() {
        return ResponseEntity.ok(userService.getCurrentUserCompanies());
    }

    @PostMapping("/profile/companies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserCompanyResponseDTO> addCompany(@Valid @RequestBody UserCompanyDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addCompany(dto));
    }

    @PutMapping("/profile/companies/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserCompanyResponseDTO> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody UserCompanyDTO dto) {
        return ResponseEntity.ok(userService.updateCompany(id, dto));
    }

    @DeleteMapping("/profile/companies/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        userService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}