package cl.milsabores.api.venta.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, String> {
  java.util.Optional<Pedido> findByUsuarioIdAndIdempotencia(String usuarioId, String idempotencia);

  java.util.List<Pedido> findAllByOrderByFechaDesc();

  java.util.List<Pedido> findByUsuarioIdOrderByFechaDesc(String usuarioId);
}
