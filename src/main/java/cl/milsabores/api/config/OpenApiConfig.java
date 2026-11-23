package cl.milsabores.api.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI documentacion() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Pastelería Mil Sabores EA3")
                .version("v1")
                .description(
                    "API PostgreSQL. Precios CLP con IVA incluido. Para escrituras obtener"
                        + " /auth/csrf y enviar su token en X-XSRF-TOKEN; el navegador conserva"
                        + " cookies. JWT en Authorization: Bearer."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes(
                    "csrf",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-XSRF-TOKEN"))
                .addSecuritySchemes("refreshCookie", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("refresh")));
  }

  @Bean
  OpenApiCustomizer contratos() {
    return api -> api.getPaths().forEach((ruta, item) -> item.readOperationsMap().forEach((metodo, op) -> {
      boolean lectura = metodo == PathItem.HttpMethod.GET;
      String recurso = ruta.replace("/api/v1", "");
      boolean publico = Set.of("/auth/csrf", "/auth/login", "/auth/register", "/auth/refresh", "/auth/logout").contains(recurso)
          || (lectura && (recurso.startsWith("/productos") || Set.of("/categorias", "/ubicaciones", "/health").contains(recurso)))
          || (!lectura && recurso.equals("/consultas"));
      var seguridad = new SecurityRequirement();
      if (!publico) seguridad.addList("bearerAuth");
      if (!lectura) seguridad.addList("csrf");
      if (recurso.equals("/auth/refresh")) seguridad.addList("refreshCookie");
      op.setSecurity(seguridad.isEmpty() ? List.of() : List.of(seguridad));
      if (op.getSummary() == null) op.setSummary(metodo + " " + recurso);
      if (!lectura) {
        op.getResponses().addApiResponse("400", error("Datos inválidos. Revisa mensaje y errores."));
        op.getResponses().addApiResponse("403", error("Permisos insuficientes o CSRF inválido."));
        op.getResponses().addApiResponse("409", error("Conflicto de datos, stock o cotización. Revisa mensaje."));
      }
      if (!publico || recurso.equals("/auth/login") || recurso.equals("/auth/refresh"))
        op.getResponses().addApiResponse("401", error("Credenciales o sesión inválidas o vencidas."));
      if (!publico) op.getResponses().addApiResponse("403", error("No tienes permiso para esta operación."));
      if (recurso.contains("{")) op.getResponses().addApiResponse("404", error("Recurso no encontrado."));
      if (recurso.equals("/pedidos") && !lectura)
        op.setDescription("Solo cliente. Primero cotizar; enviar totalEsperado y una Idempotency-Key. "
            + "Si cambia el total calculado devuelve 409 sin descontar stock. "
            + "Ante una respuesta perdida repetir los mismos datos y clave; no crear otra compra.");
    }));
  }

  private ApiResponse error(String descripcion) {
    var esquema = new ObjectSchema()
        .addProperty("status", new IntegerSchema())
        .addProperty("mensaje", new StringSchema())
        .addProperty("errores", new ObjectSchema().additionalProperties(new StringSchema()));
    return new ApiResponse().description(descripcion).content(new Content()
        .addMediaType("application/json", new MediaType().schema(esquema)));
  }
}
