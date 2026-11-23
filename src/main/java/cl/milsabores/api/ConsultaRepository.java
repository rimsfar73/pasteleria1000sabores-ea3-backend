package cl.milsabores.api;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultaRepository extends JpaRepository<Consulta, String> {
  java.util.List<Consulta> findAllByOrderByFechaDesc();
}
