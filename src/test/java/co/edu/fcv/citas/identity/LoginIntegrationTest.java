package co.edu.fcv.citas.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.fcv.citas.CitasApiApplication;
import co.edu.fcv.citas.support.AuthClient;
import co.edu.fcv.citas.support.TestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** HU-006 · Iniciar sesión (CA-01 a CA-04). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class LoginIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void clean() throws Exception {
        database.reset();
        auth.register(mvc, "ana@example.com", "2001");
    }

    @Test
    void ca01_ca04_issuesSeparateAccessInJsonAndRefreshInHttpOnlyCookieWithRoles() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\" ANA@example.com \",\"password\":\"" + AuthClient.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", Matchers.allOf(
                        Matchers.containsString("refresh_token="), Matchers.containsString("HttpOnly"),
                        Matchers.containsString("Path=/api/v1/auth"), Matchers.containsString("SameSite=Lax"))))
                .andReturn();
        String access = auth.accessToken(result);
        JsonNode claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(access.split("\\.")[1]));
        assertThat(claims.get("typ").asText()).isEqualTo("access");
        assertThat(claims.get("roles").toString()).isEqualTo("[\"USER\"]");
        assertThat(result.getResponse().getCookie("refresh_token").getValue()).isNotEqualTo(access);
    }

    @Test
    void ca02_ca04_unknownEmailAndWrongPasswordGetTheSameErrorWithoutTokens() throws Exception {
        MvcResult wrongPassword = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"otra-clave-1\"}"))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        MvcResult unknownEmail = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@example.com\",\"password\":\"otra-clave-1\"}"))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertThat(wrongPassword.getResponse().getContentAsString()).isEqualTo(unknownEmail.getResponse().getContentAsString())
                .doesNotContain("accessToken");
    }

    @Test
    void ca02_inactiveAccountCannotSignIn() throws Exception {
        db.update("update users set active=false where email='ana@example.com'");
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@example.com\",\"password\":\"" + AuthClient.PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ca03_authorizationUsesTheRoleInTheSessionContext() throws Exception {
        AuthClient.Session user = auth.login(mvc, "ana@example.com");
        mvc.perform(get("/api/v1/admin/inbox")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", user.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/catalogs/roles").header("Authorization", user.bearer())).andExpect(status().isOk());

        database.grantRole("ana@example.com", "ADMIN");
        AuthClient.Session admin = auth.login(mvc, "ana@example.com");
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", admin.bearer())).andExpect(status().isOk());
    }

    @Test
    void ca03_tamperedOrRefreshTokenIsNotAcceptedAsAccess() throws Exception {
        AuthClient.Session user = auth.login(mvc, "ana@example.com");
        mvc.perform(get("/api/v1/catalogs/roles").header("Authorization", "Bearer " + user.refreshCookie().getValue()))
                .andExpect(status().isUnauthorized());
        // Escalada de privilegios: se reemplaza el payload por uno con rol ADMIN conservando la firma original.
        String[] parts = user.accessToken().split("\\.");
        JsonNode original = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(parts[1]));
        String forgedPayload = "{\"sub\":\"" + original.get("sub").asText() + "\",\"roles\":[\"ADMIN\"],\"typ\":\"access\",\"exp\":"
                + original.get("exp").asLong() + "}";
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(forgedPayload.getBytes()) + "." + parts[2];
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }
}
