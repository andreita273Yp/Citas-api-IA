package co.edu.fcv.citas.auth;
import jakarta.servlet.*; import jakarta.servlet.http.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.*; import org.springframework.http.HttpHeaders; import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; import org.springframework.security.config.http.SessionCreationPolicy; import org.springframework.security.core.authority.SimpleGrantedAuthority; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.*; import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; import org.springframework.stereotype.Component; import org.springframework.web.cors.*; import org.springframework.web.filter.OncePerRequestFilter; import java.io.IOException; import java.util.*;
@Configuration @EnableWebSecurity public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain security(HttpSecurity http,JwtFilter jwt,AuthRequestGuard authRequestGuard) throws Exception{return http.csrf(csrf->csrf.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).exceptionHandling(e->e.authenticationEntryPoint((request,response,error)->response.sendError(HttpServletResponse.SC_UNAUTHORIZED))).authorizeHttpRequests(a->a.requestMatchers("/api/v1/auth/**","/api/v1/catalogs/eps","/api/v1/catalogs/eps-plans","/actuator/health","/error").permitAll().anyRequest().authenticated()).addFilterBefore(authRequestGuard,UsernamePasswordAuthenticationFilter.class).addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class).build();}
 @Bean("corsConfigurationSource") CorsConfigurationSource cors(@Value("${app.cors.allowed-origin}") String origin){var c=new CorsConfiguration();c.setAllowedOriginPatterns(List.of(origin,"http://localhost:4200"));c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Content-Type","Authorization","X-Requested-With"));c.setAllowCredentials(true);var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/api/**",c);return source;}
}
@Component class AuthRequestGuard extends OncePerRequestFilter {
 private static final Set<String> PROTECTED_PATHS=Set.of("/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/logout");
 private final String allowedOrigin;
 AuthRequestGuard(@Value("${app.cors.allowed-origin}") String allowedOrigin){this.allowedOrigin=allowedOrigin;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
  if("POST".equals(request.getMethod())&&PROTECTED_PATHS.contains(request.getRequestURI())){
   String origin=request.getHeader(HttpHeaders.ORIGIN);
   if(origin!=null&&(!allowedOrigin.equals(origin)||!"XMLHttpRequest".equals(request.getHeader("X-Requested-With")))){response.sendError(HttpServletResponse.SC_FORBIDDEN);return;}
  }
  chain.doFilter(request,response);
 }
}
@Component class JwtFilter extends OncePerRequestFilter {
 private final JwtService jwt; JwtFilter(JwtService jwt){this.jwt=jwt;}
 @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws ServletException,IOException {String h=r.getHeader(HttpHeaders.AUTHORIZATION);if(h!=null&&h.startsWith("Bearer "))try{var c=jwt.accessClaims(h.substring(7));if("access".equals(c.get("typ",String.class))){var roles=((List<?>)c.get("roles")).stream().map(x->new SimpleGrantedAuthority("ROLE_"+x)).toList();var auth=new UsernamePasswordAuthenticationToken(c.getSubject(),null,roles);org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);}}catch(Exception ignored){} chain.doFilter(r,s);}
}
