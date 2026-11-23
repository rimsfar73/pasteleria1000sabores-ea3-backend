package cl.milsabores.api.auth.controller;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  final AuthService auth;
  final UsuariosService usuarios;

  @Value("${app.cookie-secure}")
  boolean secure;

  @Value("${app.refresh-days}")
  long days;

  public AuthController(AuthService a, UsuariosService u) {
    auth = a;
    usuarios = u;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken());
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Dtos.UsuarioVista registro(@Valid @RequestBody Dtos.UsuarioEntrada d) {
    return Dtos.UsuarioVista.de(usuarios.guardar(null, d, false));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(
      @Valid @RequestBody Dtos.Login d,
      @CookieValue(name = "refresh", required = false) String anterior) {
    var r = auth.login(d);
    auth.salir(anterior);
    return respuesta(r);
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@CookieValue(name = "refresh", required = false) String cookie) {
    return respuesta(auth.renovar(cookie));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(@CookieValue(name = "refresh", required = false) String cookie) {
    auth.salir(cookie);
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0)).build();
  }

  @GetMapping("/me")
  public Dtos.UsuarioVista me(Authentication a) {
    return Dtos.UsuarioVista.de(usuarios.buscar(a.getName()));
  }

  private ResponseEntity<?> respuesta(AuthService.Resultado r) {
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie(r.refresh(), days * 86400))
        .body(Map.of("accessToken", r.accessToken(), "usuario", r.usuario()));
  }

  private String cookie(String token, long maxAge) {
    return ResponseCookie.from("refresh", token)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Strict")
        .path("/api/v1/auth")
        .maxAge(maxAge)
        .build()
        .toString();
  }
}
