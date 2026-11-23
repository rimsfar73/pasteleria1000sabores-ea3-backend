package cl.milsabores.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Semillas implements CommandLineRunner {
  final CategoriaRepository categorias;
  final ProductoRepository productos;
  final UsuarioRepository usuarios;
  final CuponRepository cupones;
  final PasswordEncoder passwords;
  final ObjectMapper mapper;

  @Value("${app.seed-demo}")
  boolean demo;

  @Value("${app.admin-password}")
  String clave;

  public Semillas(
      CategoriaRepository c,
      ProductoRepository p,
      UsuarioRepository u,
      CuponRepository co,
      PasswordEncoder pw,
      ObjectMapper m) {
    categorias = c;
    productos = p;
    usuarios = u;
    cupones = co;
    passwords = pw;
    mapper = m;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    try (var in = getClass().getResourceAsStream("/productos.json")) {
      for (var d : mapper.readValue(in, Dtos.ProductoEntrada[].class)) {
        Categoria c =
            categorias
                .findById(d.categoria())
                .orElseGet(
                    () -> {
                      Categoria n = new Categoria();
                      n.nombre = d.categoria();
                      return categorias.save(n);
                    });
        if (!productos.existsById(d.codigo())) {
          Producto p = new Producto();
          p.codigo = d.codigo();
          p.nombre = d.nombre();
          p.descripcion = d.descripcion();
          p.categoria = c;
          p.precio = d.precio();
          p.stock = demo ? d.stock() : 0;
          p.imagen = d.imagen();
          p.disponible = d.disponible();
          p.personalizable = d.personalizable();
          p.sinAzucar = d.sinAzucar() || d.categoria().equals("Productos Sin Azúcar");
          p.sinGluten = d.sinGluten() || d.categoria().equals("Productos Sin Gluten");
          p.vegano = d.vegano() || d.categoria().equals("Productos Veganos");
          productos.save(p);
        }
      }
    }
    if (usuarios.count() == 0) {
      if (clave.length() < 12)
        throw new IllegalStateException(
            "Define ADMIN_PASSWORD con al menos 12 caracteres para crear el administrador"
                + " inicial.");
      crear("admin@pasteleria.cl", "admin", "111111111", clave);
      if (demo) {
        crear("cliente@gmail.com", "cliente", "222222228", clave);
        crear("vendedor@gmail.com", "vendedor", "333333335", clave);
      }
    }
    for (String[] d :
        new String[][] {
          {"PASTEL10", "porcentaje", "10", "0"},
          {"DUOC15", "porcentaje", "15", "20000"},
          {"MIL5000", "fijo", "5000", "30000"}
        })
      if (!cupones.existsById(d[0])) {
        Cupon c = new Cupon();
        c.codigo = d[0];
        c.tipo = d[1];
        c.valor = new java.math.BigDecimal(d[2]);
        c.minimo = new java.math.BigDecimal(d[3]);
        c.requiereDuoc = d[0].equals("DUOC15");
        cupones.save(c);
      }
  }

  void crear(String correo, String rol, String run, String clave) {
    Usuario u = new Usuario();
    u.id = UUID.randomUUID().toString();
    u.correo = correo;
    u.rol = rol;
    u.run = run;
    u.nombre = rol;
    u.apellidos = "Demostración";
    u.direccion = "Dirección de demostración";
    u.region = "Metropolitana de Santiago";
    u.comuna = "Melipilla";
    u.claveHash = passwords.encode(clave);
    usuarios.save(u);
  }
}
