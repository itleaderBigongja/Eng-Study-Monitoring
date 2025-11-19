# 멀티 스테이지 빌드
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# pom.xml 먼저 복사 (캐싱 최적화)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 소스 코드 복사 및 빌드
COPY src ./src
RUN mvn clean package -DskipTests -B

# 실행 이미지
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# JAR 파일 복사
COPY --from=builder /app/target/*.jar app.jar

# 비root 사용자
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM 메모리 설정
ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]