package co.edu.fcv.citas;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CitasApiApplication {
  /** Zona de negocio del laboratorio: fechas de citas, bloques y reglas de pasado/futuro. */
  public static final String BUSINESS_ZONE = "America/Bogota";

  static {
    // Se fija al cargar la clase para cubrir también los contextos de prueba (@SpringBootTest).
    TimeZone.setDefault(TimeZone.getTimeZone(BUSINESS_ZONE));
  }

  public static void main(String[] args) { SpringApplication.run(CitasApiApplication.class, args); }
}
