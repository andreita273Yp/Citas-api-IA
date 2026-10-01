package co.edu.fcv.citas.auth;
import co.edu.fcv.citas.CitasApiApplication; import co.edu.fcv.citas.support.TestDatabase; import org.junit.jupiter.api.*; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.test.web.servlet.MockMvc; import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(classes=CitasApiApplication.class) @AutoConfigureMockMvc class AuthIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired TestDatabase database;
 private final String registration="{\"firstName\":\"Ana\",\"lastName\":\"Prueba\",\"documentType\":\"CC\",\"documentNumber\":\"12345\",\"email\":\"ANA@EXAMPLE.COM\",\"phone\":\"3001234567\",\"password\":\"password-segura\"}";
 @BeforeEach void clean(){database.reset();}
 @Test void registersThenLogsInAndRotatesRefresh() throws Exception {
  mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration)).andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("ana@example.com")).andExpect(jsonPath("$.roles[0]").value("USER"));
  var login=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ana@example.com\",\"password\":\"password-segura\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer")).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("HttpOnly"))).andReturn();
  var cookie=login.getResponse().getCookie("refresh_token");
  mvc.perform(post("/api/v1/auth/refresh").cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
  mvc.perform(post("/api/v1/auth/logout").cookie(cookie)).andExpect(status().isNoContent());
 }
 @Test void rejectsDuplicateIdentityAndBadCredentials() throws Exception {
  mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration)).andExpect(status().isCreated());
  mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration)).andExpect(status().isConflict());
  mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ana@example.com\",\"password\":\"otra-clave\"}")).andExpect(status().isUnauthorized());
 }
 @Test void allowsProtectedAuthRequestsFromConfiguredOriginWithXmlHttpRequest() throws Exception {
  mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration)).andExpect(status().isCreated());
  var login=mvc.perform(post("/api/v1/auth/login").header("Origin","http://localhost:4200").header("X-Requested-With","XMLHttpRequest").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"ana@example.com\",\"password\":\"password-segura\"}")).andExpect(status().isOk()).andReturn();
  var refresh=login.getResponse().getCookie("refresh_token");
  var rotated=mvc.perform(post("/api/v1/auth/refresh").header("Origin","http://localhost:4200").header("X-Requested-With","XMLHttpRequest").cookie(refresh)).andExpect(status().isOk()).andReturn().getResponse().getCookie("refresh_token");
  mvc.perform(post("/api/v1/auth/logout").header("Origin","http://localhost:4200").header("X-Requested-With","XMLHttpRequest").cookie(rotated)).andExpect(status().isNoContent());
 }
 @Test void rejectsProtectedAuthRequestsFromUnallowedOrigin() throws Exception {
  for(String path:java.util.List.of("/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/logout")) mvc.perform(post(path).header("Origin","https://untrusted.example").header("X-Requested-With","XMLHttpRequest")).andExpect(status().isForbidden());
 }
 @Test void rejectsCrossOriginProtectedAuthRequestsWithoutXmlHttpRequestHeader() throws Exception {
  for(String path:java.util.List.of("/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/logout")) mvc.perform(post(path).header("Origin","http://localhost:4200")).andExpect(status().isForbidden());
 }
}
