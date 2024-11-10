package com.foodbookingplatform.security;

import com.foodbookingplatform.models.entities.User;
import com.foodbookingplatform.models.exception.RestaurantBookingException;
import com.foodbookingplatform.repositories.TokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.access-token-expiration}")
    private long accessTokenExpire;

    @Value("${application.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpire;

    private final TokenRepository tokenRepository;

    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenExpire);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, refreshTokenExpire );
    }

    public String generateToken(User user, long expireTime) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTime ))
                .signWith(key())
                .compact();
    }

    public boolean isValidRefreshToken(String token, String userName) {
        String username = getUsernameFromJwt(token);

        boolean validRefreshToken = tokenRepository
                .findByRefreshToken(token)
                .map(t -> !t.isLoggedOut())
                .orElse(false);

        return (username.equals(userName)) && !isTokenExpired(token) && validRefreshToken;
    }

    public boolean isValid(String token, UserDetails user) {
        String username = getUsernameFromJwt(token);

        boolean validToken = tokenRepository
                .findByAccessToken(token)
                .map(t -> !t.isLoggedOut())
                .orElse(false);

        return (username.equals(user.getUsername())) && !isTokenExpired(token) && validToken;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigninKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigninKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Key key() {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secretKey)
        );
    }

    public String getUsernameFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "JWT token is empty or null");
        }else{
            try {
                Jwts.parserBuilder()
                        .setSigningKey(key())
                        .build()
                        .parse(token);
                return true;
            } catch (MalformedJwtException e) {
                throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "Invalid JWT token");
            } catch (ExpiredJwtException e) {
                throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "Expired JWT token");
            } catch (UnsupportedJwtException e) {
                throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "Unsupported JWT token");
            } catch (IllegalArgumentException e) {
                throw new RestaurantBookingException(HttpStatus.BAD_REQUEST, "JWT claims string is empty");
            }
        }
    }


}
