package cl.milsabores.api;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UsuariosController {
  final UsuariosService service;
  final UsuarioRepository repo;

  public UsuariosController(UsuariosService s, UsuarioRepository r) {
    service = s;
    repo = r;
  }

  @GetMapping("/usuarios")
  @PreAuthorize("hasRole('admin')")
  public List<Dtos.UsuarioVista> lista() {
    return repo.findByActivoTrueOrderByNombre().stream().map(Dtos.UsuarioVista::de).toList();
  }

  @GetMapping("/usuarios/{id}")
  @PreAuthorize("hasRole('admin')")
  public Dtos.UsuarioVista detalle(@PathVariable String id) {
    return Dtos.UsuarioVista.de(service.buscar(id));
  }

  @PostMapping("/usuarios")
  @PreAuthorize("hasRole('admin')")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Dtos.UsuarioVista crear(@Valid @RequestBody Dtos.UsuarioEntrada d) {
    return Dtos.UsuarioVista.de(service.guardar(null, d, true));
  }

  @PutMapping("/usuarios/{id}")
  @PreAuthorize("hasRole('admin')")
  public Dtos.UsuarioVista editar(
      @PathVariable String id, @Valid @RequestBody Dtos.UsuarioEntrada d) {
    return Dtos.UsuarioVista.de(service.guardar(id, d, true));
  }

  @DeleteMapping("/usuarios/{id}")
  @PreAuthorize("hasRole('admin')")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable String id, Authentication a) {
    service.eliminar(id, a.getName());
  }

  public record VerificacionDuoc(boolean estudiante, String correoInstitucional) {}

  @Operation(summary="Validar condición DUOC [ADMIN]", description="No disponible para auto-registro; el administrador debe verificar la condición.", security={@SecurityRequirement(name="bearerAuth")})
  @PutMapping("/usuarios/{id}/beneficio-duoc")
  @PreAuthorize("hasRole('admin')")
  public Dtos.UsuarioVista verificarDuoc(@PathVariable String id,
      @RequestBody VerificacionDuoc datos) {
    return Dtos.UsuarioVista.de(service.certificarDuoc(id, datos.estudiante(), datos.correoInstitucional()));
  }

  @PutMapping("/perfil")
  @PreAuthorize("hasRole('cliente')")
  public Dtos.UsuarioVista perfil(Authentication a, @Valid @RequestBody Dtos.UsuarioEntrada d) {
    return Dtos.UsuarioVista.de(service.guardar(a.getName(), d, false));
  }
}
