package cl.milsabores.api.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, String> {
  java.util.List<Producto> findByEliminadoFalseOrderByCodigo();

  boolean existsByCategoriaNombreAndEliminadoFalse(String nombre);

  boolean existsByCategoriaNombre(String nombre);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select p from Producto p where p.codigo=:codigo")
  java.util.Optional<Producto> bloquear(
      @org.springframework.data.repository.query.Param("codigo") String codigo);
}
