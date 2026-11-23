package cl.milsabores.api.catalogo.entity;

import jakarta.persistence.*;

@Entity
public class Producto {
  @Id public String codigo;
  public String nombre;

  @Column(length = 2000)
  public String descripcion;

  @ManyToOne(optional = false)
  @JoinColumn(name = "categoria")
  public Categoria categoria;

  @Column(precision = 14, scale = 0)
  public java.math.BigDecimal precio;

  public int stock;
  public String imagen;
  public boolean disponible;
  public boolean personalizable;
  public boolean sinAzucar;
  public boolean sinGluten;
  public boolean vegano;
  public boolean eliminado = false;
}
