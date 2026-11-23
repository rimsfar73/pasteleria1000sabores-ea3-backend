package cl.milsabores.api.usuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
  java.util.Optional<Usuario> findByCorreo(String correo);

  boolean existsByRunAndIdNot(String run, String id);

  boolean existsByCorreoAndIdNot(String correo, String id);

  java.util.List<Usuario> findByActivoTrueOrderByNombre();

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id=:id")
  java.util.Optional<Usuario> bloquear(
      @org.springframework.data.repository.query.Param("id") String id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select u from Usuario u where u.rol='admin' and u.activo=true order by u.id")
  java.util.List<Usuario> bloquearAdministradores();
}
