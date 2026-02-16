# ---- build stage ----
FROM gradle:8.7-jdk17 AS builder
WORKDIR /app

# 캐시 효율: 래퍼/설정 먼저 복사
COPY gradlew build.gradle settings.gradle /app/
COPY gradle /app/gradle

# 실행 권한(컨테이너에서 종종 필요)
RUN chmod +x /app/gradlew

# 소스 복사
COPY src /app/src

# ✅ Docker에서는 문서/테스트 강제 X → bootJar만 생성
RUN ./gradlew clean bootJar -x test --no-daemon --stacktrace

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar /app/app.jar

# 업로드 폴더(컨테이너 내부)
RUN mkdir -p /app/uploads

ENV SPRING_PROFILES_ACTIVE=dev
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]