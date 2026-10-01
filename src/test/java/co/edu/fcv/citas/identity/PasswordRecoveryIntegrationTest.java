package co.edu.fcv.citas.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.fcv.citas.CitasApiApplication;
import co.edu.fcv.citas.support.AuthClient;
import co.edu.fcv.citas.support.TestDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** HU-008 Solicitar recuperación · HU-009 Restablecer contraseña (RF-03). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class PasswordRecoveryIntegrationTest {
    private static final String NEW_PASSWORD = "nueva-clave-segura";
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();
    private AuthClient.Session admin;

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        admin = auth.admin(mvc, database, "admin@example.com", "9201");
        auth.register(mvc, "ana@example.com", "9202");
    }

    // ---------------------------------------------------------------- HU-008

    @Test
    void hu008_ca01_ca02_requestIsGenericAndCreatesATemporarySingleUseTokenStoredOnlyAsHash() throws Exception {
        MvcResult known = recover("ANA@example.com ").andExpect(status().isAccepted()).andReturn();
        MvcResult unknown = recover("nadie@example.com").andExpect(status().isAccepted()).andReturn();
        assertThat(known.getResponse().getContentAsString()).isEqualTo(unknown.getResponse().getContentAsString()).doesNotContain("token");

        String token = latestToken("ana@example.com");
        assertThat(db.queryForObject("select count(*) from password_reset_tokens", Integer.class)).isEqualTo(1);
        String stored = db.queryForObject("select token_hash from password_reset_tokens", String.class);
        assertThat(stored).hasSize(64).isNotEqualTo(token).doesNotContain(token);
        assertThat(db.queryForObject("select expires_at > now() and expires_at <= now() + interval 31 minute and used_at is null from password_reset_tokens", Boolean.class))
                .isTrue();
    }

    @Test
    void hu008_ca03_localMailboxIsTheOnlyDeliveryChannelAndIsAdminOnly() throws Exception {
        recover("ana@example.com").andExpect(status().isAccepted());
        AuthClient.Session user = auth.login(mvc, "ana@example.com");
        mvc.perform(get("/api/v1/admin/local-mailbox/password-recovery").header("Authorization", user.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/local-mailbox/password-recovery")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/local-mailbox/password-recovery").header("Authorization", admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].email").value("ana@example.com")).andExpect(jsonPath("$[0].token").isNotEmpty());
    }

    @Test
    void hu008_aNewRequestInvalidatesThePreviousToken() throws Exception {
        recover("ana@example.com");
        String first = latestToken("ana@example.com");
        recover("ana@example.com");
        String second = latestToken("ana@example.com");
        assertThat(second).isNotEqualTo(first);
        reset(first, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isUnauthorized());
        reset(second, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------- HU-009

    @Test
    void hu009_ca01_validTokenChangesThePasswordForLaterSignIn() throws Exception {
        recover("ana@example.com");
        reset(latestToken("ana@example.com"), NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());
        login(NEW_PASSWORD).andExpect(status().isOk());
        login(AuthClient.PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void hu009_ca02_usedExpiredOrInvalidTokensAreRejectedWithoutChangingThePassword() throws Exception {
        recover("ana@example.com");
        String token = latestToken("ana@example.com");
        reset("token-inventado", NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isUnauthorized());
        reset(token, NEW_PASSWORD, "otra-confirmacion").andExpect(status().isBadRequest());
        reset(token, "corta", "corta").andExpect(status().isBadRequest());

        db.update("update password_reset_tokens set expires_at = now() - interval 1 minute");
        reset(token, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isUnauthorized());
        login(AuthClient.PASSWORD).andExpect(status().isOk());

        recover("ana@example.com");
        String fresh = latestToken("ana@example.com");
        reset(fresh, NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());
        reset(fresh, "tercera-clave-segura", "tercera-clave-segura").andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void hu009_ca03_passwordIsStoredAsBcryptAndOpenSessionsAreRevoked() throws Exception {
        AuthClient.Session open = auth.login(mvc, "ana@example.com");
        recover("ana@example.com");
        reset(latestToken("ana@example.com"), NEW_PASSWORD, NEW_PASSWORD).andExpect(status().isNoContent());

        String hash = db.queryForObject("select password_hash from users where email='ana@example.com'", String.class);
        assertThat(hash).startsWith("$2").doesNotContain(NEW_PASSWORD);
        assertThat(db.queryForObject("select count(*) from refresh_tokens rt join users u on u.id=rt.user_id where u.email='ana@example.com' and rt.revoked_at is null", Integer.class))
                .isZero();
        mvc.perform(post("/api/v1/auth/refresh").cookie(open.refreshCookie())).andExpect(status().isUnauthorized());
        assertThat(db.queryForObject("select used_at is not null from password_reset_tokens", Boolean.class)).isTrue();
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions recover(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-recovery").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions reset(String token, String password, String confirmation) throws Exception {
        return mvc.perform(post("/api/v1/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"" + password + "\",\"confirmation\":\"" + confirmation + "\"}"));
    }

    private ResultActions login(String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.com\",\"password\":\"" + password + "\"}"));
    }

    /** Lee el último token entregado al buzón local (canal de desarrollo aprobado). */
    private String latestToken(String email) throws Exception {
        String body = mvc.perform(get("/api/v1/admin/local-mailbox/password-recovery").header("Authorization", admin.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (JsonNode message : json.readTree(body)) if (email.equals(message.get("email").asText())) return message.get("token").asText();
        throw new AssertionError("No hay token para " + email);
    }
}
