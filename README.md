# lifelab-be

백엔드 서버 for LifeLab Project  
(생활습관 실험 기반 서비스의 백엔드 서버)

---

## ⚙️ Tech Stack
- **Language/Framework**: Java 21, Spring Boot 3
- **Build Tool**: Gradle (Kotlin DSL)
- **DB엔진**: MySQL 8.0
- **Docker 컨테이너명**: mysql-dev
- **DB Migration**: Flyway
- **Auth**: Spring Security 적용 (Kakao 로그인 + JWT 토큰 기반 인증으로 전환 예정)
- **Infra(로컬)**: Docker Compose

---

## 📂 프로젝트 구조
```bash
src/main/java/org/lifelab/lifelabbe
├── security/       # Spring Security 설정
├── controller/     # API 엔드포인트
├── service/        # 비즈니스 로직
├── repository/     # DB 접근 계층
└── ...

🌱 브랜치 전략

main: 배포용 (안정화된 코드만 병합)

dev: 개발용 (feature 브랜치 병합 대상)

feature/*: 기능 단위 개발 브랜치


