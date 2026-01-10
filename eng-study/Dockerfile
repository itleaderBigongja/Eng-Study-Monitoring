# eng-study의 Dockerfile
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

# [수정] JVM 메모리 다이어트 설정 (기본값)
# -Xmx256m: 최대 힙 256MB 제한 (YAML 설정이 없어도 안전하게)
# -XX:+UseSerialGC: 작은 메모리에 적합한 가벼운 GC
# -XX:TieredStopAtLevel=1: 부팅 속도 향상 (JIT 컴파일 제한)
ENV JAVA_OPTS="-Xms256m -Xmx256m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# ✅ exec 형식을 사용하여 시그널 전달이 잘 되도록 수정 (권장)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]