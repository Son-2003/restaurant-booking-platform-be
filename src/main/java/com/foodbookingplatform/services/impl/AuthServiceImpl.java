package com.foodbookingplatform.services.impl;

import com.foodbookingplatform.models.constants.AppConstants;
import com.foodbookingplatform.models.entities.Location;
import com.foodbookingplatform.models.entities.Role;
import com.foodbookingplatform.models.entities.Token;
import com.foodbookingplatform.models.entities.User;
import com.foodbookingplatform.models.enums.EntityStatus;
import com.foodbookingplatform.models.exception.RestaurantBookingException;
import com.foodbookingplatform.models.exception.ResourceNotFoundException;
import com.foodbookingplatform.models.payload.dto.auth.JWTAuthResponse;
import com.foodbookingplatform.models.payload.dto.auth.LoginDto;
import com.foodbookingplatform.models.payload.dto.auth.SignupDto;
import com.foodbookingplatform.models.payload.dto.user.UserResponse;
import com.foodbookingplatform.repositories.LocationRepository;
import com.foodbookingplatform.repositories.RoleRepository;
import com.foodbookingplatform.repositories.TokenRepository;
import com.foodbookingplatform.repositories.UserRepository;
import com.foodbookingplatform.security.JwtTokenProvider;
import com.foodbookingplatform.services.AuthService;
import com.foodbookingplatform.services.EmailService;
import com.foodbookingplatform.utils.AutomaticGeneratedPassword;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper mapper;
    private final EmailService emailService;


    @Override
    public JWTAuthResponse authenticateUser(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUserNameOrEmailOrPhone(), loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByUserNameOrEmailOrPhone(loginDto.getUserNameOrEmailOrPhone(), loginDto.getUserNameOrEmailOrPhone(), loginDto.getUserNameOrEmailOrPhone()).orElseThrow(
                () -> new ResourceNotFoundException("User")
        );
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        revokeAllTokenByUser(user);
        saveUserToken(accessToken, refreshToken, user);
        return new JWTAuthResponse(accessToken, refreshToken, "User login was successful");
    }

    @Override
    public JWTAuthResponse signupUser(SignupDto signupDto) {
        User user = setUpUser(signupDto);

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RestaurantBookingException(HttpStatus.BAD_REQUEST, "User Role not set."));
        user.setRole(userRole);
        user.setPassword(passwordEncoder.encode(signupDto.getPassword()));
        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        saveUserToken(accessToken, refreshToken, user);

        return new JWTAuthResponse(accessToken, refreshToken,"User registration was successful");
    }

    @Override
    public JWTAuthResponse signupLocation(SignupDto signupDto) {
        User user = setUpUser(signupDto);
        String randomPassword = AutomaticGeneratedPassword.generateRandomPassword();

        Role userRole = roleRepository.findByName("LOCATION_ADMIN")
                .orElseThrow(() -> new RestaurantBookingException(HttpStatus.BAD_REQUEST, "User Role not set."));
        user.setRole(userRole);
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setFirstLogin(true);
        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        saveUserToken(accessToken, refreshToken, user);

        String content = "<html>" +
                "<head>" +
                "<style>" +
                "table { width: 100%; border-collapse: collapse; }" +
                "th, td { padding: 10px; border: 1px solid #ddd; text-align: left; }" +
                "th { background-color: #f2f2f2; }" +
                "body { font-family: Arial, sans-serif; }" +
                ".highlight { font-weight: bold; color: red; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<p class='greeting'>Hi, " + user.getFullName() + ",</p>" +
                "<p>Your SkedEat system account has been successfully created.</p>" +
                "<p>Please log into the system with the following information:</p>" +
                "<table>" +
                "<tr><th>Username</th><td>" + user.getEmail() + "</td></tr>" +
                "<tr><th>Password</th><td class='highlight'>" + randomPassword + "</td></tr>" +
                "</table>" +
                "<p><strong>Note:</strong> Please change your password after logging in.</p>" +
                "</body>" +
                "</html>";
        emailService.sendEmail(user.getEmail(), "[SkedEat] - Account Successfully Created", content);

        return new JWTAuthResponse(accessToken, refreshToken,"User registration was successful");
    }

    @Override
    public UserResponse getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        User user = userRepository.findByUserNameOrEmailOrPhone(userName, userName, userName)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        List<Location> location = locationRepository.getLocationsByUserId(user.getId());
        return mapToResponse(user , location.size() == 0 ? 0 : location.get(0).getId());
    }

    @Override
    public ResponseEntity<JWTAuthResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // extract the token from authorization header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // extract username from token
        String username = jwtTokenProvider.getUsernameFromJwt(token);

        // check if the user exist in database
        User user = userRepository.findByUserNameOrEmailOrPhone(username, username, username)
                .orElseThrow(()->new RuntimeException("No user found"));

        // check if the token is valid
        if(jwtTokenProvider.isValidRefreshToken(token, user.getUserName())) {
            // generate access token
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            revokeAllTokenByUser(user);
            saveUserToken(accessToken, refreshToken, user);

            return new ResponseEntity<>(new JWTAuthResponse(accessToken, refreshToken, "New token generated"), HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUserNameOrEmailOrPhone(username, username, username)
                .orElseThrow(() -> new RestaurantBookingException(HttpStatus.NOT_FOUND, "User cannot found!"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "Old password does not match!");
        }
        if(!newPassword.matches(AppConstants.PASSWORD_REGEX))
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST,
                    "Password must have at least 8 characters with at least one uppercase letter, one number, and one special character (!@#$%^&*).");
        user.setPassword(passwordEncoder.encode(newPassword));
        if(user.isFirstLogin()) user.setFirstLogin(false);
        userRepository.save(user);
    }

    private User setUpUser(SignupDto signupDto) {
        // add check if email already exists
        if (userRepository.existsByEmail(signupDto.getEmail())) {
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "Email is already exist!");
        }

        User user = new User();
        user.setUserName(signupDto.getUsername());
        user.setEmail(signupDto.getEmail());
        user.setFullName(signupDto.getFullName());
        user.setPassword(signupDto.getPassword());
        user.setPhone(signupDto.getPhone());
        user.setGender(signupDto.getGender());
        user.setImage("https://res.cloudinary.com/dpysbryyk/image/upload/v1717827115/Milk/UserDefault/dfzhxjcbnixmp8aybnge.jpg");
        user.setFirstLogin(false);
        user.setStatus(EntityStatus.ACTIVE);
        user.setPoint(0);

        return user;
    }

    private void saveUserToken(String accessToken, String refreshToken, User user) {
        Token token = new Token();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setLoggedOut(false);
        token.setUser(user);
        tokenRepository.save(token);
    }

    private void revokeAllTokenByUser(User user) {
        List<Token> validTokens = tokenRepository.findAllByUser_Id(user.getId());
        if(validTokens.isEmpty()) {
            return;
        }
        validTokens.forEach(t-> t.setLoggedOut(true));

        tokenRepository.saveAll(validTokens);
    }

    private UserResponse mapToResponse(User user, Long locationId){
        UserResponse userResponse = mapper.map(user, UserResponse.class);
        userResponse.setLocationId(locationId);

        return userResponse;
    }
}
