package co.edu.escuelaing.secureapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The configured hash belongs to the password "devpassword".
 */
@SpringBootTest(properties = {
        "app.auth.username=admin",
        "app.auth.password-hash=$2a$10$A1VoAZZ17jo4NbtlI1LcoeKcAIH1EsLmELxPLIm3KO0bC..JSBffG",
        "app.cors.allowed-origins=http://localhost:5173"
})
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String VALID_PASSWORD = "devpassword";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsValidCredentials() throws Exception {
        mockMvc.perform(login("admin", VALID_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Bienvenido, admin!"));
    }

    @Test
    void rejectsAWrongPassword() throws Exception {
        mockMvc.perform(login("admin", "not-the-password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAnUnknownUser() throws Exception {
        mockMvc.perform(login("intruder", VALID_PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    /**
     * An unknown user and a wrong password must be indistinguishable from the
     * outside: if they differed, an attacker could learn which users exist.
     */
    @Test
    void doesNotRevealWhetherTheUserExists() throws Exception {
        MvcResult unknownUser = mockMvc.perform(login("intruder", VALID_PASSWORD)).andReturn();
        MvcResult wrongPassword = mockMvc.perform(login("admin", "not-the-password")).andReturn();

        assertThat(unknownUser.getResponse().getStatus())
                .isEqualTo(wrongPassword.getResponse().getStatus());
        assertThat(unknownUser.getResponse().getContentAsString())
                .isEqualTo(wrongPassword.getResponse().getContentAsString());
    }

    @Test
    void rejectsARequestWithoutCredentials() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void neverEchoesThePasswordBack() throws Exception {
        MvcResult result = mockMvc.perform(login("admin", VALID_PASSWORD)).andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(VALID_PASSWORD);
    }

    @Test
    void exposesHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
            String username, String password) {
        return post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }
}
