package cl.milsabores.api.venta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Traduce la regla comercial DEFINITIVA de Taller BD: precio al público con IVA incluido. */
public final class CalculadoraVenta {
  private CalculadoraVenta() {}

  public record Totales(BigDecimal subtotal, BigDecimal descuento,
      BigDecimal totalNeto, BigDecimal totalIva, BigDecimal totalBruto, BigDecimal tasaIva) {}

  public static Totales calcular(BigDecimal subtotal, BigDecimal descuentoSolicitado,
      BigDecimal limiteDescuento, BigDecimal tasaIva) {
    if (subtotal == null || subtotal.signum() <= 0)
      throw new ApiException(400, "Pedido sin detalles válidos.");
    if (descuentoSolicitado == null || descuentoSolicitado.signum() < 0 ||
        limiteDescuento == null || limiteDescuento.signum() < 0)
      throw new ApiException(400, "Configuración de descuento inválida.");
    if (tasaIva == null || tasaIva.signum() < 0 || tasaIva.compareTo(BigDecimal.ONE) > 0)
      throw new ApiException(500, "Configuración de IVA inválida.");
    BigDecimal base = subtotal.setScale(0, RoundingMode.HALF_UP);
    BigDecimal desc = descuentoSolicitado.min(limiteDescuento).min(base)
        .setScale(0, RoundingMode.HALF_UP);
    BigDecimal brutoFinal = base.subtract(desc);
    BigDecimal neto = brutoFinal.divide(BigDecimal.ONE.add(tasaIva), 0, RoundingMode.HALF_UP);
    return new Totales(base, desc, neto, brutoFinal.subtract(neto), brutoFinal, tasaIva);
  }
}
