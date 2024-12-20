package com.foodbookingplatform.security;

import com.foodbookingplatform.models.entities.User;
import com.foodbookingplatform.repositories.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Getter
@Setter
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userNameOrEmailOrPhone) throws UsernameNotFoundException {
        //allow user to log in by username or email
        User user = userRepository.findByUserNameOrEmailOrPhone(userNameOrEmailOrPhone, userNameOrEmailOrPhone, userNameOrEmailOrPhone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + userNameOrEmailOrPhone));

        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().getName());

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + authority));

        return new org.springframework.security.core.userdetails.User(userNameOrEmailOrPhone, user.getPassword(), authorities);
    }
}
