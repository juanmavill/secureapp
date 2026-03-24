package co.edu.escuelaing.secureapp;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AuthController {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // Usuario hardcodeado por ahora (password: "1234" hasheado)
    private final String usuarioValido = "admin";
    private final String hashValido = encoder.encode("1234");

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (usuarioValido.equals(username) && encoder.matches(password, hashValido)) {
            return ResponseEntity.ok(Map.of("mensaje", "Bienvenido, " + username + "!"));
        } else {
            return ResponseEntity.status(401).body(Map.of("mensaje", "Credenciales incorrectas"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("estado", "Backend corriendo correctamente"));
    }
}