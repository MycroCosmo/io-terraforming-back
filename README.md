# Photo Portfolio Backend

사진가의 프로젝트와 이미지를 카테고리별로 전시·관리하는 Spring Boot API입니다. 4인 팀 프로젝트이며, 현재 제출 브랜치는 도메인 리팩터링 이후 깨진 계약을 복구하고 쓰기 API와 외부 저장소 정합성을 강화한 결과를 담습니다.

## 해결하려는 문제

- 공개 사용자는 프로젝트와 사진을 빠르게 조회할 수 있어야 합니다.
- 관리자만 프로젝트·카테고리·이미지를 변경할 수 있어야 합니다.
- 이미지 저장소와 PostgreSQL 중 한쪽만 변경되는 불일치를 줄여야 합니다.
- 목록 조회에서 불필요한 entity loading과 N+1을 피해야 합니다.

## Architecture

```text
Client
  │ REST
Spring MVC / Security
  │
Service + transaction boundary
  ├─ Spring Data JPA ─ PostgreSQL
  ├─ Spring Cache
  └─ GcsService ─ Google Cloud Storage
```

## 주요 기술

- Java 17, Spring Boot 3.3
- Spring MVC, Spring Security, Spring Data JPA
- PostgreSQL, JPQL DTO projection
- Google Cloud Storage, WebP conversion
- Spring Cache, MapStruct
- JUnit 5, Mockito, Spring MVC Test, Data JPA Test

## 핵심 기술적 문제와 해결

### 1. 도메인 리팩터링 이후 컴파일 계약 복구

DTO·mapper·service·repository test가 서로 다른 과거 API를 참조해 clean build가 중단됐습니다. mutable setter를 복원하지 않고 현재 생성자와 연관관계 편의 메서드를 기준으로 mapper와 test를 맞췄습니다. category/subcategory는 service에서 조회한 뒤 Project 생성·수정에 전달합니다.

### 2. 공개 조회와 관리자 쓰기 API 분리

기존 security 설정은 `/api/admin/**`만 보호했지만 실제 쓰기 endpoint는 `/api/projects/**`, `/api/categories/**`에 있었습니다. HTTP method 기준으로 GET은 공개하고 POST/PUT/DELETE는 인증을 요구하도록 수정했으며 MVC security test로 고정했습니다.

### 3. GCS 업로드와 DB transaction 정합성

기존 구현은 GCS 업로드를 background executor에 맡긴 즉시 URL을 반환해, 업로드 실패를 DB transaction이 알 수 없었습니다.

- 업로드 완료 후에만 URL 반환
- 신규 파일은 DB rollback 시 보상 삭제
- 교체·삭제 대상 파일은 DB commit 이후 삭제
- 이미 없는 파일의 삭제는 idempotent하게 처리
- bucket 이름을 하드코딩하지 않고 설정값으로 URL 검증

DB와 object storage를 하나의 ACID transaction으로 묶었다고 주장하지 않습니다. transaction synchronization을 이용해 실패 순서별 불일치 가능성을 줄인 보상 방식입니다.

### 4. 조회 query와 cache

- 목록은 JPQL DTO projection과 `Slice`를 사용해 필요한 컬럼만 조회
- 관리자 검색은 photo relation을 LEFT JOIN하고 count를 한 query에서 계산
- category/subcategory는 fetch join으로 조회
- 조회수는 entity read-modify-write 대신 DB update query로 원자 증가
- cache key에 page number, size, sort, filter를 포함해 서로 다른 요청의 충돌 방지

### 5. 오류 응답

DB·예상하지 못한 예외 원문은 server log에 남기고 API에는 일반화된 5xx detail만 반환합니다. table, query, storage endpoint가 client 응답에 노출되지 않도록 했습니다.

## 테스트 전략

- GET 공개 및 익명 쓰기 거부
- 현재 domain constructor와 연관관계 기반 repository 저장
- category/subcategory를 해석한 프로젝트 생성
- image upload 후 photo-project 연관관계 저장
- DB commit 전 기존 GCS 파일 미삭제
- DB rollback 시 신규 GCS 파일 보상 삭제
- 내부 예외 detail 비노출

```bash
./gradlew clean test
```

## 실행 환경

필수 환경변수:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `GCS_KEY`, `PROJECT_ID`, `BUCKET`

secret과 service-account key는 repository에 저장하지 않습니다.

## 현재 한계

- 실 GCS와 PostgreSQL을 함께 사용하는 failure-injection 통합 테스트는 아직 없습니다.
- cache는 local Spring Cache이며 다중 instance cache 일관성은 지원하지 않습니다.
- 성능 개선 수치는 재현 가능한 benchmark가 없어 기재하지 않습니다.
- GCP dependency 조합의 장기 지원성 검토가 필요합니다.

## 결과와 배운 점

외부 저장소 호출을 DB transaction annotation만으로 원자화할 수는 없습니다. 업로드와 삭제의 순서를 분리하고 rollback/after-commit 보상을 명시해야 어떤 실패에서 orphan 또는 깨진 URL이 생기는지 설명하고 테스트할 수 있었습니다.
