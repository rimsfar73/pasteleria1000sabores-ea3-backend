> **Versión integrada EA3 v4 (candidata).** Este archivo proviene del ZIP de Chris y puede contener instrucciones anteriores. La guía vigente está en `../docs/ARRANQUE-V4.md`; la trazabilidad en `../docs/TRAZABILIDAD-ORACLE-EA2-JAVA-V4.md`. Los tests nuevos requieren ejecución independiente.

---

# Backend EA3 — Pastelería Mil Sabores

Java 21, Spring Boot, Spring Security, JPA, PostgreSQL, Flyway y Swagger.
Proyecto independiente: no necesita importar ni compilar el frontend.

Configurar variables indicadas en `.env.example` dentro de la terminal.

```powershell
.\mvnw.cmd spring-boot:run
```

Swagger: http://localhost:8080/swagger-ui.html

Pruebas: `.\mvnw.cmd test`, con TEST_DB_URL, TEST_DB_USER y TEST_DB_PASSWORD apuntando a una base exclusiva de pruebas. Nunca usar datos reales. Reporte JaCoCo: `target/site/jacoco/index.html`.

Documentación completa e iniciador automático en la carpeta superior.
