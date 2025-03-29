# JS Portal 데이터베이스 설정 가이드

이 문서는 JS Portal 프로젝트의 데이터베이스 설정 방법을 설명합니다. 

## Render에서 PostgreSQL 데이터베이스 생성하기

1. Render 계정에 로그인하고 대시보드로 이동합니다.
2. `New +` 버튼을 클릭하고 `PostgreSQL` 옵션을 선택합니다.
3. 다음 정보를 입력합니다:
   - Name: `js-portal-db` (원하는 이름 사용 가능)
   - Database: `jsportal` (DB 이름)
   - User: `jsportal_admin` (DB 사용자 이름)
   - Region: 가장 가까운 지역 선택
   - PostgreSQL Version: 최신 버전 (15 이상 권장)
4. 필요한 경우 데이터베이스 사이즈를 조정합니다. 무료 티어로 시작해도 괜찮습니다.
5. `Create Database` 버튼을 클릭합니다.
6. 생성이 완료되면 Render에서 다음 연결 정보를 제공합니다:
   - Internal Database URL
   - External Database URL
   - PSQL Command (CLI 연결용)
   - Username, Password 등

## 데이터베이스 초기화

데이터베이스 생성 후, 다음 방법 중 하나를 선택하여 스키마를 적용합니다:

### 방법 1: psql 명령어 사용

```bash
# schema.sql 적용
psql <RENDER_EXTERNAL_DB_URL> -f src/database/schema.sql

# seed.sql 적용 (초기 데이터 추가)
psql <RENDER_EXTERNAL_DB_URL> -f src/database/seed.sql
```

### 방법 2: Render 콘솔 사용

1. Render 대시보드에서 생성한 PostgreSQL 데이터베이스를 선택합니다.
2. `Shell` 탭을 클릭하여 SQL 실행 콘솔을 엽니다. 
3. schema.sql 내용을 복사하여 붙여넣고 실행합니다.
4. seed.sql 내용을 복사하여 붙여넣고 실행합니다.

### 방법 3: DB 관리 도구 사용

[pgAdmin](https://www.pgadmin.org/), [DBeaver](https://dbeaver.io/) 등의 DB 관리 도구를 사용하여 External Database URL로 연결한 후 스키마와 시드 파일을 실행합니다.

## 환경 변수 설정

백엔드 서비스에서 데이터베이스에 연결하려면 다음 환경 변수를 설정해야 합니다:

```properties
# 스프링 부트 application.properties 또는 환경 변수
SPRING_DATASOURCE_URL=jdbc:postgresql://<호스트>:<포트>/<데이터베이스>
SPRING_DATASOURCE_USERNAME=<사용자이름>
SPRING_DATASOURCE_PASSWORD=<비밀번호>
```

Render에서 웹 서비스를 배포할 때 환경 변수로 설정하는 것이 안전합니다.

## 모니터링 및 유지 관리

- Render 대시보드의 `Metrics` 탭에서 데이터베이스 성능과 사용량을 모니터링할 수 있습니다.
- 자동 백업이 활성화되어 있는지 확인하세요. Render는 일일 백업을 제공합니다.
- 정기적으로 데이터베이스 유지 관리를 수행하세요(예: VACUUM, ANALYZE 등).

## 스키마 업데이트

애플리케이션이 발전함에 따라 데이터베이스 스키마를 업데이트해야 할 수 있습니다. 변경 사항을 추적하고 적용하기 위해 `src/database/migrations` 디렉터리에 마이그레이션 파일을 생성하는 것이 좋습니다.

각 마이그레이션 파일은 날짜와 설명이 포함된 이름을 사용하세요. 예: `V1_1_0__add_user_preferences.sql`

## 주의 사항

- 프로덕션 데이터베이스 자격 증명을 소스 코드에 직접 포함하지 마세요.
- 개발 환경과 프로덕션 환경에 별도의 데이터베이스를 사용하세요.
- 데이터베이스 백업을 정기적으로 확인하세요. 