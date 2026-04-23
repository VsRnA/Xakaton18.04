FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY core/pom.xml ./core/pom.xml
COPY service-game-lucky-rabbit/pom.xml ./service-game-lucky-rabbit/pom.xml
RUN mvn dependency:go-offline -pl core -am -q
COPY core/src ./core/src
RUN mvn package -pl core -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/core/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
