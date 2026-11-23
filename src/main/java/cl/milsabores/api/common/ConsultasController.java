package cl.milsabores.api.common;

import cl.milsabores.api.dtos.Dtos;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consultas")
public class ConsultasController {
  final ConsultaRepository repo;

  public ConsultasController(ConsultaRepository r) {
    repo = r;
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Map<String, String> crear(@Valid @RequestBody Dtos.ConsultaEntrada d) {
    Consulta c = new Consulta();
    c.id = UUID.randomUUID().toString();
    c.nombre = d.nombre();
    c.correo = d.correo();
    c.comentario = d.comentario();
    c.fecha = java.time.Instant.now();
    repo.save(c);
    return Map.of("id", c.id, "mensaje", "Consulta recibida.");
  }

  @GetMapping
  @PreAuthorize("hasRole('admin')")
  public List<Consulta> lista() {
    return repo.findAllByOrderByFechaDesc();
  }
}
