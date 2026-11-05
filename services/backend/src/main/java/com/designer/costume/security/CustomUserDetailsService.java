package com.designer.costume.security;

import com.designer.costume.entity.user.UserEntity;
import com.designer.costume.exception.ResourceNotFoundException;
import com.designer.costume.repository.user.UserRepository;
import com.designer.costume.repository.user.UserRolePermissionGrantRepository;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRolePermissionGrantRepository permissionGrantRepository;

    public CustomUserDetailsService(
            UserRepository userRepository,
            UserRolePermissionGrantRepository permissionGrantRepository) {
        this.userRepository = userRepository;
        this.permissionGrantRepository = permissionGrantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findWithAuthoritiesByEmail(email)
                .map(user -> CustomUserDetails.from(
                        user, permissionGrantRepository.findEffectivePermissions(user.getId())))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadActiveUserById(UUID id) {
        UserEntity user = userRepository.findWithAuthoritiesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!user.isActive()) {
            throw new UsernameNotFoundException("User is inactive");
        }
        return CustomUserDetails.from(
                user, permissionGrantRepository.findEffectivePermissions(user.getId()));
    }
}
