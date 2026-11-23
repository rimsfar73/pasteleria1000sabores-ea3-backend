package cl.milsabores.api;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ConfiguracionProduccionTest {
  @Test void rechazaOpcionesDeDemostracion() {
    assertThrows(IllegalStateException.class,
        () -> new ConfiguracionProduccion("http://localhost:5173", true, false));
    assertThrows(IllegalStateException.class,
        () -> new ConfiguracionProduccion("https://pasteleria.example", false, false));
    assertThrows(IllegalStateException.class,
        () -> new ConfiguracionProduccion("https://pasteleria.example", true, true));
  }

  @Test void aceptaOpcionesSeguras() {
    assertDoesNotThrow(() -> new ConfiguracionProduccion("https://pasteleria.example", true, false));
  }
}
