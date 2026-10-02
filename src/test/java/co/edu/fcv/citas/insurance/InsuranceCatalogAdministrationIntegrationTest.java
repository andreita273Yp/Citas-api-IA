package co.edu.fcv.citas.insurance;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-012 Gestionar EPS · HU-013 Gestionar planes EPS (RF-06). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class InsuranceCatalogAdministrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired JdbcTemplate db;
    private final ObjectMapper json = new ObjectMapper();
    private AuthClient.Session admin;
    private AuthClient.Session user;
    private long contributivo;

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        admin = auth.admin(mvc, database, "admin@example.com", "9401");
        auth.register(mvc, "user@example.com", "9402");
        user = auth.login(mvc, "user@example.com");
        contributivo = db.queryForObject("select id from insurance_regimes where code='CONTRIBUTIVO'", Long.class);
    }

    // ---------------------------------------------------------------- HU-012

    @Test
    void hu012_ca01_adminCreatesReadsAndUpdatesEps() throws Exception {
        long id = createEps(" eps demo ", "EPS Demo Salud").andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("EPS_DEMO")).andExpect(jsonPath("$.active").value(true)).andReturnId();
        mvc.perform(get("/api/v1/admin/eps").header("Authorization", admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == " + id + ")].name").value("EPS Demo Salud"));
        admin(patch("/api/v1/admin/eps/" + id), "{\"name\":\"EPS Demo Salud Total\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("EPS Demo Salud Total"));
        createEps("EPS_DEMO", "Otro nombre").andExpect(status().isConflict());
        createEps("OTRA", "eps demo salud total").andExpect(status().isConflict());
        createEps("", "Sin código").andExpect(status().isBadRequest());
        createEps("SIN_NOMBRE", " ").andExpect(status().isBadRequest());
        admin(patch("/api/v1/admin/eps/99999"), "{\"name\":\"X\"}").andExpect(status().isNotFound());
    }

    @Test
    void hu012_ca02_referencedEpsIsDeactivatedNotDeletedAndLeavesThePublicCatalog() throws Exception {
        long id = createEps("EPS_REF", "EPS Referenciada").andReturnId();
        long plan = createPlan(id, contributivo, "REF-1", "Plan Ref").andReturnId();
        mvc.perform(put("/api/v1/users/me/affiliation").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":" + plan + ",\"membershipNumber\":\"AF-1\"}")).andExpect(status().isOk());

        mvc.perform(delete("/api/v1/admin/eps/" + id).header("Authorization", admin.bearer())).andExpect(status().isMethodNotAllowed());
        admin(patch("/api/v1/admin/eps/" + id), "{\"active\":false}").andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        assertThat(db.queryForObject("select count(*) from eps where id=?", Integer.class, id)).isEqualTo(1);
        mvc.perform(get("/api/v1/catalogs/eps")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
        mvc.perform(get("/api/v1/catalogs/eps-plans?epsId=" + id)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", user.bearer())).andExpect(jsonPath("$.planId").value(plan));
    }

    @Test
    void hu012_ca03_hu013_onlyAdminManagesInsuranceCatalogs() throws Exception {
        mvc.perform(get("/api/v1/admin/eps").header("Authorization", user.bearer())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/eps").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"X\",\"name\":\"X\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/eps-plans").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"epsId\":1,\"regimeId\":1,\"code\":\"X\",\"name\":\"X\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/eps")).andExpect(status().isUnauthorized());
        assertThat(db.queryForObject("select count(*) from eps", Integer.class)).isZero();
    }

    // ---------------------------------------------------------------- HU-013

    @Test
    void hu013_ca01_planIsAssociatedToAValidEpsAndRegime() throws Exception {
        long eps = createEps("EPS_P", "EPS Planes").andReturnId();
        long plan = createPlan(eps, contributivo, " a-contrib ", "Plan Contributivo").andExpect(status().isCreated())
                .andExpect(jsonPath("$.epsId").value(eps)).andExpect(jsonPath("$.code").value("A-CONTRIB"))
                .andExpect(jsonPath("$.regimeCode").value("CONTRIBUTIVO")).andReturnId();
        long subsidiado = db.queryForObject("select id from insurance_regimes where code='SUBSIDIADO'", Long.class);
        admin(patch("/api/v1/admin/eps-plans/" + plan), "{\"name\":\"Plan Renombrado\",\"regimeId\":" + subsidiado + "}").andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plan Renombrado")).andExpect(jsonPath("$.regimeCode").value("SUBSIDIADO"));

        createPlan(99999, contributivo, "X", "Sin EPS").andExpect(status().isBadRequest());
        createPlan(eps, 99999, "Y", "Sin régimen").andExpect(status().isBadRequest());
        createPlan(eps, contributivo, "A-CONTRIB", "Duplicado").andExpect(status().isConflict());
        mvc.perform(get("/api/v1/admin/eps-plans?epsId=" + eps).header("Authorization", admin.bearer())).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void hu013_ca02_ca03_deactivatedPlanIsKeptAndNoLongerSelectable() throws Exception {
        long eps = createEps("EPS_S", "EPS Selección").andReturnId();
        long active = createPlan(eps, contributivo, "ACT", "Plan Activo").andReturnId();
        long retired = createPlan(eps, contributivo, "RET", "Plan Retirado").andReturnId();

        mvc.perform(delete("/api/v1/admin/eps-plans/" + retired).header("Authorization", admin.bearer())).andExpect(status().isMethodNotAllowed());
        admin(patch("/api/v1/admin/eps-plans/" + retired), "{\"active\":false}").andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        assertThat(db.queryForObject("select count(*) from eps_plans where id=?", Integer.class, retired)).isEqualTo(1);

        mvc.perform(get("/api/v1/catalogs/eps-plans?epsId=" + eps)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(active))
                .andExpect(jsonPath("$[0].regimeName").value("Contributivo"));
        mvc.perform(put("/api/v1/users/me/affiliation").header("Authorization", user.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":" + retired + ",\"membershipNumber\":\"AF-2\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/catalogs/regimes").header("Authorization", user.bearer())).andExpect(jsonPath("$.length()").value(5));
    }

    // ---------------------------------------------------------------- helpers

    private Result createEps(String code, String name) throws Exception {
        return new Result(admin(post("/api/v1/admin/eps"), "{\"code\":\"" + code + "\",\"name\":\"" + name + "\"}"));
    }

    private Result createPlan(long eps, long regime, String code, String name) throws Exception {
        return new Result(admin(post("/api/v1/admin/eps-plans"),
                "{\"epsId\":" + eps + ",\"regimeId\":" + regime + ",\"code\":\"" + code + "\",\"name\":\"" + name + "\"}"));
    }

    private ResultActions admin(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.header("Authorization", admin.bearer()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Envoltura para encadenar expectativas y extraer el id creado. */
    private final class Result {
        private final ResultActions actions;

        Result(ResultActions actions) { this.actions = actions; }

        Result andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            actions.andExpect(matcher);
            return this;
        }

        long andReturnId() throws Exception {
            actions.andExpect(status().isCreated());
            return json.readTree(actions.andReturn().getResponse().getContentAsString()).get("id").asLong();
        }
    }
}
