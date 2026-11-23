package cl.milsabores.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Impide arrancar el perfil productivo con opciones propias de la demostración.
@Component
@Profile("prod")
public class ConfiguracionProduccion {
  public ConfiguracionProduccion(
      @Value("${app.origin}") String origen,
      @Value("${app.cookie-secure}") boolean cookieSegura,
      @Value("${app.seed-demo}") boolean demo) {
    if (!origen.startsWith("https://") || !cookieSegura || demo)
      throw new IllegalStateException(
          "Producción exige origen HTTPS, cookie Secure y SEED_DEMO desactivado.");
  }
}
