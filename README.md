# Vue + Spring Boot Backend

Vue.js 프론트엔드와 연동하기 위한 Spring Boot 백엔드 프로젝트입니다.  
JWT 기반 인증을 적용하고, JPA + MySQL을 사용합니다.  
개인 학습 및 포트폴리오용 프로젝트이며  
추후 Docker, CI/CD까지 확장하는 것을 목표로 합니다.

---

## 🛠 Tech Stack

### Backend
- Java 17
- Spring Boot 4.0.2
- Spring Security
- Spring Data JPA
- JWT (예정)

### Database
- MySQL
- MySQL Workbench

### Frontend
- Vue.js (별도 프로젝트)

### DevOps (예정)
- Docker
- CI/CD

### Tools
- IntelliJ IDEA
- Gradle
- Git / GitHub

---

## 📁 Project Structure

```text
src
└─ main
   ├─ java
   │  └─ com.example.backend
   │     ├─ config        # Security, JWT 설정
   │     ├─ domain        # Entity
   │     ├─ repository   # JPA Repository
   │     ├─ service      # 비즈니스 로직
   │     └─ controller   # REST API
   └─ resources
      ├─ application.yml
      └─ static
```

---

## ⚙️ Environment Setup

### 1️⃣ MySQL Database 생성

```sql
CREATE DATABASE backend
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```
---

### 2️⃣ application.yml 설정
```properties
spring:
datasource:
url: jdbc:mysql://localhost:3306/backend?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
username: root
password: YOUR_PASSWORD
driver-class-name: com.mysql.cj.jdbc.Driver

jpa:
hibernate:
ddl-auto: update
show-sql: true
```
---
```text
🔐 Security
Spring Security 적용
Session 기반 인증 ❌
JWT 토큰 기반 인증 예정
로그인 / 회원가입 API 구현 예정
```
---
```text
🚀 Run Application
./gradlew bootRun
```
```text
📌 Roadmap
 Spring Boot 프로젝트 생성
 MySQL 연동
 GitHub 연동
 User 엔티티 설계
 회원가입 API
 로그인 API
 JWT 발급 / 검증
 Vue 연동
 Docker 적용
 CI/CD 구축
```