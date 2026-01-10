# study-monitoring의 Dockerfile
# 빌드 단계 - Java 21
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# 실행 단계 - Java 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8081

# [수정] JVM 메모리 다이어트 설정 (기본값)
# 512m -> 256m로 축소하고, SerialGC 및 빠른 부팅 옵션 추가
ENV JAVA_OPTS="-Xms256m -Xmx256m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# ✅ exec 형식을 사용하여 시그널 전달이 잘 되도록 수정 (권장)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
