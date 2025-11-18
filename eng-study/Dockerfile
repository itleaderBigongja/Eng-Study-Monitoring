# 멀티 스테이지 빌드 - 빌드 단계
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# pom.xml 먼저 복사( 의존성 캐싱 )
COPY pom.xml .
RUN mvn dependency:go-offline

# 소스 코드 복사 및 빌드
COPY src ./src
RUN mvn clean package -DskipTests

# 실행 단계
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/target/*.jar app.jar

# 비root 사용자 생성 (보안)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 포트 노출
EXPOSE 8080

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]

