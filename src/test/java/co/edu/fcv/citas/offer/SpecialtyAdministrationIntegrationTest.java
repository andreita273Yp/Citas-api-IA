package co.edu.fcv.citas.offer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.fcv.citas.CitasApiApplication;
import co.edu.fcv.citas.support.AuthClient;
import co.edu.fcv.citas.support.TestDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** HU-014 · Gestionar especialidades y duración (CA-01 a CA-03). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class SpecialtyAdministrationIntegrationTest {
    private static final String SPECIALTIES = "/api/v1/admin/specialties";
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();
    private AuthClient.Session admin;

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        admin = auth.admin(mvc, database, "admin@example.com", "5001");
    }

    @Test
    void ca01_adminCreatesSpecializedSpecialtiesOnlyWith30Or60Minutes() throws Exception {
        mvc.perform(post(SPECIALTIES).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\" dermatologia \",\"name\":\"Dermatología\",\"durationMinutes\":60}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DERMATOLOGIA"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.general").value(false))
                .andExpect(jsonPath("$.requiresAdminApproval").value(true))
                .andExpect(jsonPath("$.active").value(true));
        for (int invalid : new int[] {0, 15, 45, 90}) {
            mvc.perform(post(SPECIALTIES).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"X" + invalid + "\",\"name\":\"Inválida " + invalid + "\",\"durationMinutes\":" + invalid + "}"))
                    .andExpect(status().isBadRequest());
        }
        long id = specialtyId("DERMATOLOGIA");
        mvc.perform(patch(SPECIALTIES + "/" + id).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":45}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(SPECIALTIES + "/" + id).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":30,\"name\":\"Dermatología Clínica\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.durationMinutes").value(30)).andExpect(jsonPath("$.name").value("Dermatología Clínica"));
    }

    @Test
    void ca01_rejectsDuplicatesAndNonAdminActors() throws Exception {
        mvc.perform(post(SPECIALTIES).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"MEDICINA_GENERAL\",\"name\":\"Otra\",\"durationMinutes\":30}"))
                .andExpect(status().isConflict());
        mvc.perform(post(SPECIALTIES).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"OTRA\",\"name\":\"medicina general\",\"durationMinutes\":30}"))
                .andExpect(status().isConflict());

        auth.register(mvc, "user@example.com", "5002");
        AuthClient.Session user = auth.login(mvc, "user@example.com");
        mvc.perform(get(SPECIALTIES).header("Authorization", user.bearer())).andExpect(status().isForbidden());
        mvc.perform(post(SPECIALTIES).header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NUEVA\",\"name\":\"Nueva\",\"durationMinutes\":30}"))
                .andExpect(status().isForbidden());
        assertThat(db.queryForObject("select count(*) from specialties", Integer.class)).isEqualTo(12);
    }

    @Test
    void ca02_availabilityTakesTheDurationFromTheSpecialtyCatalog() throws Exception {
        long specialty = specialtyId("NEUROLOGIA");
        long professional = createProfessional("neuro@example.com", "PROF-T1", specialty);
        LocalDate date = LocalDate.now().plusDays(3);
        database.publishBlock(professional, 1, date, LocalTime.of(8, 0), LocalTime.of(10, 0));

        String path = "/api/v1/availability?professionalId=" + professional + "&locationId=1&specialtyId=" + specialty + "&date=" + date;
        mvc.perform(get(path).header("Authorization", admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3)).andExpect(jsonPath("$[0].durationMinutes").value(60));

        mvc.perform(patch(SPECIALTIES + "/" + specialty).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationMinutes\":30}")).andExpect(status().isOk());
        mvc.perform(get(path).header("Authorization", admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4)).andExpect(jsonPath("$[0].durationMinutes").value(30));
    }

    @Test
    void ca03_retiringASpecialtyDeactivatesItWithoutDeletingAndBlocksNewBookings() throws Exception {
        long specialty = specialtyId("NEUROLOGIA");
        long professional = createProfessional("neuro2@example.com", "PROF-T2", specialty);
        LocalDate date = LocalDate.now().plusDays(3);
        database.publishBlock(professional, 1, date, LocalTime.of(8, 0), LocalTime.of(10, 0));

        mvc.perform(delete(SPECIALTIES + "/" + specialty).header("Authorization", admin.bearer())).andExpect(status().isMethodNotAllowed());
        mvc.perform(patch(SPECIALTIES + "/" + specialty).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        assertThat(db.queryForObject("select count(*) from specialties where id=?", Integer.class, specialty)).isEqualTo(1);

        mvc.perform(get("/api/v1/catalogs/specialties").header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$[?(@.id == " + specialty + ")]").isEmpty());
        mvc.perform(get("/api/v1/catalogs/professionals?specialtyId=" + specialty + "&locationId=1").header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/v1/appointments").header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":" + professional + ",\"locationId\":1,\"specialtyId\":" + specialty + ",\"startAt\":\"" + date + "T08:00\"}"))
                .andExpect(status().is4xxClientError());
        assertThat(db.queryForObject("select count(*) from appointments", Integer.class)).isZero();
    }

    private long specialtyId(String code) {
        return db.queryForObject("select id from specialties where code=?", Long.class, code);
    }

    private long createProfessional(String email, String code, long specialty) throws Exception {
        String created = mvc.perform(post("/api/v1/admin/professionals").header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(ProfessionalAdministrationIntegrationTest.professionalJson(email, code, "RM-" + code, "doc-" + code)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        mvc.perform(put("/api/v1/admin/professionals/" + id + "/specialties").header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"assignments\":[{\"specialtyId\":" + specialty + ",\"primary\":true}]}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/admin/professionals/" + id + "/locations").header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"locationIds\":[1]}")).andExpect(status().isOk());
        return id;
    }
}
