package com.backend.water_management_system.security;

import com.backend.water_management_system.user.entity.User;
import com.backend.water_management_system.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /*
      Called by Spring Security during authentication.
      The "username" here is actually the NIC (our login identifier).
    */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nic) throws UsernameNotFoundException {
        User user = userRepository.findByNic(nic)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found for NIC: " + nic));
        return new UserPrincipal(user);
    }
}
