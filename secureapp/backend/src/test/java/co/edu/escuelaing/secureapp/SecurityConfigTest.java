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
     * Only /api/login and /api/health are public. Any other path is denied even if
     * it does not exist yet, so adding an endpoint does not expose it by omission.
     *
     * <p>The response is 403 rather than 401 because no authentication mechanism
     * is configured: /api/login validates credentials but neither opens a session
     * nor issues a token, so there is no way to satisfy {@code authenticated()}
     * and Spring has nothing to challenge the client with. Recorded as a known
     * limitation in the README.
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
     * With the previous wildcard, any site could call the API from a signed-in
     * user's browser.
     */
    @Test
    void rejectsAnUnlistedOrigin() throws Exception {
        mockMvc.perform(options("/api/login")
                        .header("Origin", "https://sitio-malicioso.example")
                        .header("Access-Control-Request-Method", HttpMethod.POST.name()))
                .andExpect(status().isForbidden());
    }
}
