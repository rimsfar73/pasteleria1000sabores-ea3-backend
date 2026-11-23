package cl.milsabores.api;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionComercialService {
  private final ConfiguracionComercialRepository repo;
  public ConfiguracionComercialService(ConfiguracionComercialRepository repo) { this.repo = repo; }

  @Transactional(readOnly = true)
  public BigDecimal numero(String clave) {
    String valor = repo.findById(clave)
        .orElseThrow(() -> new ApiException(500, "Falta el parámetro comercial " + clave)).valor;
    try { return new BigDecimal(valor); }
    catch (NumberFormatException e) { throw new ApiException(500, "Parámetro comercial inválido: " + clave); }
  }
}
