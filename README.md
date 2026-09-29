# Photo Portfolio Backend

사진작가의 프로젝트와 이미지를 카테고리별로 전시·관리하는 Spring Boot API입니다.
4인 팀 프로젝트로 시작했고, 이후 혼자 조회 구조와 외부 저장소 정합성을 다시 설계했습니다.

## 역할

- **팀 개발 당시** · 카테고리·서브카테고리, 관리자 목록 조회와 캐시, GCS client 재사용, WebP 변환
- **이후 개인 리팩터링** · N+1 제거와 조회 구조 재설계, 깨진 도메인 계약 복구, 쓰기 API 인가, GCS·DB 보상 처리, 오류 응답 정리

## 해결하려는 문제

- 공개 사용자는 프로젝트와 사진을 빠르게 조회할 수 있어야 합니다.
- 관리자만 프로젝트·카테고리·이미지를 변경할 수 있어야 합니다.
- 이미지 저장소와 PostgreSQL 중 한쪽만 변경되는 불일치를 줄여야 합니다.
- 목록 조회에서 불필요한 엔티티 로딩과 N+1을 피해야 합니다.

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
- Google Cloud Storage, WebP 변환
- Spring Cache, MapStruct
- JUnit 5, Mockito, Spring MVC Test, Data JPA Test

## 핵심 문제와 해결

### 1. 캐시로 가렸던 N+1을 원인부터 제거

팀 개발 당시 JPA를 처음 사용하면서 목록 조회가 느린 원인을 모른 채 캐시로 응답 속도를 맞췄습니다. 이후 쿼리 수는 그대로이고 캐시 무효화 부담만 늘어난 것을 확인했고, 지연 로딩 상태의 반복 접근으로 생기는 N+1이 원인이었습니다.

- 목록은 JPQL DTO projection과 `Slice`로 필요한 컬럼만 조회
- 관리자 검색은 photo 연관관계를 LEFT JOIN하고 count를 한 쿼리에서 계산
- category/subcategory는 fetch join으로 조회
- 조회수는 엔티티 read-modify-write 대신 DB UPDATE 쿼리로 원자 증가
- 캐시는 필요한 곳에만 남기고, 캐시 키에 page·size·sort·filter를 포함해 서로 다른 요청의 충돌 방지

### 2. 도메인 리팩터링 이후 컴파일 계약 복구

DTO·mapper·service·repository test가 서로 다른 과거 API를 참조해 clean build가 중단됐습니다. mutable setter를 되살리지 않고 현재 생성자와 연관관계 편의 메서드를 기준으로 mapper와 test를 맞췄습니다. category/subcategory는 service에서 조회한 뒤 Project 생성·수정에 전달합니다.

### 3. 공개 조회와 관리자 쓰기 API 분리

기존 security 설정은 `/api/admin/**`만 보호했지만, 실제 쓰기 endpoint는 `/api/projects/**`, `/api/categories/**`에 있었습니다. HTTP method 기준으로 GET은 공개하고 POST/PUT/DELETE는 인증을 요구하도록 수정했고, MVC security test로 고정했습니다.

### 4. GCS 업로드와 DB 트랜잭션 정합성

기존 구현은 GCS 업로드를 background executor에 맡긴 즉시 URL을 반환해, 업로드 실패를 DB 트랜잭션이 알 수 없었습니다.

- 업로드 완료 후에만 URL 반환
- 신규 파일은 DB rollback 시 보상 삭제
- 교체·삭제 대상 파일은 DB commit 이후 삭제
- 이미 없는 파일의 삭제는 idempotent하게 처리
- bucket 이름을 하드코딩하지 않고 설정값으로 URL 검증

**트레이드오프** · DB와 object storage를 하나의 ACID 트랜잭션으로 묶을 수는 없습니다. transaction synchronization으로 실패 순서별 불일치 가능성을 줄이는 보상 방식을 택했고, 동기 업로드로 응답은 느려지지만 저장된 URL이 항상 실제 파일을 가리키는 쪽을 우선했습니다.

### 5. 오류 응답

DB·예상하지 못한 예외 원문은 서버 로그에 남기고, API에는 일반화된 5xx 응답만 반환합니다. 테이블, 쿼리, 저장소 endpoint가 클라이언트에 노출되지 않도록 했습니다.

## 테스트

- GET 공개 및 익명 쓰기 거부
- 현재 도메인 생성자와 연관관계 기반 repository 저장
- category/subcategory를 해석한 프로젝트 생성
- 이미지 업로드 후 photo-project 연관관계 저장
- DB commit 전 기존 GCS 파일 미삭제
- DB rollback 시 신규 GCS 파일 보상 삭제
- 내부 예외 detail 비노출

```bash
./gradlew clean test
```

## 실행 환경

필수 환경변수: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GCS_KEY`, `PROJECT_ID`, `BUCKET`

secret과 service-account key는 저장소에 두지 않습니다.

## 다음 과제

- 실 GCS와 PostgreSQL을 함께 쓰는 장애 주입 통합 테스트
- 다중 인스턴스 환경의 캐시 일관성 (현재는 로컬 Spring Cache)
- 재현 가능한 벤치마크로 조회 성능 수치 측정

## 배운 점

외부 저장소 호출은 `@Transactional`만으로 원자화할 수 없습니다. 업로드와 삭제의 순서를 나누고 rollback/after-commit 보상을 명시해야, 어떤 실패에서 고아 파일이나 깨진 URL이 생기는지 설명하고 테스트할 수 있었습니다.
