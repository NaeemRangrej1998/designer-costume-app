package com.designer.costume.security;

import com.designer.costume.entity.role.RoleEntity;
import com.designer.costume.entity.user.UserEntity;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final Set<String> roleNames;
    private final Set<String> permissionNames;
    private final Set<GrantedAuthority> authorities;

    private CustomUserDetails(
            UUID id,
            String email,
            String passwordHash,
            boolean active,
            Set<String> roleNames,
            Set<String> permissionNames) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.roleNames = Set.copyOf(roleNames);
        this.permissionNames = Set.copyOf(permissionNames);
        Set<GrantedAuthority> granted = new HashSet<>();
        roleNames.forEach(role -> granted.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissionNames.forEach(permission -> granted.add(new SimpleGrantedAuthority(permission)));
        this.authorities = Set.copyOf(granted);
    }

    public static CustomUserDetails from(UserEntity user, Set<String> permissions) {
        Set<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(java.util.stream.Collectors.toSet());

        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                roles,
                permissions);
    }

    public UUID getId() {
        return id;
    }

    public Set<String> getRoleNames() {
        return roleNames;
    }

    public Set<String> getPermissionNames() {
        return permissionNames;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
