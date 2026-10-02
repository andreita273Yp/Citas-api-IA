package co.edu.fcv.citas.offer;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.ResultActions;

/** HU-015 Crear profesional · HU-016 Asignar especialidades · HU-017 Asignar sedes y estado. */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class ProfessionalAdministrationIntegrationTest {
    private static final String PROFESSIONALS = "/api/v1/admin/professionals";
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();
    private AuthClient.Session admin;
    private long cardiology;
    private long internal;

    static String professionalJson(String email, String code, String license, String document) {
        return """
                {"firstName":"Laura","lastName":"Sintética","documentType":"CC","documentNumber":"%s","email":"%s","phone":"3100000000",
                 "initialPassword":"%s","professionalCode":"%s","licenseNumber":"%s"}
                """.formatted(document, email, AuthClient.PASSWORD, code, license);
    }

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        admin = auth.admin(mvc, database, "admin@example.com", "6001");
        cardiology = db.queryForObject("select id from specialties where code='CARDIOLOGIA_ADULTO'", Long.class);
        internal = db.queryForObject("select id from specialties where code='MEDICINA_INTERNA'", Long.class);
    }

    // ---------------------------------------------------------------- HU-015

    @Test
    void hu015_ca01_adminCreatesAProfessionalIdentityThatCanSignInWithoutExposingThePassword() throws Exception {
        create("laura@example.com", "PROF-100", "RM-DEMO-100", "7001")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.professionalCode").value("PROF-100"))
                .andExpect(jsonPath("$.licenseNumber").value("RM-DEMO-100"))
                .andExpect(jsonPath("$.email").value("laura@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.specialties.length()").value(0))
                .andExpect(jsonPath("$.initialPassword").doesNotExist());
        assertThat(db.queryForList("select r.code from users u join user_roles ur on ur.user_id=u.id join roles r on r.id=ur.role_id where u.email='laura@example.com'", String.class))
                .containsExactly("PROFESSIONAL");
        assertThat(db.queryForObject("select password_hash from users where email='laura@example.com'", String.class)).startsWith("$2");

        AuthClient.Session professional = auth.login(mvc, "laura@example.com");
        mvc.perform(get(PROFESSIONALS).header("Authorization", admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.professionalCode == 'PROF-100')]").isNotEmpty());
        assertThat(professional.accessToken()).isNotBlank();
    }

    @Test
    void hu015_ca02_onlyAdminCanManageProfessionals() throws Exception {
        auth.register(mvc, "user@example.com", "7002");
        AuthClient.Session user = auth.login(mvc, "user@example.com");
        mvc.perform(post(PROFESSIONALS).header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(professionalJson("x@example.com", "PROF-X", "RM-X", "7003"))).andExpect(status().isForbidden());
        mvc.perform(get(PROFESSIONALS).header("Authorization", user.bearer())).andExpect(status().isForbidden());
        mvc.perform(post(PROFESSIONALS).contentType(MediaType.APPLICATION_JSON)
                .content(professionalJson("x@example.com", "PROF-X", "RM-X", "7003"))).andExpect(status().isUnauthorized());

        long id = createdId("pro@example.com", "PROF-101", "RM-101", "7004");
        AuthClient.Session professional = auth.login(mvc, "pro@example.com");
        mvc.perform(patch(PROFESSIONALS + "/" + id).header("Authorization", professional.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isForbidden());
        assertThat(db.queryForObject("select count(*) from professionals", Integer.class)).isEqualTo(1);
    }

    @Test
    void hu015_ca03_invalidOrDuplicateDataLeavesNoInconsistentRecords() throws Exception {
        createdId("uno@example.com", "PROF-200", "RM-200", "7010");
        create("UNO@example.com", "PROF-201", "RM-201", "7011").andExpect(status().isConflict());   // email
        create("dos@example.com", "PROF-200", "RM-202", "7012").andExpect(status().isConflict());   // código
        create("tres@example.com", "PROF-203", "RM-200", "7013").andExpect(status().isConflict());  // matrícula
        create("cuatro@example.com", "PROF-204", "RM-204", "6001").andExpect(status().isConflict()); // documento del admin
        create("cinco@example.com", "", "RM-205", "7015").andExpect(status().isBadRequest());
        create("seis@example.com", "PROF-206", " ", "7016").andExpect(status().isBadRequest());
        mvc.perform(post(PROFESSIONALS).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(professionalJson("siete@example.com", "PROF-207", "RM-207", "7017").replace(AuthClient.PASSWORD, "corta")))
                .andExpect(status().isBadRequest());

        assertThat(db.queryForObject("select count(*) from professionals", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from users", Integer.class)).isEqualTo(2); // admin + uno
    }

    // ---------------------------------------------------------------- HU-016

    @Test
    void hu016_ca01_ca02_professionalHoldsSeveralActiveSpecialtiesWithExactlyOnePrimary() throws Exception {
        long id = createdId("multi@example.com", "PROF-300", "RM-300", "7020");
        assignSpecialties(id, "[{\"specialtyId\":" + cardiology + ",\"primary\":true},{\"specialtyId\":" + internal + ",\"primary\":false}]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialties.length()").value(2))
                .andExpect(jsonPath("$.specialties[?(@.primary == true)].code").value("CARDIOLOGIA_ADULTO"));

        assignSpecialties(id, "[{\"specialtyId\":" + cardiology + ",\"primary\":false},{\"specialtyId\":" + internal + ",\"primary\":true}]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialties[?(@.primary == true)].code").value("MEDICINA_INTERNA"));

        assignSpecialties(id, "[{\"specialtyId\":" + cardiology + ",\"primary\":false}]").andExpect(status().isBadRequest());
        assignSpecialties(id, "[{\"specialtyId\":" + cardiology + ",\"primary\":true},{\"specialtyId\":" + internal + ",\"primary\":true}]")
                .andExpect(status().isBadRequest());
        assignSpecialties(id, "[{\"specialtyId\":" + cardiology + ",\"primary\":true},{\"specialtyId\":" + cardiology + ",\"primary\":false}]")
                .andExpect(status().isBadRequest());
        assignSpecialties(id, "[]").andExpect(status().isBadRequest());
        assignSpecialties(id, "[{\"specialtyId\":9999,\"primary\":true}]").andExpect(status().isBadRequest());
        assignSpecialties(9999, "[{\"specialtyId\":" + cardiology + ",\"primary\":true}]").andExpect(status().isNotFound());

        db.update("update specialties set active=false where id=?", internal);
        assignSpecialties(id, "[{\"specialtyId\":" + internal + ",\"primary\":true}]").andExpect(status().isBadRequest());
        assertThat(db.queryForObject("select count(*) from professional_specialties where professional_id=? and active=true and is_primary=true", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void hu016_ca03_unassignedSpecialtyIsNotOfferedNorBookable() throws Exception {
        long id = createdId("solo@example.com", "PROF-301", "RM-301", "7021");
        assignSpecialties(id, "[{\"specialtyId\":" + cardiology + ",\"primary\":true},{\"specialtyId\":" + internal + ",\"primary\":false}]").andExpect(status().isOk());
        assignLocations(id, "[1]").andExpect(status().isOk());
        offered(internal, 1).andExpect(jsonPath("$[?(@.id == " + id + ")]").isNotEmpty());

        assignSpecialties(id, "[{\"specialtyId\":" + cardiology + ",\"primary\":true}]").andExpect(status().isOk())
                .andExpect(jsonPath("$.specialties.length()").value(1));
        offered(internal, 1).andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());

        LocalDate date = LocalDate.now().plusDays(2);
        database.publishBlock(id, 1, date, LocalTime.of(8, 0), LocalTime.of(9, 0));
        book(id, 1, internal, date + "T08:00").andExpect(status().isConflict());
        book(id, 1, cardiology, date + "T08:00").andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- HU-017

    @Test
    void hu017_ca01_onlyTheFixedLocationsCanBeAssigned() throws Exception {
        long id = createdId("sedes@example.com", "PROF-400", "RM-400", "7030");
        assignLocations(id, "[1,2]").andExpect(status().isOk()).andExpect(jsonPath("$.locations.length()").value(2));
        assignLocations(id, "[2]").andExpect(status().isOk())
                .andExpect(jsonPath("$.locations.length()").value(1)).andExpect(jsonPath("$.locations[0].code").value("ICV"));
        assignLocations(id, "[3]").andExpect(status().isBadRequest());
        assignLocations(id, "[]").andExpect(status().isBadRequest());
        assignLocations(id, "[1,1]").andExpect(status().isBadRequest());
        assertThat(db.queryForList("select location_id from professional_locations where professional_id=? and active=true", Long.class, id)).containsExactly(2L);
    }

    @Test
    void hu017_ca02_deactivatedProfessionalIsNoLongerOfferedOrBookable() throws Exception {
        long id = createdId("estado@example.com", "PROF-401", "RM-401", "7031");
        assignSpecialties(id, "[{\"specialtyId\":" + cardiology + ",\"primary\":true}]").andExpect(status().isOk());
        assignLocations(id, "[1]").andExpect(status().isOk());
        LocalDate date = LocalDate.now().plusDays(2);
        database.publishBlock(id, 1, date, LocalTime.of(8, 0), LocalTime.of(9, 0));

        setActive(id, false).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        offered(cardiology, 1).andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
        book(id, 1, cardiology, date + "T08:00").andExpect(status().isConflict());

        setActive(id, true).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
        offered(cardiology, 1).andExpect(jsonPath("$[?(@.id == " + id + ")]").isNotEmpty());
        offered(cardiology, 2).andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions create(String email, String code, String license, String document) throws Exception {
        return mvc.perform(post(PROFESSIONALS).header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(professionalJson(email, code, license, document)));
    }

    private long createdId(String email, String code, String license, String document) throws Exception {
        String body = create(email, code, license, document).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private ResultActions assignSpecialties(long id, String assignments) throws Exception {
        return mvc.perform(put(PROFESSIONALS + "/" + id + "/specialties").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"assignments\":" + assignments + "}"));
    }

    private ResultActions assignLocations(long id, String locationIds) throws Exception {
        return mvc.perform(put(PROFESSIONALS + "/" + id + "/locations").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"locationIds\":" + locationIds + "}"));
    }

    private ResultActions setActive(long id, boolean active) throws Exception {
        return mvc.perform(patch(PROFESSIONALS + "/" + id).header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":" + active + "}"));
    }

    private ResultActions offered(long specialty, long location) throws Exception {
        return mvc.perform(get("/api/v1/catalogs/professionals?specialtyId=" + specialty + "&locationId=" + location)
                .header("Authorization", admin.bearer())).andExpect(status().isOk());
    }

    private ResultActions book(long professional, long location, long specialty, String startAt) throws Exception {
        return mvc.perform(post("/api/v1/appointments").header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"professionalId\":" + professional + ",\"locationId\":" + location + ",\"specialtyId\":" + specialty + ",\"startAt\":\"" + startAt + "\"}"));
    }
}
