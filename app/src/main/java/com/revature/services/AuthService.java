package com.revature.services;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.revature.exceptions.AuthenticationException;
import com.revature.exceptions.AuthorizationException;
import com.revature.models.User;
import com.revature.models.User.UserRole;
import com.revature.repositories.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.micrometer.core.annotation.Timed;

@Service
public class AuthService {
	
	private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);
	
	private UserRepository ur;
	private PasswordEncoder pe;
	private Key secretKey;
	
	final String secretX;
	
	
	@Autowired
	public AuthService(UserRepository ur, PasswordEncoder pe, @Value("${security.jwt.token.secret-key}") final String secretX) {
		
		this.secretX = secretX;
		this.ur = ur;
		this.pe = pe;
		String secret = secretX;
		secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}
	
	// Returns token as string iff successful
	@Timed(value="login.time")
	public String login(String user, String password) {
		if (StringUtils.isBlank(user)) {
			throw new AuthenticationException("Username or email was not provided");
		}
		if (StringUtils.isBlank(password)) {
			throw new AuthenticationException("Password was not provided");
		}
		User userObj = ur.findByUsernameOrEmail(user, user)
		                 .orElseThrow(() -> 
		                	 new AuthenticationException("Could not find user with username or email of: " + user)
		                 );
		if (!pe.matches(password, userObj.getPassword())) {
			throw new AuthenticationException("Incorrect password");
		}
		return generateToken(userObj);
	}
	
	public boolean authorizeUser(String token, int userId) {
		if (StringUtils.isBlank(token)) {
			throw new AuthorizationException("Null token");
		}
		return userId == extractIdFromToken(token);
	}
	
	public boolean authorizeRole(String token, UserRole... roles) {
		// Allow authorization by default if no roles are specified
		if (roles.length <= 0) {
			return true;
		}
		if (StringUtils.isBlank(token)) {
			throw new AuthorizationException("Null token");
		}
		UserRole role = extractRoleFromToken(token);
		for (UserRole r : roles) {
			if (role == r || r == UserRole.NOT_SET) {
				return true;
			}
		}
		return false;
	}
	
	public boolean verifyPassword(String token, String password) {
		
		
		if (StringUtils.isBlank(token)) {
			throw new AuthorizationException("Null token");
		}
		User user = ur.findById(extractIdFromToken(token))
					  .orElseThrow(() -> 
	           	          new AuthorizationException("Could not find user from token") );
		return pe.matches(password, user.getPassword());
	}
	
	public int extractIdFromToken(String token) {
		try {
			Jws<Claims> jws = Jwts.parserBuilder()
			                      .setSigningKey(secretKey)
			                      .build()
			                      .parseClaimsJws(token);
			return jws.getBody().get("id", Integer.class);
		} catch (SignatureException e) {
			LOG.warn("Access was denied because token signature did not match");
			throw new AuthorizationException("Token signature does not match");
		} catch (Exception e) {
			LOG.warn("Access was denied because token was invalid");
			throw new AuthorizationException("Invalid token", e);
		}
	}
	
	public UserRole extractRoleFromToken(String token) {		
		try {
			Jws<Claims> jws = Jwts.parserBuilder()
			                      .setSigningKey(secretKey)
			                      .build()
			                      .parseClaimsJws(token);
			return UserRole.valueOf(jws.getBody().get("role", String.class));
		} catch (SignatureException e) {
			LOG.warn("Access was denied because token signature did not match");
			throw new AuthorizationException("Token signature does not match");
		} catch (Exception e) {
			LOG.warn("Access was denied because token was invalid");
			throw new AuthorizationException("Invalid token", e);
		}
	}
	
	public String extractUsernameFromToken(String token) {
		String unknown = "[Unknown]";
		if (StringUtils.isBlank(token)) {
			return unknown;
		}
		try {
			Jws<Claims> jws = Jwts.parserBuilder()
			                      .setSigningKey(secretKey)
			                      .build()
			                      .parseClaimsJws(token);
			return jws.getBody().get("sub", String.class);
		} catch (Exception e) {
			LOG.warn("Failed to read username from token; user is unknown");
			return unknown;
		}
	}
	
	private String generateToken(User user) {
		LOG.debug("generateToken method called");
		if (user == null) return null;
		
		String jws = Jwts.builder()
		                 .claim("id", user.getId())
		                 .setSubject(user.getUsername())
		                 .claim("role", user.getRole())
		                 .signWith(secretKey, SignatureAlgorithm.HS256)
		                 .compact();
		LOG.info("New JWT was generated for user: {}", user.getUsername());
		return jws;
	}
}
