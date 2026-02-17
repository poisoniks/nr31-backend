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

RUN addgroup --system nr31 && adduser --system --ingroup nr31 nr31
USER nr31:nr31

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]