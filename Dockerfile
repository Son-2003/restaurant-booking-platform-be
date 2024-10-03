# Use an official Maven image to build the application
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Use an official OpenJDK runtime as a base image
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /target/FoodBookingPlatform-0.0.1-SNAPSHOT.jar FoodBookingPlatform.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "FoodBookingPlatform.jar"]
