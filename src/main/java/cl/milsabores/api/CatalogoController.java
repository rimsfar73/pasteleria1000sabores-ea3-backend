package cl.milsabores.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CatalogoController {
  final CatalogoService service;
  final CategoriaRepository categorias;
  final ProductoRepository productos;
  final Ubicaciones ubicaciones;

  public CatalogoController(
      CatalogoService s, CategoriaRepository c, ProductoRepository p, Ubicaciones u) {
    service = s;
    categorias = c;
    productos = p;
    ubicaciones = u;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("estado", "ok");
  }

  @Operation(summary="Catálogo público de productos")
  @GetMapping("/productos")
  public List<Dtos.ProductoVista> lista() {
    return service.listar();
  }

  @Operation(summary="Detalle público de producto")
  @ApiResponses({@ApiResponse(responseCode="200"), @ApiResponse(responseCode="404")})
  @GetMapping("/productos/{codigo}")
  public Dtos.ProductoVista detalle(@PathVariable String codigo) {
    return Dtos.ProductoVista.de(service.buscar(codigo));
  }

  @Operation(summary="Crear producto [ADMIN]", security={@SecurityRequirement(name="bearerAuth")})
  @ApiResponses({@ApiResponse(responseCode="201"), @ApiResponse(responseCode="400"), @ApiResponse(responseCode="401"), @ApiResponse(responseCode="403"), @ApiResponse(responseCode="409")})
  @PostMapping("/productos")
  @PreAuthorize("hasRole('admin')")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Dtos.ProductoVista crear(@Valid @RequestBody Dtos.ProductoEntrada d) {
    return service.guardar(null, d);
  }

  @Operation(summary="Editar producto e historizar cambios de precio [ADMIN]", security={@SecurityRequirement(name="bearerAuth")})
  @PutMapping("/productos/{codigo}")
  @PreAuthorize("hasRole('admin')")
  public Dtos.ProductoVista editar(
      @PathVariable String codigo, @Valid @RequestBody Dtos.ProductoEntrada d) {
    return service.guardar(codigo, d);
  }

  @Operation(summary="Baja lógica de producto [ADMIN]", security={@SecurityRequirement(name="bearerAuth")})
  @DeleteMapping("/productos/{codigo}")
  @PreAuthorize("hasRole('admin')")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable String codigo) {
    service.eliminar(codigo);
  }

  @GetMapping("/categorias")
  public List<String> categorias() {
    return categorias.findAll().stream().map(c -> c.nombre).sorted().toList();
  }

  @PostMapping("/categorias")
  @PreAuthorize("hasRole('admin')")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Categoria categoria(@Valid @RequestBody Dtos.CategoriaEntrada d) {
    if (categorias.existsById(d.nombre())) throw new ApiException(409, "Categoría existente.");
    Categoria c = new Categoria();
    c.nombre = d.nombre();
    return categorias.save(c);
  }

  @DeleteMapping("/categorias/{nombre}")
  @PreAuthorize("hasRole('admin')")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void borrarCategoria(@PathVariable String nombre) {
    if (productos.existsByCategoriaNombre(nombre))
      throw new ApiException(409, "La categoría tiene productos asociados, incluidos los eliminados. Conserva su historial.");
    categorias.deleteById(nombre);
  }

  @GetMapping("/ubicaciones")
  public Map<String, List<String>> ubicaciones() {
    return ubicaciones.regiones;
  }
}
