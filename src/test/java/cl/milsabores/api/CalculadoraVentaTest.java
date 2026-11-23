package cl.milsabores.api;

import cl.milsabores.api.venta.service.CalculadoraVenta;
import cl.milsabores.api.common.exception.ApiException;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculadoraVentaTest {
  private static BigDecimal n(String n) { return new BigDecimal(n); }

  @Test void casoDefinitivoOracleConIvaIncluido() {
    var r=CalculadoraVenta.calcular(n("45000"),n("4500"),n("20000"),n("0.19"));
    assertEquals(n("40500"),r.totalBruto());
    assertEquals(n("34034"),r.totalNeto());
    assertEquals(n("6466"),r.totalIva());
    assertEquals(r.totalBruto(),r.totalNeto().add(r.totalIva()));
    assertEquals(r.subtotal(),r.totalBruto().add(r.descuento()));
  }
  @Test void limiteDescuentoConfigurado() {
    var r=CalculadoraVenta.calcular(n("200000"),n("30000"),n("20000"),n("0.19"));
    assertEquals(n("20000"),r.descuento());
    assertEquals(n("180000"),r.totalBruto());
  }
  @Test void descuentoNoSuperaPedido() {
    var r=CalculadoraVenta.calcular(n("5000"),n("6000"),n("20000"),n("0.19"));
    assertEquals(n("0"),r.totalBruto());
  }
  @Test void evitaConfiguracionTributariaInvalida() {
    assertThrows(ApiException.class, () -> CalculadoraVenta.calcular(n("1000"),n("0"),n("0"),n("1.99")));
  }
}
