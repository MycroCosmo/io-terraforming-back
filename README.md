# Photo Portfolio Backend

사진 프로젝트를 카테고리별로 관리하고 전시하는 Spring Boot 백엔드입니다. 프로젝트·카테고리·사진 조회, 관리자 기능, GCS 이미지 처리 코드를 포함합니다.

현재 기본 브랜치에는 DTO와 엔티티 변경 후 서비스·테스트에 반영되지 않은 부분이 있어, 리팩터링 완료 버전으로 소개하지 않습니다.

## 기술 구성

빌드 설정 기준으로 Java 17, Spring Boot 3.3.4, Spring Data JPA, Spring Security, Spring Cache, MapStruct, GCS와 WebP 변환 라이브러리를 사용합니다. PostgreSQL·H2 의존성이 포함되어 있습니다. Redis나 MySQL을 현재 구성의 필수 요소로 표시하지 않습니다.

```text
REST Controller
  └─ Service
       ├─ JPA Repository → 데이터베이스
       └─ GcsService → WebP 변환·GCS 저장
```

## 코드에서 확인할 부분

- [프로젝트 API](src/main/java/com/example/portfolio/controller/ProjectController.java)
- [프로젝트 조회·변경 서비스](src/main/java/com/example/portfolio/service/ProjectService.java)
- [카테고리 연관 조회와 프로젝트 쿼리](src/main/java/com/example/portfolio/repository/ProjectRepository.java)
- [사진 처리](src/main/java/com/example/portfolio/service/PhotoService.java)
- [GCS 연동](src/main/java/com/example/portfolio/service/GcsService.java)

엔티티의 변경 메서드와 관계 편의 메서드, record DTO, DTO 기반 조회와 fetch join 등의 리팩터링 코드가 있습니다. 이러한 코드의 존재와 전체 빌드·테스트의 통과 여부는 별도로 확인해야 합니다.

## 프로젝트 API 경로

현재 `ProjectController`에 정의된 경로입니다. 실행 성공을 검증한 API 목록이라는 뜻은 아닙니다.

| 메서드 | 경로 | 역할 |
| --- | --- | --- |
| POST | `/api/projects` | 프로젝트 생성 |
| PUT | `/api/projects/{projectId}` | 프로젝트 수정 |
| GET | `/api/projects` | 프로젝트 목록 |
| GET | `/api/projects/{projectId}` | 프로젝트 상세 |
| GET | `/api/projects/{id}/photos` | 프로젝트 사진 목록 |
| DELETE | `/api/projects/{projectId}` | 프로젝트 삭제 |

## 현재 리팩터링 제한

### 서비스와 테스트의 일치

record로 변경된 DTO와 과거 setter 기반 테스트가 함께 남아 있습니다. 서비스에서 사용하는 Repository 참조·메서드와 현재 선언도 일치 여부를 정리해야 합니다. 테스트 파일이 존재한다는 이유만으로 회귀 검증 완료 상태로 간주하지 않습니다.

### 업로드와 DB의 실패 처리

현재 `GcsService.uploadWebpFile()`은 비동기 업로드를 시작한 뒤 완료를 기다리지 않고 URL을 반환합니다. 따라서 동기 업로드 완료 후 URL을 저장하는 버전과는 다릅니다.

썸네일 교체 코드에서는 기존 파일 삭제가 먼저 호출됩니다. DB 롤백 시 신규 파일 삭제, DB 커밋 후 기존 파일 삭제가 완성되어 있다고 설명하지 않습니다. 파일 저장소와 DB 사이의 보상 처리 및 실패 테스트는 별도 수정 대상입니다.

### 성능 수치

기존 문서의 응답 시간·DB 부하 감소·코드 줄 수 감소 수치는 재현 가능한 측정 근거가 확인되지 않아 제거했습니다. 정량 성과를 추가할 때는 데이터 규모, 실행 환경, 측정 방법과 전후 결과를 함께 기록해야 합니다.

## 로컬 검증 절차

JDK 17을 준비하고 저장소 루트에서 실행합니다.

```bash
./gradlew compileJava
./gradlew test
```

Windows에서는 `.\gradlew.bat`를 사용합니다. 현재 리팩터링 불일치가 남아 있으므로 위 명령을 통과했다고 보장하지 않습니다. 먼저 컴파일·테스트를 정리한 뒤 실행 환경을 설정해야 합니다.

GCS 서비스는 `spring.cloud.gcp.storage.bucket`과 `spring.cloud.gcp.storage.credentials.location` 설정을 읽습니다. 자격 증명은 저장소 밖에서 관리해야 합니다. 테스트에는 외부 GCS 호출을 분리하고 개발용 DB를 사용하세요.

이 문서 수정은 소스 코드 리팩터링이나 배포를 수행한 작업이 아닙니다. 현재 저장소와 문서의 불일치를 바로잡은 것입니다.
