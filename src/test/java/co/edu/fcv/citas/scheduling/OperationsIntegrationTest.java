package co.edu.fcv.citas.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.fcv.citas.CitasApiApplication;
import co.edu.fcv.citas.support.AuthClient;
import co.edu.fcv.citas.support.OfferFixtures;
import co.edu.fcv.citas.support.TestDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-029 Agenda profesional · HU-030 Cierre de atención · HU-031 Bandeja ADMIN · HU-032 Auditoría de estados. */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class OperationsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired OfferFixtures offer;
    @Autowired JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();

    private AuthClient.Session admin;
    private AuthClient.Session ana;
    private AuthClient.Session beto;
    private AuthClient.Session doctorSession;
    private AuthClient.Session otherDoctorSession;
    private long general;
    private long neurology;
    private long doctor;
    private long otherDoctor;
    /** Martes de la próxima semana: deja días de la misma semana antes y después. */
    private final LocalDate tuesday = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(1);

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        admin = auth.admin(mvc, database, "admin@example.com", "9601");
        general = offer.specialtyId("MEDICINA_GENERAL");
        neurology = offer.specialtyId("NEUROLOGIA");
        doctor = offer.professional(mvc, admin, "doc@example.com", "PROF-O", new long[] {general, neurology}, 1, 2);
        otherDoctor = offer.professional(mvc, admin, "otro@example.com", "PROF-P", new long[] {general}, 1);
        for (LocalDate d : new LocalDate[] {tuesday, tuesday.plusDays(1), tuesday.plusDays(7)}) {
            database.publishBlock(doctor, 1, d, LocalTime.of(8, 0), LocalTime.of(12, 0));
            database.publishBlock(otherDoctor, 1, d, LocalTime.of(8, 0), LocalTime.of(10, 0));
        }
        database.publishBlock(doctor, 2, tuesday.plusDays(2), LocalTime.of(8, 0), LocalTime.of(10, 0));
        auth.register(mvc, "ana@example.com", "9602");
        auth.register(mvc, "beto@example.com", "9603");
        ana = auth.login(mvc, "ana@example.com");
        beto = auth.login(mvc, "beto@example.com");
        doctorSession = auth.login(mvc, "doc@example.com");
        otherDoctorSession = auth.login(mvc, "otro@example.com");
    }

    // ---------------------------------------------------------------- HU-029

    @Test
    void hu029_ca01_ca03_professionalSeesOnlyOwnApprovedAppointmentsWithMinimalPatientData() throws Exception {
        long approved = book(ana, doctor, 1, general, tuesday + "T08:00");
        book(ana, doctor, 1, neurology, tuesday + "T09:00");                  // REQUESTED: no aparece
        long cancelled = book(beto, doctor, 1, general, tuesday + "T11:00");
        mvc.perform(post("/api/v1/appointments/" + cancelled + "/cancel").header("Authorization", beto.bearer())).andExpect(status().isOk());
        book(beto, otherDoctor, 1, general, tuesday + "T08:00");              // de otro profesional

        agenda(doctorSession, "from=" + tuesday + "&to=" + tuesday).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(approved))
                .andExpect(jsonPath("$[0].patient").value("Ana Prueba"))
                .andExpect(jsonPath("$[0].specialty").value("Medicina General"))
                .andExpect(jsonPath("$[0].locationCode").value("HIC"))
                .andExpect(jsonPath("$[0].durationMinutes").value(30))
                .andExpect(jsonPath("$[0].patientEmail").doesNotExist())
                .andExpect(jsonPath("$[0].documentNumber").doesNotExist());
        agenda(otherDoctorSession, "from=" + tuesday + "&to=" + tuesday).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[?(@.id == " + approved + ")]").isEmpty());
        agenda(ana, "").andExpect(status().isForbidden());
        agenda(admin, "").andExpect(status().isForbidden());
    }

    @Test
    void hu029_ca02_dayWeekAndLocationFilters() throws Exception {
        book(ana, doctor, 1, general, tuesday + "T08:00");
        book(ana, doctor, 1, general, tuesday.plusDays(1) + "T08:00");
        book(ana, doctor, 2, general, tuesday.plusDays(2) + "T08:00");
        book(ana, doctor, 1, general, tuesday.plusDays(7) + "T08:00");

        agenda(doctorSession, "date=" + tuesday + "&view=DAY").andExpect(jsonPath("$.length()").value(1));
        agenda(doctorSession, "date=" + tuesday + "&view=WEEK").andExpect(jsonPath("$.length()").value(3));
        agenda(doctorSession, "date=" + tuesday.plusDays(4) + "&view=WEEK&locationId=2").andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].locationCode").value("ICV"));
        agenda(doctorSession, "date=" + tuesday + "&view=MONTH").andExpect(status().isBadRequest());
        agenda(doctorSession, "from=" + tuesday.plusDays(3) + "&to=" + tuesday).andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- HU-030

    @Test
    void hu030_ca01_ca03_professionalClosesPastOwnAppointmentsAndTheClosureIsAudited() throws Exception {
        long completed = pastApproved(ana, tuesday + "T08:00");
        long noShow = pastApproved(beto, tuesday + "T08:30");

        close(doctorSession, completed, "COMPLETED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        close(doctorSession, noShow, "NO_SHOW").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NO_SHOW"));
        long doctorUser = db.queryForObject("select user_id from professionals where id=?", Long.class, doctor);
        mvc.perform(get("/api/v1/appointments/" + completed + "/history").header("Authorization", doctorSession.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].previousStatus").value("APPROVED")).andExpect(jsonPath("$[1].newStatus").value("COMPLETED"))
                .andExpect(jsonPath("$[1].actorId").value(doctorUser)).andExpect(jsonPath("$[1].source").value("USER"))
                .andExpect(jsonPath("$[1].occurredAt").exists());
    }

    @Test
    void hu030_ca02_foreignFutureOrNonApplicableAppointmentsCannotBeClosed() throws Exception {
        long future = book(ana, doctor, 1, general, tuesday + "T09:00");
        close(doctorSession, future, "COMPLETED").andExpect(status().isConflict());

        long past = pastApproved(ana, tuesday + "T08:00");
        close(otherDoctorSession, past, "COMPLETED").andExpect(status().isNotFound());
        close(doctorSession, past, "CANCELLED").andExpect(status().isBadRequest());
        close(ana, past, "COMPLETED").andExpect(status().isForbidden());
        close(doctorSession, past, "COMPLETED").andExpect(status().isOk());
        close(doctorSession, past, "NO_SHOW").andExpect(status().isConflict());

        long requested = book(beto, doctor, 1, neurology, tuesday + "T10:00");
        db.update("update appointments set scheduled_start_at=now() - interval 3 hour, scheduled_end_at=now() - interval 2 hour where id=?", requested);
        close(doctorSession, requested, "COMPLETED").andExpect(status().isConflict());
        assertStatus(requested, "REQUESTED");
    }

    // ---------------------------------------------------------------- HU-031

    @Test
    void hu031_ca01_ca02_inboxListsPendingSpecializedRequestsAndReschedulesWithFilters() throws Exception {
        long specialized = book(ana, doctor, 1, neurology, tuesday + "T08:00");
        book(beto, doctor, 1, general, tuesday + "T10:00");                 // APPROVED general: no aparece
        long toMove = book(beto, otherDoctor, 1, general, tuesday + "T08:00");
        String body = mvc.perform(post("/api/v1/appointments/" + toMove + "/reschedule-requests").header("Authorization", beto.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"startAt\":\"" + tuesday.plusDays(1) + "T09:00\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long reschedule = json.readTree(body).get("id").asLong();

        inbox("").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.kind == 'SPECIALIZED_REQUEST')].appointmentId").value((int) specialized))
                .andExpect(jsonPath("$[?(@.kind == 'RESCHEDULE')].requestId").value((int) reschedule))
                .andExpect(jsonPath("$[?(@.kind == 'RESCHEDULE')].currentStartsAt").value(tuesday + "T08:00:00"))
                .andExpect(jsonPath("$[?(@.kind == 'RESCHEDULE')].startsAt").value(tuesday.plusDays(1) + "T09:00:00"));
        inbox("professionalId=" + doctor).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].kind").value("SPECIALIZED_REQUEST"));
        inbox("specialtyId=" + general).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].kind").value("RESCHEDULE"));
        inbox("locationId=2").andExpect(jsonPath("$.length()").value(0));
        inbox("date=" + tuesday.plusDays(1)).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].kind").value("RESCHEDULE"));
        inbox("date=" + tuesday).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].kind").value("SPECIALIZED_REQUEST"));
    }

    @Test
    void hu031_ca03_onlyAdminOpensTheInbox() throws Exception {
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", ana.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/inbox").header("Authorization", doctorSession.bearer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/inbox")).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- HU-032

    @Test
    void hu032_ca01_ca03_historyIsCompleteAndReadableOnlyByOwnerAssignedProfessionalOrAdmin() throws Exception {
        long id = book(ana, doctor, 1, neurology, tuesday + "T08:00");
        mvc.perform(post("/api/v1/admin/appointments/" + id + "/decision").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECT\",\"reason\":\"Sin remisión\"}")).andExpect(status().isOk());

        long adminUser = db.queryForObject("select id from users where email='admin@example.com'", Long.class);
        history(ana, id).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].newStatus").value("REQUESTED")).andExpect(jsonPath("$[0].previousStatus").value((Object) null))
                .andExpect(jsonPath("$[0].source").value("USER"))
                .andExpect(jsonPath("$[1].previousStatus").value("REQUESTED")).andExpect(jsonPath("$[1].newStatus").value("REJECTED"))
                .andExpect(jsonPath("$[1].actorId").value(adminUser)).andExpect(jsonPath("$[1].source").value("ADMIN"))
                .andExpect(jsonPath("$[1].reason").value("Sin remisión")).andExpect(jsonPath("$[1].occurredAt").exists());
        history(doctorSession, id).andExpect(status().isOk());
        history(admin, id).andExpect(status().isOk());
        history(beto, id).andExpect(status().isNotFound());
        history(otherDoctorSession, id).andExpect(status().isNotFound());
    }

    @Test
    void hu032_ca02_historyHasNoWriteEndpointsAndOnlyGrows() throws Exception {
        long id = book(ana, doctor, 1, general, tuesday + "T08:00");
        mvc.perform(put("/api/v1/appointments/" + id + "/history").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("[]")).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/v1/appointments/" + id + "/history").header("Authorization", admin.bearer())).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/v1/appointments/" + id + "/cancel").header("Authorization", ana.bearer())).andExpect(status().isOk());
        history(ana, id).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].newStatus").value("APPROVED"))
                .andExpect(jsonPath("$[1].newStatus").value("CANCELLED"));
    }

    /** RN-12: ningún código de producción modifica ni borra el historial, ni lo mapea como entidad JPA editable. */
    @Test
    void hu032_ca02_productionCodeOnlyAppendsToTheHistory() throws IOException {
        Pattern mutation = Pattern.compile("(update\\s+appointment_status_history|delete\\s+from\\s+appointment_status_history|"
                + "@Table\\(\\s*name\\s*=\\s*\"appointment_status_history\")", Pattern.CASE_INSENSITIVE);
        List<Path> offenders;
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            offenders = files.filter(p -> p.toString().endsWith(".java")).filter(p -> {
                try {
                    return mutation.matcher(Files.readString(p)).find();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).toList();
        }
        assertThat(offenders).as("código que modifica appointment_status_history").isEmpty();
    }

    // ---------------------------------------------------------------- helpers

    private long book(AuthClient.Session who, long professional, long location, long specialty, String startAt) throws Exception {
        String body = mvc.perform(post("/api/v1/appointments").header("Authorization", who.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\":" + professional + ",\"locationId\":" + location + ",\"specialtyId\":" + specialty + ",\"startAt\":\"" + startAt + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    /** Cita general aprobada que se lleva al pasado para poder cerrarla. */
    private long pastApproved(AuthClient.Session who, String startAt) throws Exception {
        long id = book(who, doctor, 1, general, startAt);
        db.update("update appointments set scheduled_start_at=now() - interval 2 hour, scheduled_end_at=now() - interval 90 minute where id=?", id);
        return id;
    }

    private ResultActions agenda(AuthClient.Session who, String query) throws Exception {
        return mvc.perform(get("/api/v1/professional/appointments?" + query).header("Authorization", who.bearer()));
    }

    private ResultActions close(AuthClient.Session who, long id, String newStatus) throws Exception {
        return mvc.perform(post("/api/v1/professional/appointments/" + id + "/closure").header("Authorization", who.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + newStatus + "\"}"));
    }

    private ResultActions inbox(String query) throws Exception {
        return mvc.perform(get("/api/v1/admin/inbox?" + query).header("Authorization", admin.bearer()));
    }

    private ResultActions history(AuthClient.Session who, long id) throws Exception {
        return mvc.perform(get("/api/v1/appointments/" + id + "/history").header("Authorization", who.bearer()));
    }

    private void assertStatus(long id, String expected) {
        assertThat(db.queryForObject("select s.code from appointments a join appointment_statuses s on s.id=a.status_id where a.id=?", String.class, id))
                .isEqualTo(expected);
    }
}
