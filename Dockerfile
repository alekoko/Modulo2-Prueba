# Imagen reproducible: JDK 21 + Maven. Idéntica en Linux, Windows (Docker Desktop) y CI.
FROM maven:3.9.9-eclipse-temurin-21

WORKDIR /app

# 1) Capa de dependencias cache: solo se reconstruye si cambia el pom.xml
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 2) Código fuente
COPY src ./src

ENV TEST_ENV=qa \
    TZ=America/Mexico_City

# Tests -> Reporte Allure -> Envío de correo (perfil report-and-notify)
ENTRYPOINT ["mvn", "-B", "-Preport-and-notify", "verify"]
