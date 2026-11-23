package cl.milsabores.api.venta.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidosController {
  final PedidosService service;

  public PedidosController(PedidosService s) {
    service = s;
  }

  @Operation(summary="Listar pedidos", description="ADMIN y VENDEDOR ven órdenes; CLIENTE solo sus pedidos.", security={@SecurityRequirement(name="bearerAuth")})
  @GetMapping
  public List<Dtos.PedidoVista> lista(Authentication a) {
    return service.listar(a.getName(), gestion(a));
  }

  @Operation(summary="Detalle de pedido con control de propiedad", security={@SecurityRequirement(name="bearerAuth")})
  @ApiResponses({@ApiResponse(responseCode="200"), @ApiResponse(responseCode="401"), @ApiResponse(responseCode="403"), @ApiResponse(responseCode="404")})
  @GetMapping("/{id}")
  public Dtos.PedidoVista detalle(@PathVariable String id, Authentication a) {
    return service.detalle(id, a.getName(), gestion(a));
  }

  @Operation(summary="Confirmar pedido y descontar stock", description="CLIENTE. Requiere Idempotency-Key, CSRF y totalEsperado; 409 si cambió la cotización. resultadoPago RECHAZADO provoca 422 sin persistencia; solo simulación académica.", security={@SecurityRequirement(name="bearerAuth")})
  @ApiResponses({@ApiResponse(responseCode="201",description="Compra confirmada"), @ApiResponse(responseCode="400",description="Datos inválidos"), @ApiResponse(responseCode="401"), @ApiResponse(responseCode="403"), @ApiResponse(responseCode="404"), @ApiResponse(responseCode="409",description="Sin stock o cotización desactualizada"), @ApiResponse(responseCode="422",description="Pago simulado rechazado; no se confirma ni descuenta stock")})
  @PostMapping
  @PreAuthorize("hasRole('cliente')")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Dtos.PedidoVista crear(
      Authentication a,
      @Valid @RequestBody Dtos.PedidoEntrada d,
      @RequestHeader(value = "Idempotency-Key", required = false) String key) {
    return service.crear(a.getName(), d, key, true);
  }

  @Operation(summary="Cotizar sin descontar inventario", description="Calcula precio, tope de descuento y neto/IVA. Revalidar al confirmar.", security={@SecurityRequirement(name="bearerAuth")})
  @ApiResponses({@ApiResponse(responseCode="200"), @ApiResponse(responseCode="400"), @ApiResponse(responseCode="403"), @ApiResponse(responseCode="409")})
  @PostMapping("/cotizar")
  @PreAuthorize("hasRole('cliente')")
  public Dtos.PedidoVista cotizar(Authentication a, @Valid @RequestBody Dtos.PedidoEntrada d) {
    return service.crear(a.getName(), d, "cotizacion", false);
  }

  boolean gestion(Authentication a) {
    return a.getAuthorities().stream()
        .anyMatch(x -> Set.of("ROLE_admin", "ROLE_vendedor").contains(x.getAuthority()));
  }
}
