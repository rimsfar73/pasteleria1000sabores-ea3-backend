package cl.milsabores.api.catalogo.service;

import cl.milsabores.api.common.exception.ApiException;
import cl.milsabores.api.catalogo.repository.CategoriaRepository;
import cl.milsabores.api.dtos.Dtos;
import cl.milsabores.api.catalogo.entity.Producto;
import cl.milsabores.api.catalogo.entity.ProductoPrecioHist;
import cl.milsabores.api.catalogo.repository.ProductoPrecioHistRepository;
import cl.milsabores.api.catalogo.repository.ProductoRepository;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogoService {
  final ProductoRepository productos;
  final CategoriaRepository categorias;
  final ProductoPrecioHistRepository historial;

  public CatalogoService(ProductoRepository p, CategoriaRepository c,
      ProductoPrecioHistRepository h) {
    productos = p;
    categorias = c;
    historial = h;
  }

  public List<Dtos.ProductoVista> listar() {
    return productos.findByEliminadoFalseOrderByCodigo().stream()
        .map(Dtos.ProductoVista::de)
        .toList();
  }

  public Producto buscar(String codigo) {
    return productos
        .findById(codigo)
        .filter(p -> !p.eliminado)
        .orElseThrow(ApiException::noEncontrado);
  }

  public Dtos.ProductoVista guardar(String codigo, Dtos.ProductoEntrada d) {
    if (codigo == null && productos.existsById(d.codigo()))
      throw new ApiException(409, "Código ya registrado.");
    if (codigo != null && !codigo.equals(d.codigo()))
      throw new ApiException(400, "El código del producto no puede cambiar.");
    Producto p =
        codigo == null
            ? new Producto()
            : productos
                .bloquear(codigo)
                .filter(x -> !x.eliminado)
                .orElseThrow(ApiException::noEncontrado);
    if (d.imagen() != null
        && !d.imagen().isEmpty()
        && !d.imagen().matches("(/img/[^.].*|https?://.+)"))
      throw new ApiException(400, "Ruta de imagen inválida.");
    java.math.BigDecimal precioAnterior = codigo == null ? null : p.precio;
    p.codigo = d.codigo();
    p.nombre = d.nombre();
    p.descripcion = d.descripcion();
    p.categoria =
        categorias
            .findById(d.categoria())
            .orElseThrow(() -> new ApiException(400, "Categoría inválida."));
    p.precio = d.precio();
    p.stock = d.stock();
    p.imagen = d.imagen();
    p.disponible = d.disponible();
    p.personalizable = d.personalizable();
    p.sinAzucar = d.sinAzucar();
    p.sinGluten = d.sinGluten();
    p.vegano = d.vegano();
    Producto guardado = productos.save(p);
    if (precioAnterior != null && precioAnterior.compareTo(d.precio()) != 0) {
      ProductoPrecioHist h = new ProductoPrecioHist();
      h.producto = guardado;
      h.precioAnterior = precioAnterior;
      h.precioNuevo = d.precio();
      h.fecha = java.time.Instant.now();
      historial.save(h);
    }
    return Dtos.ProductoVista.de(guardado);
  }

  public void eliminar(String codigo) {
    var p = productos.bloquear(codigo).orElseThrow(ApiException::noEncontrado);
    p.eliminado = true;
    p.disponible = false;
  }
}
