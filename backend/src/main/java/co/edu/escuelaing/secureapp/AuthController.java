package co.edu.escuelaing.secureapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String passwordHash;

    public AuthController(PasswordEncoder passwordEncoder,
                          @Value("${app.auth.username}") String username,
                          @Value("${app.auth.password-hash}") String passwordHash) {
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Usuario y contrasena son obligatorios"));
        }

        // Both conditions are always evaluated before deciding. Short-circuiting on
        // the username would let an unknown user answer without running BCrypt, and
        // that timing difference would reveal which usernames exist.
        boolean userMatches = constantTimeEquals(this.username, request.username());
        boolean passwordMatches = passwordEncoder.matches(request.password(), this.passwordHash);

        if (userMatches && passwordMatches) {
            return ResponseEntity.ok(Map.of("mensaje", "Bienvenido, " + request.username() + "!"));
        }
        // One single response for an unknown user and a wrong password:
        // telling them apart would allow enumerating valid accounts.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("mensaje", "Credenciales incorrectas"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("estado", "Backend corriendo correctamente"));
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
