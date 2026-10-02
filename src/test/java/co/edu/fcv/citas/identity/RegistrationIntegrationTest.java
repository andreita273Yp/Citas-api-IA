package co.edu.fcv.citas.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.fcv.citas.CitasApiApplication;
import co.edu.fcv.citas.support.AuthClient;
import co.edu.fcv.citas.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** HU-005 · Registrar usuario (CA-01 a CA-04). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class RegistrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired JdbcTemplate db;

    @BeforeEach
    void clean() {
        database.reset();
    }

    @Test
    void ca01_createsUserAccountWithOnlyUserRoleAndWithoutExposingPassword() throws Exception {
        String body = """
                {"firstName":" Ana ","lastName":"Prueba","documentType":"cc","documentNumber":"1001","email":"ana@example.com",
                 "phone":"3001234567","password":"password-segura","roles":["ADMIN"]}
                """;
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(db.queryForList("select r.code from user_roles ur join roles r on r.id=ur.role_id", String.class))
                .containsExactly("USER");
    }

    @Test
    void ca04_storesPasswordOnlyAsBcryptHash() throws Exception {
        auth.register(mvc, "hash@example.com", "1002");
        String stored = db.queryForObject("select password_hash from users where email='hash@example.com'", String.class);
        assertThat(stored).startsWith("$2").isNotEqualTo(AuthClient.PASSWORD).doesNotContain(AuthClient.PASSWORD);
    }

    @Test
    void ca02_ca04_rejectsSameEmailIgnoringCaseAndSpacesWithoutCreatingAnotherAccount() throws Exception {
        auth.register(mvc, "ana@example.com", "1003");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(auth.registrationJson("  ANA@Example.COM ", "9999")))
                .andExpect(status().isConflict());
        assertThat(userCount()).isEqualTo(1);
    }

    @Test
    void ca02_ca04_rejectsSameDocumentTypeAndNumberWithoutCreatingAnotherAccount() throws Exception {
        auth.register(mvc, "uno@example.com", "1004");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(auth.registrationJson("dos@example.com", "1004")))
                .andExpect(status().isConflict());
        assertThat(userCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"lastName\":\"P\",\"documentType\":\"CC\",\"documentNumber\":\"1\",\"email\":\"a@example.com\",\"phone\":\"300\",\"password\":\"password-segura\"}",
            "{\"firstName\":\"A\",\"lastName\":\"P\",\"documentType\":\"CC\",\"documentNumber\":\"1\",\"email\":\"no-es-email\",\"phone\":\"300\",\"password\":\"password-segura\"}",
            "{\"firstName\":\"A\",\"lastName\":\"P\",\"documentType\":\"CC\",\"documentNumber\":\"1\",\"email\":\"a@example.com\",\"phone\":\"300\",\"password\":\"corta\"}",
            "{\"firstName\":\"A\",\"lastName\":\"P\",\"documentType\":\"CC\",\"documentNumber\":\"1\",\"email\":\"a@example.com\",\"phone\":\" \",\"password\":\"password-segura\"}",
            "{\"firstName\":\"A\",\"lastName\":\"P\",\"documentType\":\"CC\",\"documentNumber\":\"1\",\"email\":\"a@example.com\",\"phone\":\"300\",\"password\":\"ññññññññññññññññññññññññññññññññññññññññ\"}"
    })
    void ca03_rejectsMissingOrInvalidDataWithoutPersistingAPartialAccount(String body) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(userCount()).isZero();
    }

    private int userCount() {
        return db.queryForObject("select count(*) from users", Integer.class);
    }
}
