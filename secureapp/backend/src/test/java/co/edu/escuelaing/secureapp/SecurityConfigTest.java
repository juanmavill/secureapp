package co.edu.escuelaing.secureapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:5173")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Solo /api/login y /api/health son publicos. Cualquier otra ruta queda
     * denegada aunque todavia no exista, de modo que anadir un endpoint no lo
     * expone por olvido.
     *
     * <p>La respuesta es 403 y no 401 porque no hay ningun mecanismo de
     * autenticacion configurado: /api/login valida credenciales pero no abre
     * sesion ni emite un token, asi que no existe forma de satisfacer
     * {@code authenticated()} y Spring no tiene con que desafiar al cliente.
     * Queda anotado como limitacion en el README.
     */
    @Test
    void deniesEverythingThatIsNotExplicitlyPublic() throws Exception {
        mockMvc.perform(get("/api/cuentas"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsTheConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", HttpMethod.POST.name()))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    /**
     * Con el comodin anterior cualquier sitio podia invocar la API desde el
     * navegador de un usuario autenticado.
     */
    @Test
    void rejectsAnUnlistedOrigin() throws Exception {
        mockMvc.perform(options("/api/login")
                        .header("Origin", "https://sitio-malicioso.example")
                        .header("Access-Control-Request-Method", HttpMethod.POST.name()))
                .andExpect(status().isForbidden());
    }
}
