package cl.milsabores.api;

import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuariosService {
  final UsuarioRepository repo;
  final PasswordEncoder passwords;
  final Ubicaciones ubicaciones;
  final SesionRefreshRepository sesiones;

  public UsuariosService(
      UsuarioRepository r, PasswordEncoder p, Ubicaciones u, SesionRefreshRepository s) {
    repo = r;
    passwords = p;
    ubicaciones = u;
    sesiones = s;
  }

  public Usuario buscar(String id) {
    return repo.findById(id).filter(u -> u.activo).orElseThrow(ApiException::noEncontrado);
  }

  public Usuario guardar(String id, Dtos.UsuarioEntrada d, boolean admin) {
    Usuario u = id == null ? new Usuario() : buscar(id);
    boolean nuevo = id == null;
    if (nuevo) u.id = UUID.randomUUID().toString();
    String run = d.run().trim().toUpperCase(), correo = d.correo().trim().toLowerCase();
    if (!nuevo && !admin && !run.equals(u.run))
      throw new ApiException(400, "El RUN no puede cambiar desde el perfil.");
    if (!runValido(run))
      throw new ApiException(400, "RUN inválido: usa dígito verificador, sin puntos ni guion.");
    if (!correo.matches("[^\s@]+@(gmail[.]com|duoc[.]cl|profesor[.]duoc[.]cl)")
        && !(!nuevo && correo.equals(u.correo) && correo.endsWith("@pasteleria.cl")))
      throw new ApiException(400, "Usa un dominio gmail.com, duoc.cl o profesor.duoc.cl.");
    if (repo.existsByRunAndIdNot(run, u.id) || repo.existsByCorreoAndIdNot(correo, u.id))
      throw new ApiException(409, "El correo o RUN ya está registrado.");
    ubicaciones.validar(d.region(), d.comuna());
    String rol = admin ? Objects.requireNonNullElse(d.rol(), "cliente") : nuevo ? "cliente" : u.rol;
    if (!Set.of("admin", "vendedor", "cliente").contains(rol))
      throw new ApiException(400, "Rol inválido.");
    if (!nuevo
        && u.rol.equals("admin")
        && !rol.equals("admin")
        && repo.bloquearAdministradores().size() <= 1)
      throw new ApiException(409, "Debe permanecer al menos un administrador activo.");
    boolean cambiaClave = d.clave() != null && !d.clave().isEmpty();
    if (!nuevo && (cambiaClave || !u.rol.equals(rol)))
      sesiones.findByUsuarioId(u.id).forEach(s -> s.revocada = true);
    if (nuevo || cambiaClave) {
      if (d.clave() == null
          || !d.clave().matches("(?=.*[A-Za-z])(?=.*[0-9])[A-Za-z0-9]{8,10}")
          || !d.clave().equals(d.confirmacion()))
        throw new ApiException(
            400,
            "La contraseña debe tener 8 a 10 letras y números, y coincidir con la confirmación.");
      u.claveHash = passwords.encode(d.clave());
      u.tokenVersion++;
    }
    if (!nuevo && !u.rol.equals(rol)) u.tokenVersion++;
    u.run = run;
    u.correo = correo;
    u.nombre = d.nombre().trim();
    u.apellidos = d.apellidos().trim();
    u.direccion = d.direccion().trim();
    u.region = d.region();
    u.comuna = d.comuna();
    u.rol = rol;
    return repo.save(u);
  }

  /** El registro público no puede autodeclarar beneficios de afiliación DUOC. */
  public Usuario certificarDuoc(String id, boolean estudiante, String correoInstitucional) {
    Usuario u = buscar(id);
    if (estudiante && (correoInstitucional == null ||
        !correoInstitucional.toLowerCase(java.util.Locale.ROOT).matches("[^\\s@]+@(?:duoc[.]cl|profesor[.]duoc[.]cl)")))
      throw new ApiException(400, "La verificación DUOC exige un correo institucional válido.");
    u.estudianteDuoc = estudiante;
    u.correoInstitucional = estudiante ? correoInstitucional.toLowerCase(java.util.Locale.ROOT) : null;
    return repo.save(u);
  }

  public void eliminar(String id, String actual) {
    Usuario u = buscar(id);
    if (id.equals(actual))
      throw new ApiException(409, "No puedes eliminar tu propia cuenta desde administración.");
    if (u.rol.equals("admin") && repo.bloquearAdministradores().size() <= 1)
      throw new ApiException(409, "Debe permanecer un administrador.");
    u.activo = false;
    u.tokenVersion++;
  }

  static boolean runValido(String run) {
    if (!run.matches("[0-9]{6,8}[0-9K]")) return false;
    int suma = 0, factor = 2;
    for (int i = run.length() - 2; i >= 0; i--) {
      suma += (run.charAt(i) - '0') * factor;
      factor = factor == 7 ? 2 : factor + 1;
    }
    int resto = 11 - suma % 11;
    return run.endsWith(resto == 11 ? "0" : resto == 10 ? "K" : String.valueOf(resto));
  }
}
