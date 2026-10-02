package co.edu.fcv.citas.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.fcv.citas.CitasApiApplication;
import co.edu.fcv.citas.support.AuthClient;
import co.edu.fcv.citas.support.OfferFixtures;
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

/** HU-024 · Resolver solicitud especializada (RN-03, RN-04, RN-09, RN-11, RF-19). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class SpecializedDecisionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired OfferFixtures offer;
    @Autowired JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();

    private AuthClient.Session admin;
    private AuthClient.Session patient;
    private AuthClient.Session professional;
    private long specialist;
    private long neurology;
    private final LocalDate day = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        admin = auth.admin(mvc, database, "admin@example.com", "9101");
        neurology = offer.specialtyId("NEUROLOGIA");
        specialist = offer.professional(mvc, admin, "neuro@example.com", "PROF-N", new long[] {neurology}, 1);
        professional = auth.login(mvc, "neuro@example.com");
        database.publishBlock(specialist, 1, day, LocalTime.of(8, 0), LocalTime.of(10, 0));
        auth.register(mvc, "paciente@example.com", "9102");
        patient = auth.login(mvc, "paciente@example.com");
    }

    @Test
    void ca01_approvingKeepsTheReservationAndIsAuditedAsAdmin() throws Exception {
        long id = request("08:00");
        decide(admin, id, "{\"decision\":\"APPROVE\"}").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(db.queryForObject("select count(*) from professional_slots where appointment_id=?", Integer.class, id)).isEqualTo(2);
        assertThat(db.queryForObject("select approved_by_user_id is not null and approved_at is not null from appointments where id=?", Boolean.class, id)).isTrue();
        mvc.perform(get("/api/v1/appointments/" + id + "/history").header("Authorization", patient.bearer()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].previousStatus").value("REQUESTED")).andExpect(jsonPath("$[1].newStatus").value("APPROVED"))
                .andExpect(jsonPath("$[1].source").value("ADMIN"));
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", admin.bearer())).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ca02_rejectingWithReasonRecordsItAndReleasesTheSlots() throws Exception {
        long id = request("08:00");
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$[0].kind").value("SPECIALIZED_REQUEST")).andExpect(jsonPath("$[0].appointmentId").value(Long.toString(id)));
        decide(admin, id, "{\"decision\":\"REJECT\",\"reason\":\"Sin orden médica de remisión\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        assertThat(db.queryForObject("select count(*) from professional_slots where appointment_id=?", Integer.class, id)).isZero();
        mvc.perform(get("/api/v1/appointments/" + id).header("Authorization", patient.bearer()))
                .andExpect(jsonPath("$.status").value("REJECTED")).andExpect(jsonPath("$.decisionReason").value("Sin orden médica de remisión"));
        mvc.perform(get("/api/v1/appointments/" + id + "/history").header("Authorization", patient.bearer()))
                .andExpect(jsonPath("$[1].newStatus").value("REJECTED")).andExpect(jsonPath("$[1].reason").value("Sin orden médica de remisión"));
        // Los slots liberados vuelven a ser reservables.
        mvc.perform(get("/api/v1/availability?specialtyId=" + neurology + "&date=" + day).header("Authorization", patient.bearer()))
                .andExpect(jsonPath("$[?(@.startAt == '" + day + "T08:00:00')]").isNotEmpty());
        request("08:00");
    }

    @Test
    void ca03_rejectionWithoutReasonIsRefusedWithoutTransition() throws Exception {
        long id = request("08:00");
        decide(admin, id, "{\"decision\":\"REJECT\"}").andExpect(status().isBadRequest());
        decide(admin, id, "{\"decision\":\"REJECT\",\"reason\":\"   \"}").andExpect(status().isBadRequest());
        decide(admin, id, "{\"decision\":\"MAYBE\"}").andExpect(status().isBadRequest());
        assertStatus(id, "REQUESTED");
        assertThat(db.queryForObject("select count(*) from professional_slots where appointment_id=?", Integer.class, id)).isEqualTo(2);
        assertThat(db.queryForObject("select count(*) from appointment_status_history where appointment_id=?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void ca03_onlyRequestedAppointmentsCanBeDecided() throws Exception {
        long id = request("08:00");
        decide(admin, id, "{\"decision\":\"APPROVE\"}").andExpect(status().isOk());
        decide(admin, id, "{\"decision\":\"REJECT\",\"reason\":\"tarde\"}").andExpect(status().isConflict());
        decide(admin, 999999, "{\"decision\":\"APPROVE\"}").andExpect(status().isNotFound());
        assertStatus(id, "APPROVED");
    }

    @Test
    void ca03_nonAdminActorsCannotDecide() throws Exception {
        long id = request("08:00");
        decide(patient, id, "{\"decision\":\"APPROVE\"}").andExpect(status().isForbidden());
        decide(professional, id, "{\"decision\":\"APPROVE\"}").andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/appointments/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVE\"}")).andExpect(status().isUnauthorized());
        assertStatus(id, "REQUESTED");
    }

    private long request(String time) throws Exception {
        String body = mvc.perform(post("/api/v1/appointments").header("Authorization", patient.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":" + specialist + ",\"locationId\":1,\"specialtyId\":" + neurology + ",\"startAt\":\"" + day + "T" + time + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REQUESTED")).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private ResultActions decide(AuthClient.Session who, long id, String body) throws Exception {
        return mvc.perform(post("/api/v1/admin/appointments/" + id + "/decision").header("Authorization", who.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertStatus(long id, String expected) {
        assertThat(db.queryForObject("select s.code from appointments a join appointment_statuses s on s.id=a.status_id where a.id=?", String.class, id))
                .isEqualTo(expected);
    }
}
