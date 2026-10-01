package co.edu.fcv.citas.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.fcv.citas.CitasApiApplication;
import co.edu.fcv.citas.support.AuthClient;
import co.edu.fcv.citas.support.TestDatabase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** HU-007 · Renovar y cerrar sesión (CA-01 a CA-04). */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class SessionLifecycleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TestDatabase database;
    @Autowired AuthClient auth;
    @Autowired JdbcTemplate db;
    @Value("${app.jwt.refresh-secret}") String refreshSecret;

    private AuthClient.Session session;

    @BeforeEach
    void signIn() throws Exception {
        database.reset();
        auth.register(mvc, "ana@example.com", "3001");
        session = auth.login(mvc, "ana@example.com");
    }

    @Test
    void ca01_ca04_validRefreshRotatesBothTokensAndTheOldOneCannotBeReused() throws Exception {
        MvcResult rotated = refresh(session.refreshCookie()).andExpect(status().isOk()).andReturn();
        Cookie next = rotated.getResponse().getCookie("refresh_token");
        assertThat(next.getValue()).isNotEqualTo(session.refreshCookie().getValue());
        assertThat(auth.accessToken(rotated)).isNotBlank();

        refresh(session.refreshCookie()).andExpect(status().isUnauthorized());
        refresh(next).andExpect(status().isOk());
    }

    @Test
    void ca02_missingForgedExpiredOrAccessTypeTokensAreRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
        refresh(new Cookie("refresh_token", "no-es-un-jwt")).andExpect(status().isUnauthorized());
        refresh(new Cookie("refresh_token", session.accessToken())).andExpect(status().isUnauthorized());

        String forged = Jwts.builder().subject("1").id(UUID.randomUUID().toString()).claim("typ", "refresh")
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor("otro-secreto-de-al-menos-treinta-y-dos-bytes".getBytes(StandardCharsets.UTF_8)))
                .compact();
        refresh(new Cookie("refresh_token", forged)).andExpect(status().isUnauthorized());

        String expired = Jwts.builder().subject("1").id(UUID.randomUUID().toString()).claim("typ", "refresh")
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        refresh(new Cookie("refresh_token", expired)).andExpect(status().isUnauthorized());
    }

    @Test
    void ca02_refreshWhosePersistedSessionExpiredIsRejected() throws Exception {
        db.update("update refresh_tokens set expires_at = now() - interval 1 minute");
        refresh(session.refreshCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void ca03_logoutRevokesTheSessionAndClearsTheCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").cookie(session.refreshCookie()))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", Matchers.allOf(Matchers.containsString("refresh_token="), Matchers.containsString("Max-Age=0"))));
        assertThat(db.queryForObject("select count(*) from refresh_tokens where revoked_at is null", Integer.class)).isZero();
        refresh(session.refreshCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void ca04_concurrentRefreshWithTheSameTokenAllowsExactlyOneRotation() throws Exception {
        int attempts = 4;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                Callable<Integer> call = () -> {
                    start.await();
                    return refresh(session.refreshCookie()).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) statuses.add(result.get());
            assertThat(statuses).containsOnly(200, 401);
            assertThat(statuses.stream().filter(s -> s == 200)).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie cookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").cookie(cookie));
    }
}
