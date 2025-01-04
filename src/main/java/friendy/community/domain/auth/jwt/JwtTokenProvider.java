package friendy.community.domain.auth.jwt;

import friendy.community.global.exception.ErrorCode;
import friendy.community.global.exception.FriendyException;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final String EMAIL_KEY = "email";

    @Value("${jwt.access.secret}")
    private String jwtAccessTokenSecret;
    @Value("${jwt.access.expiration}")
    private long jwtAccessTokenExpirationInMs;

    @Value("${jwt.refresh.secret}")
    private String jwtRefreshTokenSecret;
    @Value("${jwt.refresh.expiration}")
    private long jwtRefreshTokenExpirationInMs;

    public String generateAccessToken(final String email) {
        final Date now = new Date();
        final Date expiryDate = new Date(now.getTime() + jwtAccessTokenExpirationInMs);
        final SecretKey secretKey = new SecretKeySpec(jwtAccessTokenSecret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());

        return Jwts.builder()
                .claim(EMAIL_KEY, email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(final String email) {
        final Date now = new Date();
        final Date expiryDate = new Date(now.getTime() + jwtRefreshTokenExpirationInMs);
        final SecretKey secretKey = new SecretKeySpec(jwtRefreshTokenSecret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());

        return Jwts.builder()
                .claim(EMAIL_KEY, email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public void validateAccessToken(final String token) {
        try {
            final Claims claims = getAccessTokenParser().parseClaimsJws(token).getBody();
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            final String logMessage = "인증 실패(잘못된 액세스 토큰) - 토큰 : " + token;
            throw new FriendyException(ErrorCode.UNAUTHORIZED_USER, logMessage);
        } catch (ExpiredJwtException e) {
            throw new FriendyException(ErrorCode.UNAUTHORIZED_USER, "액세스 토큰이 만료되었습니다.");
        }
    }

    public void validateRefreshToken(final String token) {
        try {
            final Claims claims = getRefreshTokenParser().parseClaimsJws(token).getBody();
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            final String logMessage = "인증 실패(잘못된 리프레시 토큰) - 토큰 : " + token;
            throw new FriendyException(ErrorCode.UNAUTHORIZED_USER, logMessage);
        } catch (ExpiredJwtException e) {
            throw new FriendyException(ErrorCode.UNAUTHORIZED_USER, "리프레시 토큰이 만료되었습니다.");
        }
    }

    private JwtParser getAccessTokenParser() {
        return Jwts.parserBuilder()
                .setSigningKey(jwtAccessTokenSecret.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    private JwtParser getRefreshTokenParser() {
        return Jwts.parserBuilder()
                .setSigningKey(jwtRefreshTokenSecret.getBytes(StandardCharsets.UTF_8))
                .build();
    }

}