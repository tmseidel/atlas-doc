# ── Stage 1: Build ──
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime ──
FROM eclipse-temurin:21-jre-alpine

# Install git + MkDocs
RUN apk add --no-cache git python3 py3-pip \
    && pip3 install mkdocs mkdocs-material --break-system-packages

WORKDIR /app
COPY --from=build /src/target/docs-portal-*.jar app.jar

RUN mkdir -p /app/workspace /app/mkdocs-config /app/site /app/data /app/mkdocs-working

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
