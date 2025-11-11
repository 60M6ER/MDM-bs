# ---------- build ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1) Кэш зависимостей бэка
COPY backend/pom.xml ./backend/pom.xml
WORKDIR /app/backend
#RUN mvn -B -q -DskipTests dependency:go-offline

# 2) Копируем исходники бэка и фронта
COPY backend/ /app/backend/
COPY frontend/ /app/frontend/

# 3) Сборка:
#   — в pom.xml backend должен быть настроен frontend-maven-plugin
#     с workingDirectory=../frontend и копированием dist в classpath:/static
RUN mvn -B -DskipTests clean package

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /opt/app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC" \
    SPRING_PROFILES_ACTIVE=prod
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]