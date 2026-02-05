package org.example.companyboiler.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.example.companyboiler.service.JwtService;
import org.example.companyboiler.service.LdapAuthService;
import org.example.companyboiler.service.LdapAuthService.LdapUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LdapAuthService ldapAuthService;
    private final JwtService jwtService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthController(LdapAuthService ldapAuthService, JwtService jwtService) {
        this.ldapAuthService = ldapAuthService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        LdapUserInfo userInfo = ldapAuthService.authenticate(request.username(), request.password());

        if (userInfo == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid credentials",
                    "message", "Username or password is incorrect"
            ));
        }

        String token = jwtService.generateToken(userInfo.username());

        // Set HTTP-only cookie for security
        Cookie cookie = new Cookie("auth_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration / 1000)); // Convert ms to seconds
        response.addCookie(cookie);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("token", token);
        responseBody.put("user", Map.of(
                "username", userInfo.username(),
                "displayName", userInfo.displayName(),
                "email", userInfo.email() != null ? userInfo.email() : ""
        ));

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear the auth cookie
        Cookie cookie = new Cookie("auth_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of(
                    "authenticated", false,
                    "message", "Not authenticated"
            ));
        }

        String username = (String) auth.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "user", Map.of(
                        "username", username
                )
        ));
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAuthenticated = auth != null &&
                auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal());

        return ResponseEntity.ok(Map.of("authenticated", isAuthenticated));
    }

    public record LoginRequest(String username, String password) {}
}
