# Stage 1: Build the Java source code
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY src ./src
RUN mkdir -p bin && javac -d bin src/model/*.java src/service/*.java src/storage/*.java src/util/*.java src/app/*.java src/web/*.java

# Stage 2: Minimal Runtime Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/bin ./bin
COPY web ./web
COPY data ./data

ENV PORT=8080
EXPOSE 8080

CMD ["java", "-cp", "bin", "web.WebServer"]
