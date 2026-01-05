# 빌드 단계 - Java 21
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# pom.xml 먼저 복사 (의존성 캐싱)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 소스 코드 복사 및 빌드
COPY src ./src
RUN mvn clean package -DskipTests -B

# 실행 단계 - Java 21 (ARM64 지원)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# JAR 파일 복사
COPY --from=builder /app/target/*.jar app.jar

# ✅ 로그 디렉토리 생성 (경로 통일: /logs)
RUN mkdir -p /logs && chmod 777 /logs

EXPOSE 8080

# JVM 메모리 설정
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# ✅ 프로파일은 환경변수로 받음
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]