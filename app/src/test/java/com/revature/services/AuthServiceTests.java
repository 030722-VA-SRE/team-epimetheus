package com.revature.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.revature.exceptions.AuthenticationException;
import com.revature.exceptions.AuthorizationException;
import com.revature.models.User;
import com.revature.models.User.UserRole;
import com.revature.repositories.UserRepository;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class AuthServiceTests {
	
	private static UserRepository mockRepo;
	private static PasswordEncoder pe;
	private static AuthService as;
	private static User admin;
	private static User user;
	private static String adminToken;
	private static String userToken;
	private static String badToken;
	
	@BeforeAll
	public static void setup() {
		mockRepo = mock(UserRepository.class);
		pe = mock(PasswordEncoder.class);
		as = new AuthService(mockRepo, pe, "this string is a fake secret key");
		admin = new User(1, "admin", "mail@inter.net", "1234asdf", UserRole.ADMIN);
		user = new User(2, "user", "ex@mple.com", "p4ssw0rd", UserRole.USER);
		
		adminToken = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwic3ViIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4ifQ.2S3t4AOfx4RPKF7sP9TKCkYdC60cknKNuxTUKfcMNd0";
		userToken = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6Miwic3ViIjoidXNlciIsInJvbGUiOiJVU0VSIn0.rdSk6AyqHe_l8JxQ-KMu-t1E-T-bO9FbbCYyTjcmUtk";
		badToken = "eyJhbGciOiJIUzI1NiJ9.eyJpZCI6MSwic3ViIjoiYWRtaW4iLCJyb2xlIjoiQURNSU4ifQ.2S3t4fx4RPKF7sP9TKCkYdC60cknKNuxTUKfcMNd0";
	}
	
	@Test
	void loginTestX0() {
		assertThrows(AuthenticationException.class, () -> {
			as.login(null, "1234asdf");
		});
		assertThrows(AuthenticationException.class, () -> {
			as.login("", "1234asdf");
		});
	}
	
	@Test
	void loginTestX1() {
		
		assertThrows(AuthenticationException.class, () -> {
			as.login("admin", null);
		});
		assertThrows(AuthenticationException.class, () -> {
			as.login("admin", "");
		});
	}
	
	@Test
	void loginTest0() {
		when(mockRepo.findByUsernameOrEmail("admin", "admin")).thenReturn(Optional.of(admin));
		when(pe.matches(any(), anyString())).thenReturn(true);
		assertDoesNotThrow(() -> {
			assertEquals(adminToken, as.login("admin", "1234asdf"));
		});
	}
	
	@Test
	void loginTest1() {
		when(mockRepo.findByUsernameOrEmail("ex@mple.com", "ex@mple.com")).thenReturn(Optional.of(user));
		when(pe.matches(any(), anyString())).thenReturn(true);
		assertDoesNotThrow(() -> {
			assertEquals(userToken, as.login("ex@mple.com", "p4ssw0rd"));
		});
	}
	
	@Test
	void authorizeUserTestX0() {
		assertThrows(AuthorizationException.class, () -> {
			as.authorizeUser(null, 1);
		});
	}
	
	@Test
	void authorizeUserTestX1() {
		assertDoesNotThrow(() -> {
			assertEquals(false, as.authorizeUser(userToken, -1));			
		});
	}
	
	@Test
	void authorizeUserTest0() {
		assertDoesNotThrow(() -> {
			assertEquals(true, as.authorizeUser(adminToken, 1));			
		});
	}
	
	@Test
	void authorizeRoleTestX0() {
		assertThrows(AuthorizationException.class, () -> {
			as.authorizeRole(null, UserRole.USER);
		});
	}
	
	@Test
	void authorizeRoleTestX1() {
		assertDoesNotThrow(() -> {
			assertEquals(false, as.authorizeRole(userToken, UserRole.STAFF, UserRole.ADMIN));
		});
	}
	
	@Test
	void authorizeRoleTest0() {
		assertDoesNotThrow(() -> {
			assertEquals(true, as.authorizeRole(userToken));
			assertEquals(true, as.authorizeRole(userToken, UserRole.NOT_SET));
		});
	}
	
	@Test
	void authorizeRoleTest1() {
		assertDoesNotThrow(() -> {
			assertEquals(true, as.authorizeRole(adminToken, UserRole.ADMIN));			
		});
	}
	
	@Test
	void verifyPasswordTestX() {
		when(mockRepo.findById(0)).thenReturn(Optional.empty());
		assertThrows(AuthorizationException.class, () -> {
			as.verifyPassword("0:ADMIN", "p4ssw0rd");
		});
		assertThrows(AuthorizationException.class, () -> {
			as.verifyPassword(null, null);
		});
	}
	
	@Test
	void verifyPasswordTest0() {
		when(mockRepo.findById(1)).thenReturn(Optional.of(admin));
		when(pe.matches(anyString(), anyString())).thenReturn(false);
		assertDoesNotThrow(() -> {
			assertEquals(false, as.verifyPassword(adminToken, user.getPassword()));
		});
	}
	
	@Test
	void verifyPasswordTest1() {
		when(mockRepo.findById(1)).thenReturn(Optional.of(admin));
		when(pe.matches(anyString(), anyString())).thenReturn(true);
		assertDoesNotThrow(() -> {
			assertEquals(true, as.verifyPassword(adminToken, admin.getPassword()));
		});
	}
	
	@Test
	void extractIdFromTokenX() {
		assertThrows(AuthorizationException.class, () -> {
			as.extractIdFromToken(null);
		});
		assertThrows(AuthorizationException.class, () -> {
			as.extractIdFromToken(badToken);
		});
	}
	
	@Test
	void extractIdFromToken0() {
		assertDoesNotThrow(() -> {
			assertEquals(1, as.extractIdFromToken(adminToken));			
		});
	}
	
	@Test
	void extractUsernameFromToken0() {
		assertDoesNotThrow(() -> {
			assertEquals("[Unknown]", as.extractUsernameFromToken(null));
			assertEquals("[Unknown]", as.extractUsernameFromToken(badToken));
		});
	}
	
	@Test
	void extractUsernameFromToken1() {
		assertDoesNotThrow(() -> {
			assertEquals("admin", as.extractUsernameFromToken(adminToken));			
		});
	}
	
	@Test
	void extractRoleFromTokenX() {
		assertThrows(AuthorizationException.class, () -> {
			as.extractRoleFromToken(null);
		});
		assertThrows(AuthorizationException.class, () -> {
			as.extractRoleFromToken(badToken);
		});
	}
	
	@Test
	void extractRoleFromToken0() {
		assertDoesNotThrow(() -> {
			assertEquals(UserRole.ADMIN, as.extractRoleFromToken(adminToken));			
		});
	}
}





