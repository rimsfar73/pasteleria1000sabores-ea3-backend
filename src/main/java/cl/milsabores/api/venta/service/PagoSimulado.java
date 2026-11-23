package cl.milsabores.api.venta.service;

import cl.milsabores.api.common.exception.ApiException;

/** Validación académica; no ejecuta pagos ni representa una pasarela real. */
public final class PagoSimulado {
  private PagoSimulado() {}

  public static void validar(String resultado, boolean confirmar) {
    String valor = resultado == null || resultado.isBlank() ? "APROBADO" : resultado;
    if (!"APROBADO".equals(valor) && !"RECHAZADO".equals(valor))
      throw new ApiException(400, "Resultado de simulación de pago inválido.");
    if (confirmar && "RECHAZADO".equals(valor))
      throw new ApiException(422, "Pago de demostración rechazado: no se creó pedido ni se descontó inventario.");
  }
}
