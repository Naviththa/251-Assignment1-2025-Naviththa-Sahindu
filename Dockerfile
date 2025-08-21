FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /src

COPY text-editor/pom.xml text-editor/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -f text-editor/pom.xml -q -DskipTests dependency:go-offline

COPY text-editor/ text-editor/
RUN --mount=type=cache,target=/root/.m2 mvn -f text-editor/pom.xml -q -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/text-editor/target/*-SNAPSHOT.jar app.jar
# If that COPY fails because you don't use SNAPSHOT, use:
# COPY --from=build /src/text-editor/target/*.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
