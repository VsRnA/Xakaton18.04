FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY stoloto-core/pom.xml ./stoloto-core/pom.xml
COPY bonus-game-service/pom.xml ./bonus-game-service/pom.xml
RUN mvn dependency:go-offline -pl bonus-game-service -am -q
COPY bonus-game-service/src ./bonus-game-service/src
RUN mvn package -pl bonus-game-service -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/bonus-game-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
