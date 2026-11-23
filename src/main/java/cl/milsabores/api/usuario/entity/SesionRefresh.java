package cl.milsabores.api.usuario.entity;

import jakarta.persistence.*;

@Entity
public class SesionRefresh {
  @Id public String hash;

  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id")
  public Usuario usuario;

  public java.time.Instant vence;
  public boolean revocada = false;
}
