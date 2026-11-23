package cl.milsabores.api;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "producto_precio_hist")
public class ProductoPrecioHist {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional = false) @JoinColumn(name="producto_codigo") public Producto producto;
  @Column(precision=14,scale=0) public BigDecimal precioAnterior;
  @Column(precision=14,scale=0) public BigDecimal precioNuevo;
  public Instant fecha;
}
