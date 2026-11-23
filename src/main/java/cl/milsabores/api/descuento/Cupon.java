package cl.milsabores.api.descuento;

import jakarta.persistence.*;

@Entity
public class Cupon {
  @Id public String codigo;
  public String tipo;

  @Column(precision = 14, scale = 0)
  public java.math.BigDecimal valor;

  @Column(precision = 14, scale = 0)
  public java.math.BigDecimal minimo;

  public boolean activo = true;
  public boolean requiereDuoc = false;
}
