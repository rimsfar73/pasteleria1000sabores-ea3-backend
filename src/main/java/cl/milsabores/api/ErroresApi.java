package cl.milsabores.api;

import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ErroresApi {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<?> negocio(ApiException e) {
    return respuesta(e.status, e.getMessage(), Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validacion(MethodArgumentNotValidException e) {
    Map<String, String> campos = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(x -> campos.put(x.getField(), x.getDefaultMessage()));
    return respuesta(400, "Revisa los datos enviados.", campos);
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> formato(Exception e) {
    return respuesta(400, "Formato de solicitud incorrecto.", Map.of());
  }

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  ResponseEntity<?> duplicado(Exception e) {
    return respuesta(409, "El registro ya existe o tiene registros asociados.", Map.of());
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<?> prohibido(Exception e) {
    return respuesta(403, "No tienes permiso para esta operación.", Map.of());
  }

  static ResponseEntity<?> respuesta(int status, String mensaje, Map<String, String> errores) {
    return ResponseEntity.status(status)
        .body(Map.of("status", status, "mensaje", mensaje, "errores", errores));
  }
}
