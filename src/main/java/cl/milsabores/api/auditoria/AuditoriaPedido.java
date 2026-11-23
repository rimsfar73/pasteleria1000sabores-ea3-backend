package cl.milsabores.api.auditoria;

import cl.milsabores.api.venta.entity.Pedido;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "auditoria_pedido")
public class AuditoriaPedido {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional = false) @JoinColumn(name="pedido_id") public Pedido pedido;
  public Instant fecha;
  public String evento;
  public String usuarioId;
  public String detalle;
}
