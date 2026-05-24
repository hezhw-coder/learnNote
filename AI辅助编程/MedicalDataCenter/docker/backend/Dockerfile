FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY backend/pom.xml ./pom.xml
RUN mvn -B -e -DskipTests dependency:go-offline

COPY backend/src ./src
RUN mvn -B -e -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN mkdir -p /app/logs /data/exports

COPY --from=builder /workspace/target/*.jar /app/app.jar

ENV TZ=Asia/Shanghai
ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
