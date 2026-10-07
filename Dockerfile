# ---- 阶段1：构建前端 ----
FROM node:22-alpine AS frontend
WORKDIR /fe
COPY frontend/package*.json ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---- 阶段2：构建后端（打包前端静态资源进 jar）----
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
# 用阶段1产物覆盖静态资源目录
COPY --from=frontend /fe/dist ./src/main/resources/static
RUN mvn -q -B clean package -DskipTests

# ---- 阶段3：运行镜像 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /build/target/vision-platform.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=postgres \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
