package cl.milsabores.api.config;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name = "configuracion_comercial")
public class ConfiguracionComercial {
  @Id public String clave;
  public String valor;
}
