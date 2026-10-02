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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-021 Buscar disponibilidad · HU-022 Cita general · HU-023 Cita especializada (RN-01, RN-02, RN-05 a RN-08). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class BookingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired OfferFixtures offer;
    @Autowired JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();

    private AuthClient.Session patient;
    private long general;
    private long cardiology;
    private long neurology; // 60 min
    private long generalist;
    private long specialist;
    private final LocalDate day = LocalDate.now().plusDays(2);

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        AuthClient.Session admin = auth.admin(mvc, database, "admin@example.com", "9001");
        general = offer.specialtyId("MEDICINA_GENERAL");
        cardiology = offer.specialtyId("CARDIOLOGIA_ADULTO");
        neurology = offer.specialtyId("NEUROLOGIA");
        generalist = offer.professional(mvc, admin, "general@example.com", "PROF-G", new long[] {general}, 1, 2);
        specialist = offer.professional(mvc, admin, "especialista@example.com", "PROF-E", new long[] {cardiology, neurology}, 1);
        database.publishBlock(generalist, 1, day, LocalTime.of(8, 0), LocalTime.of(10, 0));
        database.publishBlock(generalist, 2, day.plusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0));
        database.publishBlock(specialist, 1, day, LocalTime.of(8, 0), LocalTime.of(10, 0));
        auth.register(mvc, "paciente@example.com", "9002");
        patient = auth.login(mvc, "paciente@example.com");
    }

    // ---------------------------------------------------------------- HU-021

    @Test
    void hu021_ca01_searchCombinesLocationProfessionalSpecialtyAndDateFilters() throws Exception {
        search("specialtyId=" + general + "&date=" + day).andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].professionalId").value(generalist)).andExpect(jsonPath("$[0].locationCode").value("HIC"))
                .andExpect(jsonPath("$[0].professionalName").exists());
        search("specialtyId=" + general + "&date=" + day.plusDays(1) + "&locationId=2").andExpect(jsonPath("$.length()").value(2));
        search("specialtyId=" + general + "&date=" + day + "&locationId=2").andExpect(jsonPath("$.length()").value(0));
        search("specialtyId=" + cardiology + "&date=" + day + "&professionalId=" + specialist).andExpect(jsonPath("$.length()").value(4));
        search("specialtyId=" + cardiology + "&date=" + day + "&professionalId=" + generalist).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/catalogs/specialties?type=GENERAL").header("Authorization", patient.bearer()))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].code").value("MEDICINA_GENERAL"));
        mvc.perform(get("/api/v1/catalogs/specialties?type=SPECIALIZED").header("Authorization", patient.bearer()))
                .andExpect(jsonPath("$.length()").value(11));
    }

    @Test
    void hu021_ca02_sixtyMinuteSpecialtyOnlyOffersStartsWithTwoConsecutiveFreeSlots() throws Exception {
        search("specialtyId=" + neurology + "&date=" + day).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].durationMinutes").value(60)).andExpect(jsonPath("$[2].startAt").value(day + "T09:00:00"));
        // Ocupar 08:30 parte las franjas: solo queda 09:00-10:00.
        book("especialista", specialist, 1, cardiology, day + "T08:30").andExpect(status().isCreated());
        search("specialtyId=" + neurology + "&date=" + day).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startAt").value(day + "T09:00:00")).andExpect(jsonPath("$[0].endAt").value(day + "T10:00:00"));
        search("specialtyId=" + cardiology + "&date=" + day).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void hu021_pastSlotsAreNeverOffered() throws Exception {
        LocalDate today = LocalDate.now();
        database.publishBlock(generalist, 1, today, LocalTime.of(0, 0), LocalTime.of(0, 30));
        search("specialtyId=" + general + "&date=" + today).andExpect(jsonPath("$[?(@.startAt =~ /.*T00:00.*/)]").isEmpty());
    }

    @Test
    void hu021_ca03_inactiveOrUnassociatedOfferIsNotReturned() throws Exception {
        search("specialtyId=" + general + "&date=" + day + "&professionalId=" + specialist).andExpect(jsonPath("$.length()").value(0));
        db.update("update specialties set active=false where id=?", cardiology);
        search("specialtyId=" + cardiology + "&date=" + day).andExpect(status().isNotFound());
        db.update("update specialties set active=true where id=?", cardiology);
        db.update("update professional_locations set active=false where professional_id=?", specialist);
        search("specialtyId=" + cardiology + "&date=" + day).andExpect(jsonPath("$.length()").value(0));
    }

    // ---------------------------------------------------------------- HU-022

    @Test
    void hu022_ca01_ca03_generalAppointmentIsApprovedAutomaticallyAndAudited() throws Exception {
        String created = book("general", generalist, 1, general, day + "T08:00").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.durationMinutes").value(30))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        assertThat(db.queryForObject("select count(*) from professional_slots where appointment_id=?", Integer.class, id)).isEqualTo(1);
        assertThat(db.queryForObject("select approved_at is not null from appointments where id=?", Boolean.class, id)).isTrue();
        mvc.perform(get("/api/v1/appointments/" + id + "/history").header("Authorization", patient.bearer()))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].newStatus").value("APPROVED"))
                .andExpect(jsonPath("$[0].source").value("SYSTEM")).andExpect(jsonPath("$[0].occurredAt").exists());
    }

    @Test
    void hu022_ca02_aSlotTakenBetweenSearchAndConfirmationIsRejected() throws Exception {
        search("specialtyId=" + general + "&date=" + day).andExpect(jsonPath("$.length()").value(4));
        book("general", generalist, 1, general, day + "T08:00").andExpect(status().isCreated());
        auth.register(mvc, "otro@example.com", "9003");
        AuthClient.Session other = auth.login(mvc, "otro@example.com");
        mvc.perform(post("/api/v1/appointments").header("Authorization", other.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(generalist, 1, general, day + "T08:00"))).andExpect(status().isConflict());
        assertThat(db.queryForObject("select count(*) from appointments", Integer.class)).isEqualTo(1);
    }

    @Test
    void hu022_ca02_concurrentBookingsOfTheSameSlotProduceExactlyOneAppointment() throws Exception {
        int attempts = 8;
        List<AuthClient.Session> patients = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            auth.register(mvc, "c" + i + "@example.com", "C-" + i);
            patients.add(auth.login(mvc, "c" + i + "@example.com"));
        }
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (AuthClient.Session p : patients) {
                Callable<Integer> call = () -> {
                    start.await();
                    return mvc.perform(post("/api/v1/appointments").header("Authorization", p.bearer()).contentType(MediaType.APPLICATION_JSON)
                            .content(booking(generalist, 1, general, day + "T09:00"))).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : results) statuses.add(f.get());
            assertThat(statuses).containsOnly(201, 409);
            assertThat(statuses.stream().filter(s -> s == 201)).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(db.queryForObject("select count(*) from appointments", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from professional_slots where appointment_id is not null", Integer.class)).isEqualTo(1);
    }

    @Test
    void bookingRejectsPastMisalignedAndWrongLocationOrSpecialty() throws Exception {
        database.publishBlock(generalist, 1, LocalDate.now().minusDays(1), LocalTime.of(8, 0), LocalTime.of(9, 0));
        book("general", generalist, 1, general, LocalDate.now().minusDays(1) + "T08:00").andExpect(status().isBadRequest());
        book("general", generalist, 1, general, day + "T08:15").andExpect(status().isBadRequest());
        book("general", generalist, 2, general, day + "T08:00").andExpect(status().isConflict());     // sin bloque en ICV ese día
        book("especialista", specialist, 2, cardiology, day + "T08:00").andExpect(status().isConflict()); // sede no asignada
        book("especialista", specialist, 1, general, day + "T08:00").andExpect(status().isConflict());    // especialidad no asociada
        db.update("update specialties set active=false where id=?", cardiology);
        book("especialista", specialist, 1, cardiology, day + "T08:00").andExpect(status().isNotFound()); // especialidad inactiva
        assertThat(db.queryForObject("select count(*) from appointments", Integer.class)).isZero();
    }

    @Test
    void onlyUsersBookTheirOwnAppointments() throws Exception {
        AuthClient.Session professional = auth.login(mvc, "general@example.com");
        mvc.perform(post("/api/v1/appointments").header("Authorization", professional.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(generalist, 1, general, day + "T08:00"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/appointments").contentType(MediaType.APPLICATION_JSON)
                .content(booking(generalist, 1, general, day + "T08:00"))).andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- HU-023

    @Test
    void hu023_ca01_ca02_ca03_specializedRequestIsRequestedHoldsBothSlotsAndIsAudited() throws Exception {
        String created = book("especialista", specialist, 1, neurology, day + "T08:00").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED")).andExpect(jsonPath("$.durationMinutes").value(60))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        assertThat(db.queryForList("select date_format(start_at,'%H:%i') from professional_slots where appointment_id=? order by start_at", String.class, id))
                .containsExactly("08:00", "08:30");

        auth.register(mvc, "otro@example.com", "9004");
        AuthClient.Session other = auth.login(mvc, "otro@example.com");
        mvc.perform(post("/api/v1/appointments").header("Authorization", other.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(specialist, 1, cardiology, day + "T08:30"))).andExpect(status().isConflict());
        search("specialtyId=" + cardiology + "&date=" + day).andExpect(jsonPath("$[?(@.startAt =~ /.*T08:.*/)]").isEmpty());

        mvc.perform(get("/api/v1/appointments/" + id + "/history").header("Authorization", patient.bearer()))
                .andExpect(jsonPath("$[0].newStatus").value("REQUESTED")).andExpect(jsonPath("$[0].source").value("USER"));
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions search(String query) throws Exception {
        return mvc.perform(get("/api/v1/availability?" + query).header("Authorization", patient.bearer()));
    }

    private static String booking(long professional, long location, long specialty, String startAt) {
        return "{\"professionalId\":" + professional + ",\"locationId\":" + location + ",\"specialtyId\":" + specialty
                + ",\"startAt\":\"" + startAt + "\",\"reason\":\"Control sintético\"}";
    }

    private ResultActions book(String label, long professional, long location, long specialty, String startAt) throws Exception {
        return mvc.perform(post("/api/v1/appointments").header("Authorization", patient.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(professional, location, specialty, startAt)));
    }
}
