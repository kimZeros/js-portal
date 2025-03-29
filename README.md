# JS Portal

JS Portal은 콘텐츠 자동화 시스템으로, 여러 커뮤니티 사이트에서 콘텐츠를 수집하고 OpenAI를 활용하여 새로운 콘텐츠를 생성합니다.

## 프로젝트 구조

이 프로젝트는 프론트엔드와 백엔드로 분리되어 있습니다:

```
js-portal/
├── frontend/         # Next.js 프론트엔드
│   ├── public/       # 정적 파일
│   ├── src/          # 소스 코드
│   │   └── app/      # Next.js 앱 디렉토리
│   └── ...           # 기타 프론트엔드 관련 파일
└── backend/          # Spring Boot 백엔드
    ├── src/          # 소스 코드
    │   ├── main/     # 메인 코드
    │   │   ├── java/com/jsportal/  # Java 소스 코드
    │   │   └── resources/          # 리소스 파일
    │   └── database/               # 데이터베이스 관련 파일
    └── ...           # 기타 백엔드 관련 파일
```

## 백엔드 실행 방법

```bash
cd backend
mvn spring-boot:run
```

## 프론트엔드 실행 방법

```bash
cd frontend
npm run dev
```

## 주요 기능

1. **커뮤니티 콘텐츠 크롤링**: 인기 커뮤니티 사이트에서 인기 포스트를 자동으로 크롤링합니다.
2. **AI 콘텐츠 생성**: OpenAI API를 활용하여 수집된 데이터를 기반으로 새로운 콘텐츠를 생성합니다.
3. **다국어 지원**: 한국어, 영어, 일본어 등 다양한 언어로 콘텐츠를 생성합니다.
4. **자동 배치 처리**: 일정 간격으로 콘텐츠 수집 및 생성 작업을 자동으로 수행합니다.
5. **콘텐츠 관리 시스템**: 생성된 콘텐츠를 관리하고 사용자에게 제공하는 시스템을 포함합니다.

## 기술 스택

- **백엔드**: Spring Boot, JPA, MySQL
- **프론트엔드**: Next.js, TypeScript, TailwindCSS
- **API**: OpenAI API
- **배포**: Docker, AWS
