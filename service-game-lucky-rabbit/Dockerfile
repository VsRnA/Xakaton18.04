FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY core/pom.xml ./core/pom.xml
COPY service-game-lucky-rabbit/pom.xml ./service-game-lucky-rabbit/pom.xml
RUN mvn dependency:go-offline -pl service-game-lucky-rabbit -am -q
COPY service-game-lucky-rabbit/src ./service-game-lucky-rabbit/src
RUN mvn package -pl service-game-lucky-rabbit -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/service-game-lucky-rabbit/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
