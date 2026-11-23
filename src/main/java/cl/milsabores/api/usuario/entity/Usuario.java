package cl.milsabores.api.usuario.entity;

import jakarta.persistence.*;

@Entity
public class Usuario {
  @Id public String id;

  @Column(unique = true, nullable = false)
  public String correo;

  @Column(unique = true, nullable = false)
  public String run;

  public String nombre;
  public String apellidos;
  public String direccion;
  public String region;
  public String comuna;
  public String claveHash;
  public String rol;
  @Column(name = "es_estudiante_duoc") public boolean estudianteDuoc;
  public String correoInstitucional;
  public boolean activo = true;
  public int tokenVersion = 0;
}
