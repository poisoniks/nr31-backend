# Build stage
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src

RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ARG PROFILE=prod
ENV SPRING_PROFILES_ACTIVE=${PROFILE}

RUN addgroup --system nr31 && adduser --system --ingroup nr31 nr31
RUN mkdir -p /app/uploads && chown nr31:nr31 /app/uploads
RUN mkdir -p /app/logs && chown nr31:nr31 /app/logs
USER nr31:nr31

VOLUME /app/uploads
VOLUME /app/logs

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]