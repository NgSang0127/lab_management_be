package org.sang.labmanagement.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.sang.labmanagement.redis.BaseRedisServiceImpl;
import org.sang.labmanagement.security.token.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

	private final TokenService tokenService;

	@Value("${application.security.jwt.expiration}")
	private long jwtExpiration;

	@Value("${application.security.jwt.secret-key}")
	private String secretKey;

	@Value("${application.security.jwt.refresh-token.expiration}")
	private long refreshExpiration;

//	@PostConstruct
//	public void init() {
//		System.out.println("JWT Secret Key: " + secretKey);
//	}

	public String generateToken(UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	public String generateToken(Map<String, Object> claims, UserDetails userDetails) {
		return buildToken(claims, userDetails, jwtExpiration);
	}
	public String generateRefreshToken(
			UserDetails userDetails
	) {
		String refreshToken=buildToken(new HashMap<>(), userDetails, refreshExpiration);
		tokenService.saveRefreshToken(userDetails.getUsername(),refreshToken);

		return refreshToken;
	}

	private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long jwtExpiration) {
		var authorities=userDetails.getAuthorities();
		return Jwts
				.builder()
				.setClaims(extraClaims)
				.setSubject(userDetails.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
				.claim("authorities", authorities)
				.signWith(getSignInKey(), SignatureAlgorithm.HS256)
				.compact();
	}
	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);


		if (tokenService.getBlackListAccess(token) || tokenService.getBlackListRefresh(token)) {
			return false; // Token đã bị thu hồi
		}

		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}


	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());//so sánh thời gian hết hạn với thời gian hiện tại
	}

	private Date extractExpiration(String token) {
		return extractClaims(token, Claims::getExpiration);
	}

	public String extractUsername(String token) {
		return extractClaims(token, Claims::getSubject);
	}

	public <T> T extractClaims(String token, Function<Claims, T> claimResolver) {
		final Claims claims = extractAllClaims(token);
		return claimResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		return Jwts
				.parserBuilder()
				.setSigningKey(getSignInKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}


	private Key getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
