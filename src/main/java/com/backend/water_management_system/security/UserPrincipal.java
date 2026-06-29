package com.backend.water_management_system.security;

import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/*
  Wraps our User entity so it can be used by Spring Security without
  coupling the User entity itself to any Spring Security interfaces.
*/
public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    // Spring Security uses this as the credential (i.e. the password field)
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    // NIC is the login identifier
    @Override
    public String getUsername() {
        return user.getNic();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Only ACTIVE accounts can authenticate
    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
