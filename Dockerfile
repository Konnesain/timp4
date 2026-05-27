FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve -q
COPY src ./src
RUN mvn package -q -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache tzdata
ENV TZ=Asia/Novosibirsk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 2001
ENTRYPOINT ["java", "-jar", "app.jar"]
