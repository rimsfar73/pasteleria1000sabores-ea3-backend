package cl.milsabores.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;

// Pruebas reales contra PostgreSQL: autenticación, permisos, CRUD y transacciones.
@SpringBootTest(
    properties = {
      "app.seed-demo=true",
      "app.admin-password=PruebaSegura123",
      "app.key-directory=target/test-keys"
    })
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegracionTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ProductoRepository productos;
  @Autowired UsuarioRepository usuarios;
  @Autowired PedidoRepository pedidos;
  @Autowired TransactionTemplate tx;

  String token(String correo) throws Exception {
    return login(correo).path("accessToken").asText();
  }

  JsonNode login(String correo) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("correo", correo, "clave", "PruebaSegura123"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  String producto(String code, int stock) {
    return "{\"codigo\":\""
        + code
        + "\",\"nombre\":\"Prueba\",\"descripcion\":\"Torta de prueba\",\"categoria\":\"Tortas"
        + " Cuadradas\",\"precio\":10000,\"stock\":"
        + stock
        + ",\"imagen\":\"/img/tc001.jpg\",\"disponible\":true,\"personalizable\":false}";
  }

  void crear(String code, int stock) throws Exception {
    mvc.perform(
            post("/api/v1/productos")
                .with(csrf())
                .header("Authorization", "Bearer " + token("admin@pasteleria.cl"))
                .contentType("application/json")
                .content(producto(code, stock)))
        .andExpect(status().isCreated());
  }

  String orden(String code, int cantidad, String cupon) {
    return "{\"items\":[{\"codigo\":\""
        + code
        + "\",\"cantidad\":"
        + cantidad
        + "}],\"cupon\":\""
        + cupon
        + "\",\"direccion\":\"Calle de prueba 123\",\"region\":\"Metropolitana de"
        + " Santiago\",\"comuna\":\"Melipilla\"}";
  }

  final Map<String, String> totalesAceptados = new java.util.concurrent.ConcurrentHashMap<>();

  ResultActions comprar(String token, String data, String key) throws Exception {
    var solicitud = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(data);
    if (!solicitud.has("totalEsperado")) {
      if (!totalesAceptados.containsKey(key)) {
        var cotizacion = mvc.perform(post("/api/v1/pedidos/cotizar").with(csrf())
            .header("Authorization", "Bearer " + token).contentType("application/json").content(data))
            .andReturn().getResponse();
        if (cotizacion.getStatus() == 200) totalesAceptados.put(key,
            json.readTree(cotizacion.getContentAsString()).path("total").asText());
      }
      if (totalesAceptados.containsKey(key)) solicitud.put("totalEsperado", new java.math.BigDecimal(totalesAceptados.get(key)));
    }
    return confirmar(token, json.writeValueAsString(solicitud), key);
  }

  ResultActions confirmar(String token, String data, String key) throws Exception {
    return mvc.perform(
        post("/api/v1/pedidos")
            .with(csrf())
            .header("Authorization", "Bearer " + token)
            .header("Idempotency-Key", key)
            .contentType("application/json")
            .content(data));
  }

  @Test
  @Order(1)
  void catalogoPublicoYClaveNuncaExpuesta() throws Exception {
    mvc.perform(get("/api/v1/productos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].codigo").exists());
    var d = login("admin@pasteleria.cl");
    assertFalse(d.path("usuario").has("claveHash"));
    assertFalse(d.path("usuario").has("clave"));
  }

  @Test
  void credencialesIncorrectas() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content("{\"correo\":\"admin@pasteleria.cl\",\"clave\":\"incorrecta\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void csrfObligatorio() throws Exception {
    mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void privadoSinToken() throws Exception {
    mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
  }

  @Test
  void jwtAlterado() throws Exception {
    mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer invalid.token.value"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void corsSoloOrigenPermitido() throws Exception {
    mvc.perform(
            options("/api/v1/productos")
                .header("Origin", "https://intruso.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden());
    mvc.perform(
            options("/api/v1/productos")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
  }

  @Test
  void vendedorSoloLectura() throws Exception {
    String t = token("vendedor@gmail.com");
    mvc.perform(get("/api/v1/pedidos").header("Authorization", "Bearer " + t))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + t))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/productos")
                .with(csrf())
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content(producto("DENEGADO", 1)))
        .andExpect(status().isForbidden());
    comprar(t, orden("TC001", 1, ""), "vendedor-prueba").andExpect(status().isForbidden());
  }

  @Test
  void clienteNoAdmin() throws Exception {
    mvc.perform(
            get("/api/v1/usuarios").header("Authorization", "Bearer " + token("cliente@gmail.com")))
        .andExpect(status().isForbidden());
  }

  @Test
  void crudProductos() throws Exception {
    String c = "CRUD" + System.nanoTime();
    crear(c, 3);
    String t = token("admin@pasteleria.cl");
    mvc.perform(
            put("/api/v1/productos/" + c)
                .with(csrf())
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content(producto(c, 5)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(5));
    mvc.perform(
            delete("/api/v1/productos/" + c).with(csrf()).header("Authorization", "Bearer " + t))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/productos/" + c)).andExpect(status().isNotFound());
  }

  @Test
  void stockNegativoYDecimalRechazados() throws Exception {
    String t = token("admin@pasteleria.cl");
    for (String data :
        List.of(producto("NEG", -1), producto("DEC", 1).replace("\"stock\":1", "\"stock\":1.5")))
      mvc.perform(
              post("/api/v1/productos")
                  .with(csrf())
                  .header("Authorization", "Bearer " + t)
                  .contentType("application/json")
                  .content(data))
          .andExpect(status().isBadRequest());
  }

  @Test
  void simulacionPagoRechazadoNoCreaPedidoNiDescuentaStock() throws Exception {
    String codigo = "RECH" + System.nanoTime();
    crear(codigo, 4);
    String tokenCliente = token("cliente@gmail.com");
    String clave = UUID.randomUUID().toString();
    var cotizacion = mvc.perform(post("/api/v1/pedidos/cotizar").with(csrf())
        .header("Authorization", "Bearer " + tokenCliente)
        .contentType("application/json").content(orden(codigo, 1, "")))
        .andExpect(status().isOk()).andReturn().getResponse();
    var solicitud = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(orden(codigo, 1, ""));
    solicitud.put("totalEsperado", new java.math.BigDecimal(json.readTree(cotizacion.getContentAsString()).path("total").asText()));
    solicitud.put("resultadoPago", "RECHAZADO");
    confirmar(tokenCliente, json.writeValueAsString(solicitud), clave)
        .andExpect(status().isUnprocessableEntity());
    assertEquals(4, productos.findById(codigo).orElseThrow().stock);
    assertTrue(pedidos.findByUsuarioIdAndIdempotencia(usuarios.findByCorreo("cliente@gmail.com").orElseThrow().id, clave).isEmpty());
  }

  @Test
  void compraDescuentaStockYAplicaCuponUnaVez() throws Exception {
    String c = "BUY" + System.nanoTime();
    crear(c, 3);
    String t = token("cliente@gmail.com"),
        data = orden(c, 2, "PASTEL10"),
        key = UUID.randomUUID().toString();
    String first =
        comprar(t, data, key)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.total").value(18000))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String second =
        comprar(t, data, key)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(json.readTree(first).get("id"), json.readTree(second).get("id"));
    assertEquals(1, productos.findById(c).orElseThrow().stock);
    comprar(t, orden(c, 1, ""), key).andExpect(status().isConflict());
  }

  @Test
  void excesoNoAlteraStock() throws Exception {
    String c = "MAX" + System.nanoTime();
    crear(c, 1);
    comprar(token("cliente@gmail.com"), orden(c, 2, ""), UUID.randomUUID().toString())
        .andExpect(status().isConflict());
    assertEquals(1, productos.findById(c).orElseThrow().stock);
  }

  @Test
  void cuponInvalidoRevierteDescuentoStock() throws Exception {
    String c = "ROLL" + System.nanoTime();
    crear(c, 2);
    comprar(token("cliente@gmail.com"), orden(c, 1, "INVALIDO"), UUID.randomUUID().toString())
        .andExpect(status().isBadRequest());
    assertEquals(2, productos.findById(c).orElseThrow().stock);
  }

  @Test
  void noAceptaPrecioDelNavegador() throws Exception {
    String data = orden("TC001", 1, "").replace("\"cantidad\":1", "\"cantidad\":1,\"precio\":1");
    comprar(token("cliente@gmail.com"), data, UUID.randomUUID().toString())
        .andExpect(status().isBadRequest());
  }

  @Test
  void renovacionRotatoriaYLogoutRevocaJwt() throws Exception {
    var r =
        mvc.perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content("{\"correo\":\"cliente@gmail.com\",\"clave\":\"PruebaSegura123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var cookie = r.getCookie("refresh");
    assertNotNull(cookie);
    assertTrue(cookie.isHttpOnly());
    var renovada =
        mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(cookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(cookie))
        .andExpect(status().isUnauthorized());
    String t = json.readTree(renovada.getContentAsString()).path("accessToken").asText();
    mvc.perform(post("/api/v1/auth/logout").with(csrf()).cookie(renovada.getCookie("refresh")))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + t))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void consultaSePersiste() throws Exception {
    mvc.perform(
            post("/api/v1/consultas")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"nombre\":\"Prueba\",\"correo\":\"prueba@gmail.com\",\"comentario\":\"Consulta"
                        + " EA3\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            get("/api/v1/consultas")
                .header("Authorization", "Bearer " + token("admin@pasteleria.cl")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].comentario").exists());
  }

  @Test
  void runConDigitoVerificador() {
    assertTrue(UsuariosService.runValido("111111111"));
    assertFalse(UsuariosService.runValido("123456789"));
  }

  String runNuevo() {
    String cuerpo = String.valueOf(10000000 + Math.floorMod(System.nanoTime(), 80000000));
    int suma = 0, f = 2;
    for (int i = cuerpo.length() - 1; i >= 0; i--) {
      suma += (cuerpo.charAt(i) - '0') * f;
      f = f == 7 ? 2 : f + 1;
    }
    int n = 11 - suma % 11;
    return cuerpo + (n == 11 ? "0" : n == 10 ? "K" : String.valueOf(n));
  }

  Map<String, Object> datosUsuario(String rol) {
    String run = runNuevo();
    return new LinkedHashMap<>(
        Map.of(
            "run",
            run,
            "nombre",
            "Prueba",
            "apellidos",
            "Integracion",
            "correo",
            "u" + run + "@gmail.com",
            "clave",
            "Clave1234",
            "confirmacion",
            "Clave1234",
            "direccion",
            "Calle 123",
            "region",
            "Metropolitana de Santiago",
            "comuna",
            "Melipilla",
            "rol",
            rol));
  }

  JsonNode registrar(Map<String, Object> datos) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/v1/auth/register")
                    .with(csrf())
                    .contentType("application/json")
                    .content(json.writeValueAsString(datos)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  org.springframework.mock.web.MockHttpServletResponse loginNuevo(Map<String, Object> datos)
      throws Exception {
    return mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType("application/json")
                .content(
                    json.writeValueAsString(
                        Map.of("correo", datos.get("correo"), "clave", datos.get("clave")))))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();
  }

  @Test
  void registroNoPermiteEscalarRol() throws Exception {
    var datos = datosUsuario("admin");
    assertEquals("cliente", registrar(datos).path("rol").asText());
  }

  @Test
  void pedidoAjenoEsInaccesible() throws Exception {
    String c = "OWN" + System.nanoTime();
    crear(c, 2);
    String result =
        comprar(token("cliente@gmail.com"), orden(c, 1, ""), UUID.randomUUID().toString())
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.readTree(result).path("id").asText();
    var datos = datosUsuario("cliente");
    registrar(datos);
    String t = json.readTree(loginNuevo(datos).getContentAsString()).path("accessToken").asText();
    mvc.perform(get("/api/v1/pedidos/" + id).header("Authorization", "Bearer " + t))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/v1/pedidos/" + id)
                .header("Authorization", "Bearer " + token("vendedor@gmail.com")))
        .andExpect(status().isOk());
  }

  @Test
  void crudUsuariosYBajaRevocaAcceso() throws Exception {
    var d = datosUsuario("vendedor");
    String admin = token("admin@pasteleria.cl");
    var r =
        mvc.perform(
                post("/api/v1/usuarios")
                    .with(csrf())
                    .header("Authorization", "Bearer " + admin)
                    .contentType("application/json")
                    .content(json.writeValueAsString(d)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    String id = json.readTree(r.getContentAsString()).path("id").asText();
    String t = json.readTree(loginNuevo(d).getContentAsString()).path("accessToken").asText();
    d.put("nombre", "Actualizado");
    d.put("clave", "");
    d.put("confirmacion", "");
    mvc.perform(
            put("/api/v1/usuarios/" + id)
                .with(csrf())
                .header("Authorization", "Bearer " + admin)
                .contentType("application/json")
                .content(json.writeValueAsString(d)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Actualizado"));
    mvc.perform(
            delete("/api/v1/usuarios/" + id)
                .with(csrf())
                .header("Authorization", "Bearer " + admin))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + t))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void cambioClaveRevocaRefreshAnterior() throws Exception {
    var d = datosUsuario("cliente");
    registrar(d);
    var login = loginNuevo(d);
    String t = json.readTree(login.getContentAsString()).path("accessToken").asText();
    d.put("clave", "Nueva1234");
    d.put("confirmacion", "Nueva1234");
    mvc.perform(
            put("/api/v1/perfil")
                .with(csrf())
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content(json.writeValueAsString(d)))
        .andExpect(status().isOk());
    mvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(login.getCookie("refresh")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void perfilNoEscalaRol() throws Exception {
    var d = datosUsuario("cliente");
    registrar(d);
    String t = json.readTree(loginNuevo(d).getContentAsString()).path("accessToken").asText();
    d.put("rol", "admin");
    d.put("clave", "");
    d.put("confirmacion", "");
    mvc.perform(
            put("/api/v1/perfil")
                .with(csrf())
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content(json.writeValueAsString(d)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rol").value("cliente"));
  }

  @Test
  void compraConcurrenteNoSobrevende() throws Exception {
    String c = "CON" + System.nanoTime();
    crear(c, 1);
    String t = token("cliente@gmail.com");
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var inicio = new java.util.concurrent.CountDownLatch(1);
      java.util.concurrent.Callable<Integer> accion =
          () -> {
            inicio.await();
            return comprar(t, orden(c, 1, ""), UUID.randomUUID().toString())
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var a = executor.submit(accion);
      var b = executor.submit(accion);
      inicio.countDown();
      var resultados = new ArrayList<>(List.of(a.get(), b.get()));
      Collections.sort(resultados);
      assertEquals(List.of(201, 409), resultados);
      assertEquals(0, productos.findById(c).orElseThrow().stock);
    }
  }

  @Test
  void categoriasCrud() throws Exception {
    String t = token("admin@pasteleria.cl"), nombre = "Categoria " + System.nanoTime();
    mvc.perform(
            post("/api/v1/categorias")
                .with(csrf())
                .header("Authorization", "Bearer " + t)
                .contentType("application/json")
                .content(json.writeValueAsString(Map.of("nombre", nombre))))
        .andExpect(status().isCreated());
    mvc.perform(
            delete("/api/v1/categorias/" + nombre)
                .with(csrf())
                .header("Authorization", "Bearer " + t))
        .andExpect(status().isNoContent());
  }

  @Test
  void noEliminaPropioAdministrador() throws Exception {
    var r = login("admin@pasteleria.cl");
    mvc.perform(
            delete("/api/v1/usuarios/" + r.path("usuario").path("id").asText())
                .with(csrf())
                .header("Authorization", "Bearer " + r.path("accessToken").asText()))
        .andExpect(status().isConflict());
  }

  @Test
  void cotizacionDesglosaIvaSinAgregarloPorSegundaVez() throws Exception {
    String codigo = "IVA" + System.nanoTime();
    crear(codigo, 5);
    mvc.perform(post("/api/v1/pedidos/cotizar")
            .with(csrf())
            .header("Authorization", "Bearer " + token("cliente@gmail.com"))
            .contentType("application/json")
            .content(orden(codigo, 1, "PASTEL10")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subtotal").value(10000))
        .andExpect(jsonPath("$.descuento").value(1000))
        .andExpect(jsonPath("$.total").value(9000))
        .andExpect(jsonPath("$.totalNeto").value(7563))
        .andExpect(jsonPath("$.totalIva").value(1437));
    assertEquals(5, productos.findById(codigo).orElseThrow().stock);
  }

  @Test
  void totalEsperadoAntiguoImpideCompraSilenciosamenteRepreciada() throws Exception {
    String codigo = "REPRICE" + System.nanoTime();
    crear(codigo, 4);
    String solicitud = orden(codigo, 1, "").replace("\"comuna\":", "\"totalEsperado\":8000,\"comuna\":");
    comprar(token("cliente@gmail.com"), solicitud, UUID.randomUUID().toString())
        .andExpect(status().isConflict());
    assertEquals(4, productos.findById(codigo).orElseThrow().stock);
  }

  @Test
  void beneficioDuocEsExclusivoDeAdministrador() throws Exception {
    String codigo = "DUO" + System.nanoTime();
    crear(codigo, 3);
    var nuevo = datosUsuario("cliente");
    String id = registrar(nuevo).path("id").asText();
    String clienteToken = json.readTree(loginNuevo(nuevo).getContentAsString()).path("accessToken").asText();
    String adminToken = token("admin@pasteleria.cl");
    var solicitud = post("/api/v1/pedidos/cotizar").with(csrf())
        .header("Authorization", "Bearer " + clienteToken)
        .contentType("application/json").content(orden(codigo, 2, "DUOC15"));
    mvc.perform(solicitud).andExpect(status().isForbidden());
    mvc.perform(put("/api/v1/usuarios/" + id + "/beneficio-duoc")
            .with(csrf()).header("Authorization", "Bearer " + clienteToken)
            .contentType("application/json")
            .content("{\"estudiante\":true,\"correoInstitucional\":\"estudiante@duoc.cl\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(put("/api/v1/usuarios/" + id + "/beneficio-duoc")
            .with(csrf()).header("Authorization", "Bearer " + adminToken)
            .contentType("application/json")
            .content("{\"estudiante\":true,\"correoInstitucional\":\"estudiante@duoc.cl\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estudianteDuoc").value(true));
    mvc.perform(post("/api/v1/pedidos/cotizar").with(csrf())
            .header("Authorization", "Bearer " + clienteToken)
            .contentType("application/json").content(orden(codigo, 2, "DUOC15")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(17000));
  }

  @Test
  void categoriaConProductoEliminadoConservaHistorial() throws Exception {
    String codigo = "HIST" + System.nanoTime(), nombre = "Historial " + System.nanoTime();
    String t = token("admin@pasteleria.cl");
    mvc.perform(post("/api/v1/categorias").with(csrf()).header("Authorization", "Bearer " + t)
        .contentType("application/json").content(json.writeValueAsString(Map.of("nombre", nombre)))).andExpect(status().isCreated());
    mvc.perform(post("/api/v1/productos").with(csrf()).header("Authorization", "Bearer " + t)
        .contentType("application/json").content(producto(codigo, 1).replace("Tortas Cuadradas", nombre))).andExpect(status().isCreated());
    mvc.perform(delete("/api/v1/productos/" + codigo).with(csrf()).header("Authorization", "Bearer " + t)).andExpect(status().isNoContent());
    mvc.perform(delete("/api/v1/categorias/" + nombre).with(csrf()).header("Authorization", "Bearer " + t))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("historial")));
  }

  @Test
  void openApiDistinguePublicoPrivadoYDocumentaErrores() throws Exception {
    String texto = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var api = json.readTree(texto);
    var paths = api.path("paths");
    assertEquals(0, paths.path("/api/v1/productos").path("get").path("security").size());
    assertTrue(paths.path("/api/v1/usuarios").path("get").path("security").get(0).has("bearerAuth"));
    assertFalse(paths.path("/api/v1/auth/login").path("post").path("security").get(0).has("bearerAuth"));
    assertTrue(paths.path("/api/v1/pedidos").path("post").path("responses").has("409"));
    paths.forEach(path -> path.forEach(op -> assertFalse(op.path("summary").asText().isBlank())));
    java.nio.file.Files.createDirectories(java.nio.file.Path.of("target"));
    java.nio.file.Files.writeString(java.nio.file.Path.of("target/openapi.json"), texto);
  }


  @Test
  void faltaTotalAceptadoNoCreaPedido() throws Exception {
    String c = "SINPRECIO" + System.nanoTime(); crear(c, 2);
    confirmar(token("cliente@gmail.com"), orden(c, 1, ""), UUID.randomUUID().toString()).andExpect(status().isConflict());
    assertEquals(2, productos.findById(c).orElseThrow().stock);
  }

  @Test
  void precioCambiaDespuesDeCotizarYReintentoConservaCompra() throws Exception {
    String c = "CAMBIO" + System.nanoTime(); crear(c, 3);
    String t = token("cliente@gmail.com"), data = orden(c, 1, "");
    var body = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(data);
    body.put("totalEsperado", 10000);
    tx.executeWithoutResult(x -> { var p=productos.findById(c).orElseThrow(); p.precio=new java.math.BigDecimal("12000"); productos.save(p); });
    confirmar(t, json.writeValueAsString(body), UUID.randomUUID().toString()).andExpect(status().isConflict());
    assertEquals(3, productos.findById(c).orElseThrow().stock);
    body.put("totalEsperado", 12000);
    String key=UUID.randomUUID().toString(), aceptado=json.writeValueAsString(body);
    String first=confirmar(t,aceptado,key).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    tx.executeWithoutResult(x -> { var p=productos.findById(c).orElseThrow(); p.precio=new java.math.BigDecimal("14000"); productos.save(p); });
    String again=confirmar(t,aceptado,key).andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(12000)).andReturn().getResponse().getContentAsString();
    assertEquals(json.readTree(first).path("id"),json.readTree(again).path("id"));
    assertEquals(2,productos.findById(c).orElseThrow().stock);
  }

}
