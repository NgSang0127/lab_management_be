# Build stage
FROM maven:3.8.7-openjdk-18 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime state
FROM amazoncorretto:17
ARG PROFILE=prod
ARG APP_VERSION=0.0.1-SNAPSHOT
#Define new things

WORKDIR /app
COPY --from=build /build/target/LabManagement-*.jar /app/

EXPOSE 8088

ENV DB_URL=jdbc:mysql://mysql-lab:3306/lab_management
ENV ACTIVE_PROFILE=${PROFILE}
ENV JAR_VERSION=${APP_VERSION}
ENV EMAIL_HOSTNAME=smtp.gmail.com
ENV EMAIL_USERNAME=testdev01112002@gmail.com
ENV EMAIL_PASSWORD=sgxklrzfsgeklpyd

CMD java -jar -Dspring.profiles.active=${ACTIVE_PROFILE} -Dspring.datasource.url=${DB_URL} LabManagement-${JAR_VERSION}.jar
