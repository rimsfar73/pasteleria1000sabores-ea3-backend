package cl.milsabores.api.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

// Los DTO definen qué acepta y devuelve la API. Nunca se devuelve claveHash.
public final class Dtos {
  public record Login(@NotBlank @Email String correo, @NotBlank String clave) {}

  public record UsuarioEntrada(
      @NotBlank @Size(max = 9) String run,
      @NotBlank @Size(max = 50) String nombre,
      @NotBlank @Size(max = 100) String apellidos,
      @NotBlank @Email @Size(max = 100) String correo,
      String clave,
      String confirmacion,
      @NotBlank @Size(max = 255) String direccion,
      @NotBlank String region,
      @NotBlank String comuna,
      String rol) {}

  public record UsuarioVista(
      String id,
      String run,
      String nombre,
      String apellidos,
      String correo,
      String direccion,
      String region,
      String comuna,
      String rol,
      boolean estudianteDuoc) {
    public static UsuarioVista de(Usuario u) {
      return new UsuarioVista(
          u.id, u.run, u.nombre, u.apellidos, u.correo, u.direccion, u.region, u.comuna, u.rol, u.estudianteDuoc);
    }
  }

  public record ProductoEntrada(
      @NotBlank @Pattern(regexp = "[A-Z0-9_-]{1,30}") String codigo,
      @NotBlank @Size(max = 150) String nombre,
      @NotBlank @Size(max = 2000) String descripcion,
      @NotBlank String categoria,
      @NotNull @DecimalMin("1") @Digits(integer = 10, fraction = 0) BigDecimal precio,
      @NotNull @Min(0) @Max(1000000) Integer stock,
      @Size(max = 255) String imagen,
      boolean disponible,
      boolean personalizable,
      boolean sinAzucar,
      boolean sinGluten,
      boolean vegano) {}

  public record ProductoVista(
      String codigo,
      String nombre,
      String descripcion,
      String categoria,
      BigDecimal precio,
      int stock,
      String imagen,
      boolean disponible,
      boolean personalizable, boolean sinAzucar, boolean sinGluten, boolean vegano) {
    public static ProductoVista de(Producto p) {
      return new ProductoVista(
          p.codigo,
          p.nombre,
          p.descripcion,
          p.categoria.nombre,
          p.precio,
          p.stock,
          p.imagen,
          p.disponible,
          p.personalizable,
          p.sinAzucar,
          p.sinGluten,
          p.vegano);
    }
  }

  public record CategoriaEntrada(@NotBlank @Size(max = 100) String nombre) {}

  public record Linea(@NotBlank String codigo, @Min(1) @Max(1000000) int cantidad) {}

  public record PedidoEntrada(
      @NotEmpty @Size(max = 100) List<@Valid Linea> items,
      @Size(max = 30) String cupon,
      @NotBlank @Size(max = 255) String direccion,
      @NotBlank String region,
      @NotBlank String comuna,
      @Size(max = 100) String nombreReceptor,
      @Email @Size(max = 150) String correoReceptor,
      @Pattern(regexp = "[+0-9 ]{8,20}") String telefono,
      @Size(max = 20) String numero,
      @Size(max = 500) String observaciones,
      BigDecimal totalEsperado,
      @Pattern(regexp = "APROBADO|RECHAZADO") String resultadoPago) {
    public PedidoEntrada(List<Linea> items, String cupon, String direccion, String region, String comuna) {
      this(items, cupon, direccion, region, comuna, null, null, null, null, null, null, null);
    }
    public PedidoEntrada(List<Linea> items, String cupon, String direccion, String region, String comuna,
        String nombreReceptor, String correoReceptor, String telefono, String numero,
        String observaciones, BigDecimal totalEsperado) {
      this(items, cupon, direccion, region, comuna, nombreReceptor, correoReceptor,
          telefono, numero, observaciones, totalEsperado, null);
    }
  }

  public record DetalleVista(
      String codigo, String nombre, int cantidad, BigDecimal precio, BigDecimal subtotal) {}

  public record PedidoVista(
      String id,
      String usuarioId,
      java.time.Instant fecha,
      String estado,
      String direccion,
      String region,
      String comuna,
      String cupon,
      BigDecimal subtotal,
      BigDecimal descuento,
      BigDecimal total,
      List<DetalleVista> items,
      BigDecimal totalNeto,
      BigDecimal totalIva,
      BigDecimal tasaIva,
      String nombreReceptor,
      String correoReceptor,
      String telefono,
      String numero,
      String observaciones) {}

  public record ConsultaEntrada(
      @NotBlank @Size(max = 100) String nombre,
      @Email @NotBlank @Size(max = 100) String correo,
      @NotBlank @Size(max = 500) String comentario) {}
}
