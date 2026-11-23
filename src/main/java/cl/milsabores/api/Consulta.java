package cl.milsabores.api;

import jakarta.persistence.*;

@Entity
public class Consulta {
  @Id public String id;
  public String nombre;
  public String correo;

  @Column(length = 500)
  public String comentario;

  public java.time.Instant fecha;
}
