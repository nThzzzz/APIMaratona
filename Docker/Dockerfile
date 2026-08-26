# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiar so o descritor primeiro faz o Docker cachear as dependencias:
# enquanto o pom.xml nao mudar, alterar codigo nao rebaixa esta camada.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Os testes ficam de fora da imagem de proposito: quem roda a suite e o
# `./mvnw test` local e o workflow do GitHub Actions, nao o build do container.
RUN ./mvnw -B -q clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario sem privilegios: nada aqui precisa de root.
RUN useradd --create-home --shell /usr/sbin/nologin maratona
USER maratona

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
