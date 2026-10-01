package co.edu.fcv.citas.scheduling;

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
import co.edu.fcv.citas.support.OfferFixtures;
import co.edu.fcv.citas.support.TestDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-018 Crear bloques · HU-019 Modificar bloques futuros · HU-020 Consultar calendario · HU-017 CA-03. */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class AgendaBlocksIntegrationTest {
    private static final String BLOCKS = "/api/v1/professional/blocks";
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired OfferFixtures offer;
    @Autowired JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();

    private AuthClient.Session admin;
    private AuthClient.Session professional;
    private long professionalId;
    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        admin = auth.admin(mvc, database, "admin@example.com", "8001");
        professionalId = offer.professional(mvc, admin, "doc@example.com", "PROF-A", new long[] {offer.specialtyId("MEDICINA_GENERAL")}, 1);
        professional = auth.login(mvc, "doc@example.com");
    }

    // ---------------------------------------------------------------- HU-018

    @Test
    void hu018_ca01_futureBlockInAnAssignedLocationIsPublishedAs30MinuteSlots() throws Exception {
        create(professional, 1, tomorrow, "08:00", "12:00").andExpect(status().isCreated())
                .andExpect(jsonPath("$.locationCode").value("HIC"))
                .andExpect(jsonPath("$.date").value(tomorrow.toString()))
                .andExpect(jsonPath("$.startTime").value("08:00"))
                .andExpect(jsonPath("$.endTime").value("12:00"))
                .andExpect(jsonPath("$.slots").value(8))
                .andExpect(jsonPath("$.bookedSlots").value(0));
        assertThat(db.queryForList("select date_format(start_at,'%H:%i') from professional_slots order by start_at", String.class))
                .containsExactly("08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30");
    }

    @Test
    void hu018_ca02_pastOverlappingMisalignedOrUnassignedBlocksAreRejectedWithoutPublishing() throws Exception {
        create(professional, 1, LocalDate.now().minusDays(1), "08:00", "10:00").andExpect(status().isBadRequest());
        create(professional, 1, tomorrow, "08:15", "10:00").andExpect(status().isBadRequest());
        create(professional, 1, tomorrow, "10:00", "09:00").andExpect(status().isBadRequest());
        create(professional, 1, tomorrow, "10:00", "10:00").andExpect(status().isBadRequest());
        create(professional, 2, tomorrow, "08:00", "10:00").andExpect(status().isConflict()); // ICV no asignada

        create(professional, 1, tomorrow, "08:00", "12:00").andExpect(status().isCreated());
        create(professional, 1, tomorrow, "11:30", "13:00").andExpect(status().isConflict());  // solapa
        create(professional, 1, tomorrow, "07:00", "08:30").andExpect(status().isConflict());
        create(professional, 1, tomorrow, "09:00", "10:00").andExpect(status().isConflict());  // contenido
        assertThat(db.queryForObject("select count(*) from availability_blocks", Integer.class)).isEqualTo(1);
        assertThat(db.queryForObject("select count(*) from professional_slots", Integer.class)).isEqualTo(8);
    }

    @Test
    void hu018_ca03_twoSeparateBlocksOnTheSameDayDoNotPublishTheGapBetweenThem() throws Exception {
        create(professional, 1, tomorrow, "08:00", "12:00").andExpect(status().isCreated());
        create(professional, 1, tomorrow, "14:00", "17:00").andExpect(status().isCreated());
        long general = offer.specialtyId("MEDICINA_GENERAL");
        mvc.perform(get("/api/v1/availability?specialtyId=" + general + "&date=" + tomorrow).header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(14))
                .andExpect(jsonPath("$[?(@.startAt =~ /.*T1[23]:.*/)]").isEmpty());
    }

    @Test
    void hu017_ca03_inactiveProfessionalCannotPublishAvailability() throws Exception {
        mvc.perform(patch("/api/v1/admin/professionals/" + professionalId).header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isOk());
        create(professional, 1, tomorrow, "08:00", "10:00").andExpect(status().isConflict());
        assertThat(db.queryForObject("select count(*) from availability_blocks", Integer.class)).isZero();
    }

    @Test
    void onlyProfessionalsManageBlocks() throws Exception {
        auth.register(mvc, "user@example.com", "8002");
        AuthClient.Session user = auth.login(mvc, "user@example.com");
        create(user, 1, tomorrow, "08:00", "10:00").andExpect(status().isForbidden());
        create(admin, 1, tomorrow, "08:00", "10:00").andExpect(status().isForbidden());
        mvc.perform(post(BLOCKS).contentType(MediaType.APPLICATION_JSON).content(body(1, tomorrow, "08:00", "10:00")))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- HU-019

    @Test
    void hu019_ca01_editingAFutureFreeBlockRecalculatesItsSlots() throws Exception {
        long block = createdId(professional, 1, tomorrow, "08:00", "10:00");
        mvc.perform(put(BLOCKS + "/" + block).header("Authorization", professional.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(1, tomorrow, "09:00", "12:00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slots").value(6)).andExpect(jsonPath("$.startTime").value("09:00"));
        assertThat(db.queryForList("select date_format(start_at,'%H:%i') from professional_slots order by start_at", String.class))
                .containsExactly("09:00", "09:30", "10:00", "10:30", "11:00", "11:30");
    }

    @Test
    void hu019_ca02_deletingAFutureFreeBlockStopsOfferingItsSlots() throws Exception {
        long block = createdId(professional, 1, tomorrow, "08:00", "10:00");
        mvc.perform(delete(BLOCKS + "/" + block).header("Authorization", professional.bearer())).andExpect(status().isNoContent());
        assertThat(db.queryForObject("select count(*) from professional_slots", Integer.class)).isZero();
        assertThat(db.queryForObject("select count(*) from availability_blocks", Integer.class)).isZero();
    }

    @Test
    void hu019_ca03_pastForeignOrCommittedBlocksCannotBeChanged() throws Exception {
        long block = createdId(professional, 1, tomorrow, "08:00", "10:00");

        offer.professional(mvc, admin, "otro@example.com", "PROF-B", new long[] {offer.specialtyId("MEDICINA_GENERAL")}, 1);
        AuthClient.Session other = auth.login(mvc, "otro@example.com");
        mvc.perform(delete(BLOCKS + "/" + block).header("Authorization", other.bearer())).andExpect(status().isNotFound());
        mvc.perform(put(BLOCKS + "/" + block).header("Authorization", other.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(1, tomorrow, "08:00", "09:00"))).andExpect(status().isNotFound());

        auth.register(mvc, "paciente@example.com", "8003");
        AuthClient.Session patient = auth.login(mvc, "paciente@example.com");
        mvc.perform(post("/api/v1/appointments").header("Authorization", patient.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"professionalId\":" + professionalId + ",\"locationId\":1,\"specialtyId\":" + offer.specialtyId("MEDICINA_GENERAL")
                        + ",\"startAt\":\"" + tomorrow + "T09:00\"}")).andExpect(status().isCreated());
        mvc.perform(delete(BLOCKS + "/" + block).header("Authorization", professional.bearer())).andExpect(status().isConflict());
        mvc.perform(put(BLOCKS + "/" + block).header("Authorization", professional.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content(body(1, tomorrow, "10:00", "11:00"))).andExpect(status().isConflict());
        assertThat(db.queryForObject("select count(*) from professional_slots where appointment_id is not null", Integer.class)).isEqualTo(1);

        db.update("update availability_blocks set available_date=? where id=?", LocalDate.now().minusDays(2), block);
        db.update("update professional_slots set appointment_id=null");
        mvc.perform(delete(BLOCKS + "/" + block).header("Authorization", professional.bearer())).andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------- HU-020

    @Test
    void hu020_professionalSeesOnlyOwnBlocksFilteredByDateAndLocation() throws Exception {
        mvc.perform(put("/api/v1/admin/professionals/" + professionalId + "/locations").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"locationIds\":[1,2]}")).andExpect(status().isOk());
        create(professional, 1, tomorrow, "08:00", "10:00").andExpect(status().isCreated());
        create(professional, 2, tomorrow.plusDays(1), "08:00", "10:00").andExpect(status().isCreated());
        offer.professional(mvc, admin, "otro2@example.com", "PROF-C", new long[] {offer.specialtyId("MEDICINA_GENERAL")}, 1);
        AuthClient.Session other = auth.login(mvc, "otro2@example.com");
        create(other, 1, tomorrow, "08:00", "10:00").andExpect(status().isCreated());

        mvc.perform(get(BLOCKS).header("Authorization", professional.bearer())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get(BLOCKS + "?locationId=2").header("Authorization", professional.bearer()))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].locationCode").value("ICV"));
        mvc.perform(get(BLOCKS + "?from=" + tomorrow + "&to=" + tomorrow).header("Authorization", professional.bearer()))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].date").value(tomorrow.toString()));
        mvc.perform(get(BLOCKS).header("Authorization", other.bearer())).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/professional/me").header("Authorization", professional.bearer())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(professionalId)).andExpect(jsonPath("$.locations.length()").value(2));
    }

    // ---------------------------------------------------------------- helpers

    private static String body(long location, LocalDate date, String start, String end) {
        return "{\"locationId\":" + location + ",\"date\":\"" + date + "\",\"startTime\":\"" + start + "\",\"endTime\":\"" + end + "\"}";
    }

    private ResultActions create(AuthClient.Session who, long location, LocalDate date, String start, String end) throws Exception {
        return mvc.perform(post(BLOCKS).header("Authorization", who.bearer()).contentType(MediaType.APPLICATION_JSON).content(body(location, date, start, end)));
    }

    private long createdId(AuthClient.Session who, long location, LocalDate date, String start, String end) throws Exception {
        return json.readTree(create(who, location, date, start, end).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asLong();
    }
}
