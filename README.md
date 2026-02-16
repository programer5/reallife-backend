# 📸 RealLife (SNS Backend)

### Real moments. Real people. Real life.

Spring Boot 기반 SNS 백엔드 프로젝트입니다.\
JWT 인증 · REST Docs · Flyway · Redis Pub/Sub · SSE 를 활용해 **실서비스
구조에 가까운 아키텍처**를 목표로 개발했습니다.

------------------------------------------------------------------------

## ✨ 주요 특징

-   Stateless JWT 인증 + Refresh Rotation
-   테스트 기반 API 문서 자동화 (Spring REST Docs)
-   Cursor Pagination 기반 피드/메시지/알림
-   Redis Pub/Sub 기반 멀티 인스턴스 SSE
-   이벤트 기반 알림 시스템
-   Docker 기반 실행 환경 (MySQL + Redis + App + Nginx)

------------------------------------------------------------------------

## 🧱 Architecture Overview

Client ↓ Nginx Reverse Proxy ↓ Spring Boot API ├─ MySQL (Data) └─ Redis
(PubSub / SSE Fanout)

------------------------------------------------------------------------

## 🛠 Tech Stack

  Category    Tech
  ----------- ------------------------
  Backend     Spring Boot 4, Java 17
  Security    Spring Security + JWT
  DB          MySQL + JPA + QueryDSL
  Cache       Redis
  Realtime    SSE
  Migration   Flyway
  Docs        REST Docs
  Infra       Docker Compose + Nginx

------------------------------------------------------------------------

## 🚀 실행 방법

### 1) 환경파일 준비

``` bash
cp .env.sample .env
```

### 2) 전체 실행

``` bash
docker compose down
docker compose up -d --build
```

### 접속

  기능   주소
  ------ -----------------------------------------
  API    http://localhost:8080/api
  Docs   http://localhost:8080/docs
  SSE    http://localhost:8080/api/sse/subscribe

------------------------------------------------------------------------

## 📚 API 문서 생성

``` bash
./gradlew clean test asciidoctor copyRestDocs
```

------------------------------------------------------------------------

## 📡 SSE 이벤트

-   connected
-   ping
-   message-created
-   notification-created

------------------------------------------------------------------------

## 🗺 Roadmap

-   [x] 인증 / 사용자
-   [x] DM + 알림 + SSE
-   [x] 검색 / 커서 페이징
-   [ ] 피드/게시글 고도화
-   [ ] Vue.js 프론트엔드
-   [ ] HTTPS 배포

------------------------------------------------------------------------

## 👨‍💻 목표

단순 CRUD 프로젝트가 아니라\
**실제 서비스 운영 가능한 구조를 경험하기 위한 학습 프로젝트**
