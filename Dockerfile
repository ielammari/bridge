# The clean production version as a container image.
# Build: docker build -t bridge .

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -Pprod -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --create-home --home-dir /app bridge
WORKDIR /app
COPY --from=build /src/target/prod/bridge-prod.jar bridge.jar
RUN mkdir -p storage/prod/cv && chown -R bridge:bridge /app
USER bridge
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "bridge.jar"]
