# JS Portal - 백엔드 시스템 아키텍처

JS Portal은 자동화된 콘텐츠 생성 및 관리 시스템으로, 다중 언어(한국어, 영어, 일본어) 지원과 다양한 수익 모델을 통한 최적화된 수익 창출을 목표로 합니다.

## 백엔드 시스템 개요

JS Portal 백엔드는 Spring Boot 기반으로 구축되어 있으며, 주요 기능은 다음과 같습니다:

1. **인기 키워드 자동 수집**: Google Trends와 Naver DataLab API를 활용하여 인기 키워드를 자동으로 수집합니다.
2. **자동 콘텐츠 생성**: OpenAI GPT API를 활용하여 키워드 기반의 고품질 콘텐츠를 자동으로 생성합니다.
3. **커뮤니티 콘텐츠 크롤링**: 인기 커뮤니티 사이트에서 인기 콘텐츠를 수집하고 재작성합니다.
4. **소셜 미디어 자동 포스팅**: Facebook과 Naver 블로그에 자동으로 콘텐츠를 포스팅합니다.
5. **수익 분석 및 최적화**: 다양한 광고 플랫폼(AdSense, Coupang Partners 등)의 수익 데이터를 수집하고 분석합니다.

## 시스템 아키텍처

```
                                         +-------------------+
                                         |   Frontend (Next.js)  |
                                         +-------------------+
                                                  |
                                                  | HTTP/REST
                                                  v
+-------------------+    +-----------------+    +-------------------+    +-----------------+
|  외부 API 서비스   |<-->|  서비스 레이어   |<-->|  컨트롤러 레이어   |<-->|  데이터베이스    |
+-------------------+    +-----------------+    +-------------------+    +-----------------+
    |         |              |        |                                        ^
    |         |              |        |                                        |
    v         v              v        v                                        |
+-------+ +-------+    +----------+ +----------+                        +--------------+
| OpenAI | | 소셜 미디어 |    | 배치 작업 | | 스케줄러 |                        |  레포지토리   |
+-------+ +-------+    +----------+ +----------+                        +--------------+
```

## 주요 컴포넌트

### 1. 서비스 클래스

#### 1.1 OpenAiService
- OpenAI GPT API를 활용한 콘텐츠 생성 담당
- 키워드 기반 정보성 콘텐츠 생성
- 커뮤니티 콘텐츠 기반 재미있는 콘텐츠 생성
- 언어별 프롬프트 최적화

#### 1.2 KeywordCollectionBatchService
- Google Trends와 Naver DataLab API를 활용한 인기 키워드 수집
- 언어별(한국어, 영어, 일본어) 키워드 관리
- 키워드 우선순위 결정 및 중복 제거

#### 1.3 ContentGenerationBatchService
- 수집된 키워드를 기반으로 콘텐츠 자동 생성
- 일일 생성 제한 및 간격 관리
- 콘텐츠 재생성 기능

#### 1.4 CrawlingService
- 다양한 커뮤니티 사이트에서 인기 콘텐츠 크롤링
- CSS 선택자 기반 데이터 추출
- 원본 콘텐츠에서 유용한 정보 추출

#### 1.5 FacebookPostingService
- Facebook Graph API를 활용한 페이지 포스팅
- 이미지, 비디오, 링크 포스팅 지원
- 토큰 관리 및 자동 갱신

#### 1.6 NaverBlogPostingService
- Naver Blog API를 활용한 블로그 포스팅
- HTML 변환 및 포맷팅
- OAuth 인증 및 토큰 관리

#### 1.7 RevenueAnalysisService
- 다양한 광고 플랫폼의 수익 데이터 수집 및 분석
- 일별, 플랫폼별, 언어별 수익 분석
- 트렌드 및 패턴 식별

### 2. 배치 작업 및 스케줄링

백엔드 시스템은 다음과 같은 정기적인 배치 작업을 실행합니다:

| 배치 작업 | 실행 시간 | 설명 |
|----------|---------|------|
| 키워드 수집 | 매일 06:00 | 인기 키워드 수집 |
| 콘텐츠 생성 | 매일 08:00 | 키워드 기반 콘텐츠 생성 |
| 커뮤니티 크롤링 | 매일 10:00, 16:00, 22:00 | 인기 커뮤니티 콘텐츠 수집 |
| 소셜 미디어 포스팅 | 매일 11:30, 17:30, 23:30 | 생성된 콘텐츠 자동 포스팅 |
| 수익 데이터 수집 | 매일 05:00 | 전날 수익 데이터 수집 |

