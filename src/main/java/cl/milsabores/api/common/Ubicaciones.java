package cl.milsabores.api.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class Ubicaciones {
  public final Map<String, List<String>> regiones;

  public Ubicaciones(ObjectMapper mapper) throws Exception {
    try (var in = getClass().getResourceAsStream("/regiones.json")) {
      regiones = mapper.readValue(in, new TypeReference<>() {});
    }
  }

  public void validar(String region, String comuna) {
    if (!regiones.getOrDefault(region, List.of()).contains(comuna))
      throw new ApiException(400, "Selecciona una región y comuna válidas.");
  }
}
