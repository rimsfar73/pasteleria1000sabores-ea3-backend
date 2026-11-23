package cl.milsabores.api.venta.service;

import java.math.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PedidosService {
  final PedidoRepository pedidos;
  final ProductoRepository productos;
  final UsuarioRepository usuarios;
  final CuponRepository cupones;
  final Ubicaciones ubicaciones;
  final ConfiguracionComercialService config;
  final AuditoriaPedidoRepository auditoria;

  public PedidosService(
      PedidoRepository p,
      ProductoRepository pr,
      UsuarioRepository u,
      CuponRepository c,
      Ubicaciones ub,
      ConfiguracionComercialService cfg,
      AuditoriaPedidoRepository aud) {
    pedidos = p;
    productos = pr;
    usuarios = u;
    cupones = c;
    ubicaciones = ub;
    config = cfg;
    auditoria = aud;
  }

  // El cliente envía códigos/cantidades. Precio, descuento e inventario se deciden aquí.
  public Dtos.PedidoVista crear(
      String usuarioId, Dtos.PedidoEntrada d, String key, boolean confirmar) {
    if (key == null || !key.matches("[A-Za-z0-9_-]{8,100}"))
      throw new ApiException(400, "Falta una clave de idempotencia válida.");
    Usuario usuario =
        usuarios
            .bloquear(usuarioId)
            .filter(u -> u.activo && u.rol.equals("cliente"))
            .orElseThrow(() -> new ApiException(403, "Solo clientes pueden comprar."));
    String huella = AuthService.hash(d.toString());
    if (confirmar) {
      var anterior = pedidos.findByUsuarioIdAndIdempotencia(usuarioId, key);
      if (anterior.isPresent()) {
        if (!anterior.get().huella.equals(huella))
          throw new ApiException(409, "La clave ya se usó para otra solicitud.");
        return vista(anterior.get());
      }
    }
    // La simulación de rechazo se valida en el servidor ANTES de tocar stock.
    // Su propósito es exclusivamente didáctico: no sustituye una pasarela real.
    PagoSimulado.validar(d.resultadoPago(), confirmar);
    ubicaciones.validar(d.region(), d.comuna());
    Pedido p = new Pedido();
    p.id = UUID.randomUUID().toString();
    p.usuario = usuario;
    p.idempotencia = key;
    p.huella = huella;
    p.fecha = java.time.Instant.now();
    p.estado = "CONFIRMADO";
    p.direccion = d.direccion();
    p.region = d.region();
    p.comuna = d.comuna();
    p.nombreReceptor = d.nombreReceptor();
    p.correoReceptor = d.correoReceptor();
    p.telefono = d.telefono();
    p.numero = d.numero();
    p.observaciones = d.observaciones();
    p.subtotal = BigDecimal.ZERO;
    Set<String> vistos = new HashSet<>();
    // Orden fijo de bloqueo evita interbloqueos entre compras simultáneas.
    for (var item : d.items().stream().sorted(Comparator.comparing(Dtos.Linea::codigo)).toList()) {
      if (!vistos.add(item.codigo()))
        throw new ApiException(400, "No repitas códigos en el pedido.");
      Producto producto =
          productos
              .bloquear(item.codigo())
              .filter(x -> !x.eliminado)
              .orElseThrow(ApiException::noEncontrado);
      if (!producto.disponible || producto.stock < item.cantidad())
        throw new ApiException(409, "Stock insuficiente para " + producto.nombre + ".");
      DetallePedido detalle = new DetallePedido();
      detalle.pedido = p;
      detalle.producto = producto;
      detalle.nombre = producto.nombre;
      detalle.cantidad = item.cantidad();
      detalle.precio = producto.precio;
      p.detalles.add(detalle);
      p.subtotal = p.subtotal.add(producto.precio.multiply(BigDecimal.valueOf(item.cantidad())));
      if (confirmar) producto.stock -= item.cantidad();
    }
    p.cupon = Objects.requireNonNullElse(d.cupon(), "").trim().toUpperCase();
    p.descuento = BigDecimal.ZERO;
    if (!p.cupon.isEmpty()) {
      Cupon c =
          cupones
              .findById(p.cupon)
              .filter(x -> x.activo)
              .orElseThrow(() -> new ApiException(400, "Cupón no válido."));
      if (c.requiereDuoc && !usuario.estudianteDuoc)
        throw new ApiException(403, "Cupón reservado para clientes DUOC verificados.");
      if (p.subtotal.compareTo(c.minimo) < 0)
        throw new ApiException(400, "No se cumple el mínimo del cupón.");
      p.descuento =
          c.tipo.equals("porcentaje")
              ? p.subtotal
                  .multiply(c.valor)
                  .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
              : c.valor.min(p.subtotal);
    }
    var valores = CalculadoraVenta.calcular(
        p.subtotal, p.descuento, config.numero("LIMITE_DESCUENTO"), config.numero("IVA"));
    p.subtotal = valores.subtotal();
    p.descuento = valores.descuento();
    p.total = valores.totalBruto();
    p.totalNeto = valores.totalNeto();
    p.totalIva = valores.totalIva();
    p.tasaIva = valores.tasaIva();
    if (confirmar && (d.totalEsperado() == null || p.total.compareTo(d.totalEsperado()) != 0))
      throw new ApiException(409, "Falta el total aceptado o cambió desde la cotización. Vuelve a cotizar antes de confirmar.");
    if (confirmar) {
      pedidos.save(p);
      AuditoriaPedido evento = new AuditoriaPedido();
      evento.pedido = p;
      evento.fecha = java.time.Instant.now();
      evento.usuarioId = usuarioId;
      evento.evento = "CREACION";
      evento.detalle = "Pedido confirmado, inventario validado en transacción";
      auditoria.save(evento);
    }
    return vista(p);
  }

  public List<Dtos.PedidoVista> listar(String id, boolean gestion) {
    return (gestion
            ? pedidos.findAllByOrderByFechaDesc()
            : pedidos.findByUsuarioIdOrderByFechaDesc(id))
        .stream().map(this::vista).toList();
  }

  public Dtos.PedidoVista detalle(String pedidoId, String usuarioId, boolean gestion) {
    var p = pedidos.findById(pedidoId).orElseThrow(ApiException::noEncontrado);
    if (!gestion && !p.usuario.id.equals(usuarioId))
      throw new ApiException(403, "El pedido pertenece a otro usuario.");
    return vista(p);
  }

  Dtos.PedidoVista vista(Pedido p) {
    return new Dtos.PedidoVista(
        p.id,
        p.usuario.id,
        p.fecha,
        p.estado,
        p.direccion,
        p.region,
        p.comuna,
        p.cupon,
        p.subtotal,
        p.descuento,
        p.total,
        p.detalles.stream()
            .map(
                d ->
                    new Dtos.DetalleVista(
                        d.producto.codigo,
                        d.nombre,
                        d.cantidad,
                        d.precio,
                        d.precio.multiply(BigDecimal.valueOf(d.cantidad))))
            .toList(),
        p.totalNeto, p.totalIva, p.tasaIva, p.nombreReceptor,
        p.correoReceptor, p.telefono, p.numero, p.observaciones);
  }
}
