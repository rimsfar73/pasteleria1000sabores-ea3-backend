package cl.milsabores.api;

import jakarta.persistence.*;

@Entity
public class Pedido {
  @Id public String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id")
  public Usuario usuario;

  public java.time.Instant fecha;
  public String estado;
  public String idempotencia;
  public String huella;
  public String direccion;
  public String region;
  public String comuna;
  public String cupon;
  public String nombreReceptor;
  public String correoReceptor;
  public String telefono;
  public String numero;
  public String observaciones;

  @Column(precision = 14, scale = 0) public java.math.BigDecimal totalNeto;
  @Column(precision = 14, scale = 0) public java.math.BigDecimal totalIva;
  @Column(precision = 5, scale = 4) public java.math.BigDecimal tasaIva;

  @Column(precision = 14, scale = 0)
  public java.math.BigDecimal subtotal;

  @Column(precision = 14, scale = 0)
  public java.math.BigDecimal descuento;

  @Column(precision = 14, scale = 0)
  public java.math.BigDecimal total;

  @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
  public java.util.List<DetallePedido> detalles = new java.util.ArrayList<>();
}