### 3. 데이터 모델

#### 3.1 주요 엔티티

**Keyword**
- 인기 검색어 정보 저장
- 언어, 우선순위, 상태 등 관리

**Content**
- 생성된 콘텐츠 저장
- 제목, 내용, 썸네일, 상태 등 관리
- 키워드 및 언어와 연결

**ContentSource**
- 콘텐츠 출처 정보 저장
- 크롤링된 콘텐츠의 원본 URL 등 저장

**SocialPostingHistory**
- 소셜 미디어 포스팅 이력 저장
- 플랫폼, 포스트 ID, URL 등 저장

**DailyRevenue**
- 일별 수익 데이터 저장
- 플랫폼, 금액, 클릭수 등 저장

## 기술 스택

- **언어 및 프레임워크**: Java 17, Spring Boot 3.x
- **데이터베이스**: PostgreSQL
- **ORM**: Spring Data JPA
- **마이그레이션**: Flyway
- **API 문서화**: Springdoc OpenAPI (Swagger)
- **외부 API 통합**: RestTemplate
- **스케줄링**: Spring Scheduler
- **캐싱**: Caffeine
- **보안**: Spring Security, JWT
- **로깅**: SLF4J, Logback

## 환경 설정

시스템은 다음과 같은 환경 변수를 통해 구성됩니다:

- **데이터베이스**: DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
- **OpenAI API**: OPENAI_API_KEY, OPENAI_MODEL
- **Facebook**: FACEBOOK_APP_ID, FACEBOOK_APP_SECRET, FACEBOOK_PAGE_ID, FACEBOOK_PAGE_ACCESS_TOKEN
- **Naver**: NAVER_CLIENT_ID, NAVER_CLIENT_SECRET, NAVER_ACCESS_TOKEN, NAVER_REFRESH_TOKEN
- **AdSense**: ADSENSE_CLIENT_ID, ADSENSE_CLIENT_SECRET, ADSENSE_ACCESS_TOKEN, ADSENSE_REFRESH_TOKEN
- **Coupang Partners**: COUPANG_ACCESS_KEY, COUPANG_SECRET_KEY

## 배포 방법

1. 필요한 환경 변수 설정
2. 데이터베이스 생성 및 초기화
3. JAR 파일 빌드: `./mvnw clean package`
4. JAR 파일 실행: `java -jar jsportal-backend.jar`

## 개발 방법

1. 개발 환경 설정: `./mvnw spring-boot:run`
2. API 문서 접근: `http://localhost:8080/api/swagger-ui.html`
3. H2 콘솔 접근: `http://localhost:8080/api/h2-console` (개발 환경에서만 사용)

## 확장성 고려사항

- **언어 추가**: 추가 언어 지원은 `supported.languages` 설정에 언어 코드 추가로 가능
- **광고 플랫폼 추가**: `RevenuePlatform` 엔티티에 새 플랫폼 추가
- **커뮤니티 소스 추가**: `CrawlConfig` 클래스에 새 사이트 설정 추가

## 성능 최적화

- 주기적인 배치 작업은 시간대를 분산하여 시스템 부하 분산
- API 요청 제한을 위한 간격 설정으로 외부 API 비용 최적화
- 캐싱을 통한 DB 부하 감소
- 대용량 데이터 처리 시 페이징 사용

## 보안 고려사항

- 모든 API 키 및 비밀 값은 환경 변수로 관리
- 외부 API 토큰은 필요시에만 갱신하여 노출 최소화
- 사용자 인증은 JWT 토큰 기반으로 구현
- API 요청 검증 및 입력값 검증을 통한 보안 강화

## 모니터링 및 로깅

- 모든 서비스는 SLF4J를 통한 로깅 구현
- API 호출, 배치 작업 시작/종료, 에러 상황 등 상세 로깅
- 로그 파일은 일별 롤링 및 최대 보관 기간 설정
- 프로메테우스 및 그라파나를 통한 모니터링 가능 (추가 설정 필요)

## 앞으로의 개선 계획

1. **AI 모델 다양화**: GPT 외 다른 AI 모델 통합으로 콘텐츠 다양성 확보
2. **콘텐츠 품질 개선**: 자동 생성된 콘텐츠의 품질 평가 시스템 도입
3. **소셜 미디어 채널 확장**: 인스타그램, 트위터 등 추가 채널 지원
4. **성능 최적화**: 대용량 데이터 처리를 위한 배치 작업 최적화
5. **사용자 맞춤형 분석**: 사용자별 콘텐츠 성과 및 수익 분석 기능 추가 