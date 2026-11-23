package cl.milsabores.api;

import jakarta.persistence.*;

@Entity
public class DetallePedido {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "pedido_id")
  public Pedido pedido;

  @ManyToOne(optional = false)
  @JoinColumn(name = "producto_codigo")
  public Producto producto;

  public String nombre;
  public int cantidad;

  @Column(precision = 14, scale = 0)
  public java.math.BigDecimal precio;
}
