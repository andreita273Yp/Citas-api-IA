package co.edu.fcv.citas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** HU-001 · Fundación técnica: límites hexagonales, configuración segura, CORS explícito y health. */
@SpringBootTest(classes = CitasApiApplication.class)
@AutoConfigureMockMvc
class FoundationIntegrationTest {
    private static final Path MAIN_SOURCES = Path.of("src/main/java/co/edu/fcv/citas");
    private static final Pattern FRAMEWORK_IMPORT = Pattern.compile("^import\\s+(static\\s+)?(org\\.springframework|jakarta\\.(persistence|servlet)|org\\.hibernate)\\.", Pattern.MULTILINE);

    @Autowired MockMvc mvc;

    @Test
    void ca02_domainAndApplicationLayersDoNotDependOnSpringJpaOrHttp() throws IOException {
        List<Path> layerFiles;
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            layerFiles = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().replace('\\', '/').matches(".*/(domain|application)/.*")).toList();
        }
        assertThat(layerFiles).as("debe existir código de dominio/aplicación").isNotEmpty();
        for (Path file : layerFiles) {
            assertThat(FRAMEWORK_IMPORT.matcher(Files.readString(file)).find())
                    .as("%s no debe importar Spring, JPA, Servlet ni Hibernate", file).isFalse();
        }
    }

    @Test
    void t02_healthIsPublicAndRevealsNoDetails() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void t02_corsAllowsOnlyTheConfiguredFrontendOrigin() throws Exception {
        mvc.perform(options("/api/v1/catalogs/roles").header("Origin", "http://localhost:4200").header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/v1/catalogs/roles").header("Origin", "https://evil.example").header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
