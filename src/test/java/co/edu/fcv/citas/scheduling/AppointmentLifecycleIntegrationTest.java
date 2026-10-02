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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-025 Mis citas · HU-026 Cancelar · HU-027 Solicitar reprogramación · HU-028 Resolver reprogramación (RF-13 a RF-15). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class AppointmentLifecycleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired OfferFixtures offer;
    @Autowired JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();

    private AuthClient.Session admin;
    private AuthClient.Session ana;
    private AuthClient.Session beto;
    private long general;
    private long neurology;
    private long doctor;
    private final LocalDate day = LocalDate.now().plusDays(4);

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        admin = auth.admin(mvc, database, "admin@example.com", "9501");
        general = offer.specialtyId("MEDICINA_GENERAL");
        neurology = offer.specialtyId("NEUROLOGIA");
        doctor = offer.professional(mvc, admin, "doc@example.com", "PROF-L", new long[] {general, neurology}, 1, 2);
        database.publishBlock(doctor, 1, day, LocalTime.of(8, 0), LocalTime.of(12, 0));
        database.publishBlock(doctor, 2, day.plusDays(1), LocalTime.of(8, 0), LocalTime.of(10, 0));
        auth.register(mvc, "ana@example.com", "9502");
        auth.register(mvc, "beto@example.com", "9503");
        ana = auth.login(mvc, "ana@example.com");
        beto = auth.login(mvc, "beto@example.com");
    }

    // ---------------------------------------------------------------- HU-025

    @Test
    void hu025_ca01_ca02_userListsOwnAppointmentsWithMinimumDataFiltersAndRejectionReason() throws Exception {
        long approved = book(ana, general, 1, day + "T08:00");
        long rejected = book(ana, neurology, 1, day + "T10:00");
        book(beto, general, 1, day + "T09:00");
        mvc.perform(post("/api/v1/admin/appointments/" + rejected + "/decision").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECT\",\"reason\":\"Falta remisión\"}")).andExpect(status().isOk());

        mvc.perform(get("/api/v1/appointments").header("Authorization", ana.bearer())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == " + approved + ")].locationCode").value("HIC"))
                .andExpect(jsonPath("$[?(@.id == " + approved + ")].professional").value("Pro PROF-L"))
                .andExpect(jsonPath("$[?(@.id == " + approved + ")].specialty").value("Medicina General"))
                .andExpect(jsonPath("$[?(@.id == " + approved + ")].durationMinutes").value(30))
                .andExpect(jsonPath("$[?(@.id == " + approved + ")].startsAt").value(day + "T08:00:00"))
                .andExpect(jsonPath("$[?(@.id == " + approved + ")].status").value("APPROVED"))
                .andExpect(jsonPath("$[?(@.id == " + approved + ")].decisionReason").value((Object) null))
                .andExpect(jsonPath("$[?(@.id == " + rejected + ")].decisionReason").value("Falta remisión"));

        mvc.perform(get("/api/v1/appointments?status=REJECTED").header("Authorization", ana.bearer()))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(rejected));
        mvc.perform(get("/api/v1/appointments?from=" + day.plusDays(1)).header("Authorization", ana.bearer())).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/appointments?from=" + day + "&to=" + day).header("Authorization", ana.bearer())).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/appointments?from=" + day.plusDays(1) + "&to=" + day).header("Authorization", ana.bearer())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/appointments?status=ALGO").header("Authorization", ana.bearer())).andExpect(status().isBadRequest());
    }

    @Test
    void hu025_ca03_detailOfAnotherUsersAppointmentIsNotDisclosed() throws Exception {
        long own = book(ana, general, 1, day + "T08:00");
        mvc.perform(get("/api/v1/appointments/" + own).header("Authorization", ana.bearer())).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(own));
        mvc.perform(get("/api/v1/appointments/" + own).header("Authorization", beto.bearer())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/appointments").header("Authorization", beto.bearer())).andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------- HU-026

    @Test
    void hu026_ca01_cancellingAFutureOwnAppointmentReleasesItsSlotsAndIsAudited() throws Exception {
        long id = book(ana, neurology, 1, day + "T08:00");
        cancel(ana, id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(heldSlots(id)).isZero();
        book(beto, neurology, 1, day + "T08:00");
        mvc.perform(get("/api/v1/appointments/" + id + "/history").header("Authorization", ana.bearer()))
                .andExpect(jsonPath("$[1].previousStatus").value("REQUESTED")).andExpect(jsonPath("$[1].newStatus").value("CANCELLED"))
                .andExpect(jsonPath("$[1].source").value("USER"));
    }

    @Test
    void hu026_ca02_ca03_foreignPastOrTerminalAppointmentsCannotBeCancelledOrReactivated() throws Exception {
        long id = book(ana, general, 1, day + "T08:00");
        cancel(beto, id).andExpect(status().isNotFound());
        assertStatus(id, "APPROVED");

        cancel(ana, id).andExpect(status().isOk());
        cancel(ana, id).andExpect(status().isConflict());
        reschedule(ana, id, day + "T09:00", null).andExpect(status().isConflict());
        assertStatus(id, "CANCELLED");
        assertThat(db.queryForObject("select count(*) from appointment_status_history h join appointment_statuses s on s.id=h.status_id where h.appointment_id=? and s.code='CANCELLED'",
                Integer.class, id)).isEqualTo(1);

        long past = book(ana, general, 1, day + "T09:00");
        db.update("update appointments set scheduled_start_at=now() - interval 2 hour, scheduled_end_at=now() - interval 90 minute where id=?", past);
        cancel(ana, past).andExpect(status().isConflict());
        assertStatus(past, "APPROVED");
    }

    // ---------------------------------------------------------------- HU-027

    @Test
    void hu027_ca01_ca02_requestHoldsTheNewSlotsAndPreservesTheOriginalAppointment() throws Exception {
        long id = book(ana, neurology, 1, day + "T08:00");
        approve(id);
        String body = reschedule(ana, id, day.plusDays(1) + "T08:00", 2L).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.locationCode").value("ICV"))
                .andExpect(jsonPath("$.startAt").value(day.plusDays(1) + "T08:00:00")).andExpect(jsonPath("$.endAt").value(day.plusDays(1) + "T09:00:00"))
                .andReturn().getResponse().getContentAsString();
        long request = json.readTree(body).get("id").asLong();

        assertThat(heldSlots(id)).isEqualTo(4);
        mvc.perform(get("/api/v1/appointments/" + id).header("Authorization", ana.bearer()))
                .andExpect(jsonPath("$.startsAt").value(day + "T08:00:00")).andExpect(jsonPath("$.locationCode").value("HIC"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reschedule.id").value(request)).andExpect(jsonPath("$.reschedule.status").value("PENDING"));
        mvc.perform(get("/api/v1/availability?specialtyId=" + neurology + "&date=" + day.plusDays(1)).header("Authorization", beto.bearer()))
                .andExpect(jsonPath("$[?(@.startAt =~ /.*T08:.*/)]").isEmpty());
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$[?(@.kind == 'RESCHEDULE')].requestId").value(Long.toString(request)));
    }

    @Test
    void hu027_ca03_ineligibleAppointmentsOrUnavailableSlotsAreRejectedWithoutSideEffects() throws Exception {
        long requested = book(ana, neurology, 1, day + "T08:00");
        reschedule(ana, requested, day + "T10:00", null).andExpect(status().isConflict());         // no APPROVED

        long id = book(ana, general, 1, day + "T09:00");
        long taken = book(beto, general, 1, day + "T11:00");
        reschedule(beto, id, day + "T10:00", null).andExpect(status().isNotFound());            // ajena
        reschedule(ana, id, LocalDate.now().minusDays(1) + "T10:00", null).andExpect(status().isBadRequest());
        reschedule(ana, id, day + "T10:15", null).andExpect(status().isBadRequest());
        reschedule(ana, id, day + "T11:00", null).andExpect(status().isConflict());              // ocupada
        reschedule(ana, id, day.plusDays(9) + "T08:00", null).andExpect(status().isConflict());  // sin agenda
        db.update("update professional_locations set active=false where professional_id=? and location_id=2", doctor);
        reschedule(ana, id, day.plusDays(1) + "T08:00", 2L).andExpect(status().isConflict());    // sede no asignada

        reschedule(ana, id, day + "T10:00", null).andExpect(status().isCreated());
        reschedule(ana, id, day + "T10:30", null).andExpect(status().isConflict());              // ya hay una pendiente
        assertThat(db.queryForObject("select count(*) from reschedule_requests", Integer.class)).isEqualTo(1);
        assertThat(heldSlots(taken)).isEqualTo(1);
        mvc.perform(get("/api/v1/appointments/" + id).header("Authorization", ana.bearer())).andExpect(jsonPath("$.startsAt").value(day + "T09:00:00"));
    }

    // ---------------------------------------------------------------- HU-028

    @Test
    void hu028_ca01_approvalMovesTheAppointmentAndReleasesTheOldSlots() throws Exception {
        long id = book(ana, general, 1, day + "T08:00");
        long request = requestReschedule(ana, id, day + "T11:30");
        decide(admin, request, "{\"decision\":\"APPROVE\"}").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));

        assertThat(heldStarts(id)).containsExactly(day + " 11:30");
        mvc.perform(get("/api/v1/appointments/" + id).header("Authorization", ana.bearer()))
                .andExpect(jsonPath("$.startsAt").value(day + "T11:30:00")).andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reschedule.status").value("APPROVED"));
        book(beto, general, 1, day + "T08:00");
        assertThat(db.queryForObject("select decided_by_user_id is not null and decided_at is not null from reschedule_requests where id=?", Boolean.class, request)).isTrue();
    }

    @Test
    void hu028_ca02_rejectionWithReasonReleasesTheNewSlotsAndKeepsTheOriginal() throws Exception {
        long id = book(ana, general, 1, day + "T08:00");
        long request = requestReschedule(ana, id, day + "T11:30");
        decide(admin, request, "{\"decision\":\"REJECT\"}").andExpect(status().isBadRequest());
        decide(admin, request, "{\"decision\":\"REJECT\",\"reason\":\"Agenda llena\"}").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(heldStarts(id)).containsExactly(day + " 08:00");
        mvc.perform(get("/api/v1/appointments/" + id).header("Authorization", ana.bearer()))
                .andExpect(jsonPath("$.startsAt").value(day + "T08:00:00")).andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reschedule.status").value("REJECTED")).andExpect(jsonPath("$.reschedule.decisionReason").value("Agenda llena"));
        book(beto, general, 1, day + "T11:30");
    }

    @Test
    void hu028_afterRejectionTheUserKeepsOrCancelsTheAppointment() throws Exception {
        long kept = book(ana, general, 1, day + "T08:00");
        long r1 = requestReschedule(ana, kept, day + "T11:00");
        decide(admin, r1, "{\"decision\":\"REJECT\",\"reason\":\"No\"}").andExpect(status().isOk());
        mvc.perform(post("/api/v1/appointments/" + kept + "/reschedule-requests/" + r1 + "/keep").header("Authorization", ana.bearer()))
                .andExpect(status().isNoContent());
        assertThat(db.queryForObject("select patient_action_after_rejection from reschedule_requests where id=?", String.class, r1)).isEqualTo("KEEP_APPOINTMENT");
        mvc.perform(post("/api/v1/appointments/" + kept + "/reschedule-requests/" + r1 + "/keep").header("Authorization", ana.bearer()))
                .andExpect(status().isConflict());

        long dropped = book(ana, general, 1, day + "T09:00");
        long r2 = requestReschedule(ana, dropped, day + "T11:30");
        decide(admin, r2, "{\"decision\":\"REJECT\",\"reason\":\"No\"}").andExpect(status().isOk());
        cancel(ana, dropped).andExpect(status().isOk());
        assertThat(db.queryForObject("select patient_action_after_rejection from reschedule_requests where id=?", String.class, r2)).isEqualTo("CANCEL_APPOINTMENT");
    }

    @Test
    void hu028_ca03_onlyAdminDecidesAndOnlyPendingRequests() throws Exception {
        long id = book(ana, general, 1, day + "T08:00");
        long request = requestReschedule(ana, id, day + "T11:30");
        decide(ana, request, "{\"decision\":\"APPROVE\"}").andExpect(status().isForbidden());
        AuthClient.Session professional = auth.login(mvc, "doc@example.com");
        decide(professional, request, "{\"decision\":\"APPROVE\"}").andExpect(status().isForbidden());
        assertThat(heldSlots(id)).isEqualTo(2);

        decide(admin, request, "{\"decision\":\"APPROVE\"}").andExpect(status().isOk());
        decide(admin, request, "{\"decision\":\"REJECT\",\"reason\":\"tarde\"}").andExpect(status().isConflict());
        decide(admin, 999999, "{\"decision\":\"APPROVE\"}").andExpect(status().isNotFound());
        assertThat(heldStarts(id)).containsExactly(day + " 11:30");
    }

    @Test
    void cancellingWithAPendingRescheduleReleasesBothRangesAndCancelsTheRequest() throws Exception {
        long id = book(ana, general, 1, day + "T08:00");
        long request = requestReschedule(ana, id, day + "T11:30");
        cancel(ana, id).andExpect(status().isOk());
        assertThat(heldSlots(id)).isZero();
        assertThat(db.queryForObject("select s.code from reschedule_requests r join reschedule_request_statuses s on s.id=r.status_id where r.id=?", String.class, request))
                .isEqualTo("CANCELLED");
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", admin.bearer())).andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------- helpers

    private long book(AuthClient.Session who, long specialty, long location, String startAt) throws Exception {
        String body = mvc.perform(post("/api/v1/appointments").header("Authorization", who.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":" + doctor + ",\"locationId\":" + location + ",\"specialtyId\":" + specialty + ",\"startAt\":\"" + startAt + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private void approve(long appointmentId) throws Exception {
        mvc.perform(post("/api/v1/admin/appointments/" + appointmentId + "/decision").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\"}")).andExpect(status().isOk());
    }

    private ResultActions cancel(AuthClient.Session who, long id) throws Exception {
        return mvc.perform(post("/api/v1/appointments/" + id + "/cancel").header("Authorization", who.bearer()));
    }

    private ResultActions reschedule(AuthClient.Session who, long id, String startAt, Long locationId) throws Exception {
        String location = locationId == null ? "" : ",\"locationId\":" + locationId;
        return mvc.perform(post("/api/v1/appointments/" + id + "/reschedule-requests").header("Authorization", who.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"startAt\":\"" + startAt + "\"" + location + "}"));
    }

    private long requestReschedule(AuthClient.Session who, long id, String startAt) throws Exception {
        return json.readTree(reschedule(who, id, startAt, null).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions decide(AuthClient.Session who, long requestId, String body) throws Exception {
        return mvc.perform(post("/api/v1/admin/reschedule-requests/" + requestId + "/decision").header("Authorization", who.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int heldSlots(long appointmentId) {
        return db.queryForObject("select count(*) from professional_slots where appointment_id=?", Integer.class, appointmentId);
    }

    private List<String> heldStarts(long appointmentId) {
        return db.queryForList("select date_format(start_at,'%Y-%m-%d %H:%i') from professional_slots where appointment_id=? order by start_at", String.class, appointmentId);
    }

    private void assertStatus(long id, String expected) {
        assertThat(db.queryForObject("select s.code from appointments a join appointment_statuses s on s.id=a.status_id where a.id=?", String.class, id))
                .isEqualTo(expected);
    }
}
