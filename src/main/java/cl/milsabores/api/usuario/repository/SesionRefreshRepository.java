package cl.milsabores.api.usuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionRefreshRepository extends JpaRepository<SesionRefresh, String> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select s from SesionRefresh s where s.hash=:hash")
  java.util.Optional<SesionRefresh> bloquear(
      @org.springframework.data.repository.query.Param("hash") String hash);

  java.util.List<SesionRefresh> findByUsuarioId(String id);
}
