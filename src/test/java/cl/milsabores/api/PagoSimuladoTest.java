package cl.milsabores.api;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PagoSimuladoTest {
  @Test void porDefectoSeAceptaEnDemo() { assertDoesNotThrow(() -> PagoSimulado.validar(null, true)); }
  @Test void cotizarRechazadoNoBloqueaCalculo() { assertDoesNotThrow(() -> PagoSimulado.validar("RECHAZADO", false)); }
  @Test void confirmarRechazadoDevuelve422() {
    ApiException e = assertThrows(ApiException.class, () -> PagoSimulado.validar("RECHAZADO", true));
    assertEquals(422, e.status);
  }
  @Test void resultadoDesconocidoDevuelve400() {
    ApiException e = assertThrows(ApiException.class, () -> PagoSimulado.validar("AUTORIZADO", true));
    assertEquals(400, e.status);
  }
  @Test void aprobadoNoFalla() { assertDoesNotThrow(() -> PagoSimulado.validar("APROBADO", true)); }
}
