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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** HU-010 Gestionar perfil · HU-011 Gestionar afiliación (RF-04). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class ProfileAndAffiliationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired JdbcTemplate db;
    private AuthClient.Session ana;
    private AuthClient.Session beto;
    private long planContributivo;
    private long planSubsidiado;
    private long otherEpsPlan;
    private long epsA;

    @BeforeEach
    void setUp() throws Exception {
        database.reset();
        auth.register(mvc, "ana@example.com", "9301");
        auth.register(mvc, "beto@example.com", "9302");
        ana = auth.login(mvc, "ana@example.com");
        beto = auth.login(mvc, "beto@example.com");
        epsA = database.insertEps("EPS_A", "EPS Sintética A");
        long epsB = database.insertEps("EPS_B", "EPS Sintética B");
        planContributivo = database.insertPlan(epsA, "CONTRIBUTIVO", "A-C", "Plan A Contributivo");
        planSubsidiado = database.insertPlan(epsA, "SUBSIDIADO", "A-S", "Plan A Subsidiado");
        otherEpsPlan = database.insertPlan(epsB, "CONTRIBUTIVO", "B-C", "Plan B");
    }

    // ---------------------------------------------------------------- HU-010

    @Test
    void hu010_ca01_userReadsOnlyOwnAllowedData() throws Exception {
        mvc.perform(get("/api/v1/users/me").header("Authorization", ana.bearer())).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com")).andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void hu010_ca02_ca03_onlyThePhoneCanBeUpdatedAndItIsValidated() throws Exception {
        mvc.perform(patch("/api/v1/users/me").header("Authorization", ana.bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\" +57 300 999 8877 \",\"email\":\"hacker@example.com\",\"firstName\":\"Otro\",\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("+57 300 999 8877"))
                .andExpect(jsonPath("$.email").value("ana@example.com")).andExpect(jsonPath("$.firstName").value("Ana"));
        mvc.perform(get("/api/v1/users/me").header("Authorization", ana.bearer())).andExpect(jsonPath("$.phone").value("+57 300 999 8877"));
        assertThat(db.queryForList("select r.code from user_roles ur join roles r on r.id=ur.role_id join users u on u.id=ur.user_id where u.email='ana@example.com'", String.class))
                .containsExactly("USER");

        for (String invalid : new String[] {"", "  ", "abc", "12", "3001234567890123456789012345678901"}) {
            mvc.perform(patch("/api/v1/users/me").header("Authorization", ana.bearer()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"phone\":\"" + invalid + "\"}")).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/users/me").header("Authorization", beto.bearer())).andExpect(jsonPath("$.phone").value("3001234567"));
    }

    // ---------------------------------------------------------------- HU-011

    @Test
    void hu011_ca01_userAssociatesAValidPlanAndGetsEpsAndRegimeFromIt() throws Exception {
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", ana.bearer())).andExpect(status().isNoContent());
        affiliate(ana, planContributivo, "AF-001").andExpect(status().isOk())
                .andExpect(jsonPath("$.planId").value(planContributivo)).andExpect(jsonPath("$.planName").value("Plan A Contributivo"))
                .andExpect(jsonPath("$.epsId").value(epsA)).andExpect(jsonPath("$.epsName").value("EPS Sintética A"))
                .andExpect(jsonPath("$.regimeCode").value("CONTRIBUTIVO")).andExpect(jsonPath("$.membershipNumber").value("AF-001"));
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", ana.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(planContributivo));
    }

    @Test
    void hu011_ca02_repeatingThePlanDoesNotDuplicateAndChangingItKeepsASingleCurrentAffiliation() throws Exception {
        affiliate(ana, planContributivo, "AF-001").andExpect(status().isOk());
        affiliate(ana, planContributivo, "AF-001").andExpect(status().isOk());
        assertThat(rows("ana@example.com")).isEqualTo(1);

        affiliate(ana, planSubsidiado, "AF-002").andExpect(status().isOk()).andExpect(jsonPath("$.regimeCode").value("SUBSIDIADO"));
        assertThat(rows("ana@example.com")).isEqualTo(2);
        assertThat(db.queryForObject("select count(*) from user_insurance_affiliations a join users u on u.id=a.user_id where u.email='ana@example.com' and a.is_current=true", Integer.class))
                .isEqualTo(1);

        mvc.perform(delete("/api/v1/users/me/affiliation").header("Authorization", ana.bearer())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", ana.bearer())).andExpect(status().isNoContent());
    }

    @Test
    void hu011_inactivePlansOrPlansOfInactiveEpsCannotBeSelected() throws Exception {
        db.update("update eps_plans set active=false where id=?", planSubsidiado);
        affiliate(ana, planSubsidiado, "AF-003").andExpect(status().isBadRequest());
        db.update("update eps set active=false where id=?", epsA);
        affiliate(ana, planContributivo, "AF-004").andExpect(status().isBadRequest());
        affiliate(ana, 99999, "AF-005").andExpect(status().isBadRequest());
        affiliate(ana, otherEpsPlan, " ").andExpect(status().isBadRequest());
        assertThat(rows("ana@example.com")).isZero();
    }

    @Test
    void hu011_ca03_eachUserOperatesOnlyOnOwnAffiliation() throws Exception {
        affiliate(ana, planContributivo, "AF-001").andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", beto.bearer())).andExpect(status().isNoContent());
        affiliate(beto, otherEpsPlan, "AF-900").andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", ana.bearer())).andExpect(jsonPath("$.planId").value(planContributivo));
        mvc.perform(delete("/api/v1/users/me/affiliation").header("Authorization", beto.bearer())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/me/affiliation").header("Authorization", ana.bearer())).andExpect(status().isOk());
    }

    @Test
    void registrationAcceptsAnOptionalActivePlan() throws Exception {
        String body = auth.registrationJson("con-plan@example.com", "9303").replace("\"password\"", "\"insurancePlanId\":" + planContributivo + ",\"password\"");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        assertThat(rows("con-plan@example.com")).isEqualTo(1);
        db.update("update eps_plans set active=false where id=?", planContributivo);
        String rejected = auth.registrationJson("plan-inactivo@example.com", "9304").replace("\"password\"", "\"insurancePlanId\":" + planContributivo + ",\"password\"");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(rejected)).andExpect(status().isBadRequest());
    }

    private ResultActions affiliate(AuthClient.Session who, long planId, String membership) throws Exception {
        return mvc.perform(put("/api/v1/users/me/affiliation").header("Authorization", who.bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planId\":" + planId + ",\"membershipNumber\":\"" + membership + "\"}"));
    }

    private int rows(String email) {
        return db.queryForObject("select count(*) from user_insurance_affiliations a join users u on u.id=a.user_id where u.email=?", Integer.class, email);
    }
}
