#!/usr/bin/env bash
set -e

if [[ -z "${TEST_DB_PASSWORD:-}" ]]; then
  echo "ERROR: TEST_DB_PASSWORD no está configurada."
  exit 1
fi

echo "Iniciando Pasteleria EA3..."
echo "Base: pasteleria"
echo "Usuario: pasteleria_app"

DB_URL="jdbc:postgresql://localhost:5432/pasteleria" \
DB_USER="pasteleria_app" \
DB_PASSWORD="$TEST_DB_PASSWORD" \
mvn spring-boot:run
