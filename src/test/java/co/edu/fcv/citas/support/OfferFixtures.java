package co.edu.fcv.citas.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

/** Crea profesionales completos (identidad, especialidades y sedes) por la API de administración. */
@Component
public class OfferFixtures {
    private final ObjectMapper json = new ObjectMapper();
    private final JdbcTemplate db;

    public OfferFixtures(JdbcTemplate db) {
        this.db = db;
    }

    public long specialtyId(String code) {
        return db.queryForObject("select id from specialties where code=?", Long.class, code);
    }

    /** Crea un profesional con la primera especialidad como primaria y las sedes indicadas. Su contraseña es {@link AuthClient#PASSWORD}. */
    public long professional(MockMvc mvc, AuthClient.Session admin, String email, String code, long[] specialtyIds, long... locationIds)
            throws Exception {
        String body = """
                {"firstName":"Pro","lastName":"%s","documentType":"CC","documentNumber":"D-%s","email":"%s","phone":"3100000000",
                 "initialPassword":"%s","professionalCode":"%s","licenseNumber":"RM-%s"}
                """.formatted(code, code, email, AuthClient.PASSWORD, code, code);
        String created = mvc.perform(post("/api/v1/admin/professionals").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();
        String assignments = Arrays.stream(specialtyIds).mapToObj(s -> "{\"specialtyId\":" + s + ",\"primary\":" + (s == specialtyIds[0]) + "}")
                .collect(Collectors.joining(",", "[", "]"));
        mvc.perform(put("/api/v1/admin/professionals/" + id + "/specialties").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"assignments\":" + assignments + "}")).andExpect(status().isOk());
        String locations = Arrays.stream(locationIds).mapToObj(Long::toString).collect(Collectors.joining(",", "[", "]"));
        mvc.perform(put("/api/v1/admin/professionals/" + id + "/locations").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"locationIds\":" + locations + "}")).andExpect(status().isOk());
        return id;
    }
}
