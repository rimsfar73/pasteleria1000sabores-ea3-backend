package cl.milsabores.api;

public class ApiException extends RuntimeException {
  public final int status;

  public ApiException(int status, String mensaje) {
    super(mensaje);
    this.status = status;
  }

  public static ApiException noEncontrado() {
    return new ApiException(404, "No se encontró el registro solicitado.");
  }
}
