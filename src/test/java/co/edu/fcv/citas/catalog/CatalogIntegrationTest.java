package co.edu.fcv.citas.catalog;

import co.edu.fcv.citas.CitasApiApplication;
import co.edu.fcv.citas.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class CatalogIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    TestDatabase database;

    @BeforeEach
    void clean() {
        database.reset();
    }

    @Test
    void publishesFixedCatalogsToAuthenticatedConsumersAsReadOnly() throws Exception {
        String token = registerAndLogin();

        mvc.perform(get("/api/v1/catalogs/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'USER')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'PROFESSIONAL')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'ADMIN')]").exists());
        mvc.perform(get("/api/v1/catalogs/locations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'HIC')].address").value("Km 7 Autopista Bucaramanga - Piedecuesta, Valle de Menzulí"))
                .andExpect(jsonPath("$[?(@.code == 'HIC')].city").value("Piedecuesta"))
                .andExpect(jsonPath("$[?(@.code == 'ICV')].address").value("Calle 155A No. 23-58, Urbanización El Bosque"))
                .andExpect(jsonPath("$[?(@.code == 'ICV')].city").value("Floridablanca"));
        mvc.perform(get("/api/v1/catalogs/appointment-statuses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'APPROVED')]").exists());
        mvc.perform(get("/api/v1/catalogs/reschedule-statuses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'PENDING')]").exists());
        mvc.perform(get("/api/v1/catalogs/regimes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'CONTRIBUTIVO')]").exists());
        mvc.perform(post("/api/v1/catalogs/locations").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void rejectsUnauthenticatedCatalogAccess() throws Exception {
        mvc.perform(get("/api/v1/catalogs/locations")).andExpect(status().isUnauthorized());
    }

    private String registerAndLogin() throws Exception {
        String registration = """
                {"firstName":"Catalog","lastName":"Reader","documentType":"CC","documentNumber":"987654321","email":"catalog.reader@example.com","phone":"3001234567","password":"password-segura"}
                """;
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated());
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"catalog.reader@example.com\",\"password\":\"password-segura\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().replaceAll(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
