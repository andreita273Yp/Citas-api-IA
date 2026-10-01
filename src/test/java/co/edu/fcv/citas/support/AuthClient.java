package co.edu.fcv.citas.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Atajos de registro e inicio de sesión para pruebas de integración REST. */
@Component
public class AuthClient {
    public static final String PASSWORD = "password-segura";
    private final ObjectMapper json = new ObjectMapper();

    public String registrationJson(String email, String documentNumber) {
        return """
                {"firstName":"Ana","lastName":"Prueba","documentType":"CC","documentNumber":"%s","email":"%s","phone":"3001234567","password":"%s"}
                """.formatted(documentNumber, email, PASSWORD);
    }

    public void register(MockMvc mvc, String email, String documentNumber) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registrationJson(email, documentNumber)))
                .andExpect(status().isCreated());
    }

    public Session login(MockMvc mvc, String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return new Session(accessToken(result), result.getResponse().getCookie("refresh_token"));
    }

    public String accessToken(MvcResult result) throws Exception {
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    public record Session(String accessToken, Cookie refreshCookie) {
        public String bearer() { return "Bearer " + accessToken; }
    }
}
