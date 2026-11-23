
package cl.milsabores.api.auth.controller;

import cl.milsabores.api.auth.service.AuthService;
import cl.milsabores.api.dtos.Dtos;
import cl.milsabores.api.usuario.service.UsuariosService;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;
    private final UsuariosService usuarios;

    @Value("${app.cookie-secure}")
    private boolean secure;

    @Value("${app.refresh-days}")
    private long days;

    public AuthController(AuthService auth, UsuariosService usuarios) {
        this.auth = auth;
        this.usuarios = usuarios;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.UsuarioVista registro(
            @Valid @RequestBody Dtos.UsuarioEntrada d) {

        return Dtos.UsuarioVista.de(
                usuarios.guardar(null, d, false)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody Dtos.Login d,
            @CookieValue(name = "refresh", required = false)
            String anterior) {

        var resultado = auth.login(d);

        auth.salir(anterior);

        return respuesta(resultado);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refresh", required = false)
            String cookie) {

        return respuesta(auth.renovar(cookie));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refresh", required = false)
            String cookie) {

        auth.salir(cookie);

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie("", 0)
                )
                .build();
    }

    @GetMapping("/me")
    public Dtos.UsuarioVista me(Authentication authentication) {

        return Dtos.UsuarioVista.de(
                usuarios.buscar(authentication.getName())
        );
    }

    private ResponseEntity<?> respuesta(AuthService.Resultado resultado) {

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie(resultado.refresh(), days * 86400)
                )
                .body(
                        Map.of(
                                "accessToken", resultado.accessToken(),
                                "usuario", resultado.usuario()
                        )
                );
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
