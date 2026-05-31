# RIKU-BE 리팩터링 제안서

작성일: 2026-05-31

이 문서는 현재 레포지토리를 기준으로 한 도메인 기반 패키지 재구성 및 코드 개선 제안입니다. 실제 파일 이동이나 코드 변경은 아직 수행하지 않았고, 검토용 제안만 정리했습니다.

## 1. 현재 구조 요약

현재 프로젝트는 대부분 기술 계층 기반 구조입니다.

```text
RIKU.server
  Config
  Controller
  Dto
  Entity
  Repository
  Security
  Service
  Util
```

특징:

- `Controller`, `Service`, `Repository`, `Entity`, `Dto`로 나뉜 layer-based 구조입니다.
- `src/main/java/RIKU/server/domains` 아래에 `auth`, `post`, `user` 빈 디렉터리가 있어 도메인 구조 전환 흔적은 있지만 실제 Java 파일은 기존 구조에 남아 있습니다.
- 주요 비즈니스는 사용자, 인증, 런 게시글, 참여/출석, 댓글, 페이서, 일정, 포인트/랭킹, 운영진 관리로 나뉩니다.

## 2. 도메인 후보

실제 코드 기준 도메인 후보:

- `auth`: 로그인, JWT 발급, 인증 흐름
- `user`: 회원가입, 프로필, 일일 출석
- `admin`: 운영진 사용자 관리, 페이서 권한 관리, 임시 비밀번호 발급
- `run.post`: 정규런/훈련/행사/번개 게시글, 게시글 목록/상세/수정/삭제
- `run.participant`: 런 참여, 출석 코드, 출석, 마감, 수동 출석 처리
- `run.comment`: 게시글 댓글/대댓글
- `run.pacer`: 페이서 배정, 페이스 그룹
- `run.home`: 홈 화면 카드 조회
- `calendar`: 일/월 일정 조회
- `point`: 포인트, 참여 내역, 랭킹
- `storage`: S3 업로드/삭제
- `security`: JWT, 인증 필터, 인증 Principal
- `global`: 공통 응답, 공통 예외, validation, base entity, 날짜 유틸

레포지토리에서 `chat`, `notification`, `goal`, `badge`, `suggestion`에 해당하는 구현은 확인되지 않았습니다.

## 3. 추천 패키지 구조

```text
RIKU.server
  ServerApplication

  config
    SecurityConfig
    SwaggerConfig

  security
    AuthMember
    AuthenticationProviderImpl
    JwtAuthenticationFilter
    JwtInfo
    JwtTokenProvider
    UserDetailsServiceImpl

  global
    response
    exception
    validation
    entity
    util

  infra
    storage
      S3Config
      S3Uploader

  domains
    auth
      controller
      service
      dto.request
      dto.response

    user
      controller
      service
      repository
      entity
      dto.request
      dto.response

    admin
      controller
      service
      dto.request
      dto.response

    run
      post
        controller
        service
        repository
        entity
        dto.request
        dto.response
      participant
        controller
        service
        repository
        entity
        dto.request
        dto.response
      comment
        controller
        service
        repository
        entity
        dto.request
        dto.response
      pacer
        repository
        entity
        dto.request
        dto.response
      home
        controller
        service
        dto.response

    calendar
      controller
      service
      dto.response

    point
      controller
      service
      repository
      entity
      dto.request
      dto.response

    season
      SeasonProvider
```

## 4. 핵심 파일 이동 요약

상세 이동표는 `docs/refactoring-migration-map.csv`에 별도로 정리했습니다.

요약:

- 인증 관련: `AuthController`, `AuthService`, 로그인 DTO는 `domains/auth`로 이동
- 사용자 관련: `UserController`, `UserService`, `UserRepository`, `User`, `UserRole`, 프로필 DTO는 `domains/user`로 이동
- 운영진 관련: `AdminController`, `AdminService`, 운영진 전용 DTO는 `domains/admin`으로 이동
- 게시글 관련: `Controller/Post`, `Service/Post`, post 관련 repository/entity/dto는 `domains/run/post`로 이동
- 참여/출석 관련: `ParticipantController`, `ParticipantService`, participant entity/repository/dto는 `domains/run/participant`로 이동
- 댓글 관련: `CommentController`, `CommentService`, comment entity/repository/dto는 `domains/run/comment`로 이동
- 페이서 관련: `Pacer`, `PacerRepository`, pacer DTO는 `domains/run/pacer`로 이동
- 포인트/랭킹 관련: `UserPointController`, `UserPointService`, `UserPointRepository`, `UserPoint`, `PointType`, 랭킹 DTO는 `domains/point`로 이동
- 공통 인프라: `S3Config`, `S3Uploader`는 `infra/storage`로 이동
- 공통 코드: `BaseResponse`, `BaseResponseStatus`, advice, validation DTO, base entity, exception, util은 `global`로 이동

## 5. 코드 개선 제안

### 보안 설정 우선 검토

`SecurityConfig`에서 `/user/**`가 `permitAll()`로 열려 있습니다. 현재 이 패턴은 로그인/회원가입뿐 아니라 `/user/profile`, `/user/attend`, `/user/{userId}/role`까지 포함할 수 있습니다. 특히 `AuthController.updateUserRole`은 테스트용 API로 보이며 권한 상승 위험이 있으므로 리팩터링 전에 먼저 확인해야 합니다.

### 큰 서비스 분리

- `ParticipantService`: 참여, 출석 코드, 출석 마감, 포인트 지급, 관리자 출석 보정까지 담당합니다.
- `PostService`: 게시글 목록, 수정, 삭제, 첨부파일 삭제, 페이서 재배정까지 담당합니다.
- `RegularPostService`, `TrainingPostService`, `EventPostService`, `FlashPostService`: 이미지 업로드, 첨부파일 업로드, 댓글 트리 변환 로직이 반복됩니다.

분리 후보:

- `AttendanceService`
- `ParticipationService`
- `PointGrantService`
- `PostAttachmentService`
- `CommentTreeReader`
- `PacerAssignmentService`
- `PostQueryService`
- `PostUpdateService`
- `PostDeleteService`

### 쿼리/성능 개선 후보

- `UserPointService`와 `AdminService`는 사용자 목록을 가져온 뒤 사용자별 포인트 합계/참여 횟수를 반복 조회합니다.
- 데이터가 늘면 N+1 조회 문제가 커질 수 있습니다.
- aggregate query 또는 projection 기반 조회로 개선하는 것이 좋습니다.

### 데이터/동작 위험 후보

- `CommentService.deleteComment`는 soft delete로 status만 변경하지만, 댓글 조회 쪽은 status 필터를 확인하지 않습니다.
- 삭제 댓글이 다시 보일 가능성이 있습니다.
- `BaseResponseStatus`에서 `INVALID_POST_TYPE`과 `INVALID_RUN_TYPE`이 같은 `3010` 코드를 사용합니다.
- 파일 크기 메시지는 10MB/20MB라고 되어 있지만 실제 설정은 50MB 기준입니다.
- `UserPointService.EXCLUDED_USER_IDS`는 사용자 ID 하드코딩입니다.
- `JwtAuthenticationFilter`는 debug 로그로 token 값을 출력합니다.
- `AllowedImageFileExtensions`는 현재 사용처가 확인되지 않습니다.

## 6. 우선순위

### High Priority

1. `/user/**` 보안 설정과 `AuthController.updateUserRole` 운영 필요성 확인
2. 리팩터링 전 최소 테스트 추가
3. 동작 변경 없는 package/import 이동부터 진행

### Medium Priority

1. `ParticipantService`, `PostService` 책임 분리
2. 댓글 soft delete 조회 필터 검토
3. 포인트/랭킹/관리자 목록 조회 쿼리 최적화

### Low Priority

1. DTO 이름/위치 정리
2. unused field/import 제거
3. 응답 message Map을 명시적 DTO로 변경
4. 인코딩/주석 정리
5. `AllowedImageFileExtensions` 제거 또는 실제 검증 로직에 적용

## 7. 안전한 단계별 진행 계획

### Step 1. 현재 기준선 확보

- 목표: 빌드/테스트 가능 상태 확인
- 대상 파일: 없음
- 검증: `.\gradlew.bat compileJava`, `.\gradlew.bat test`
- 위험도: 낮음

### Step 2. 도메인 구조 확정

- 목표: 최종 패키지 구조 합의
- 대상 파일: 없음
- 검증: 문서 리뷰
- 위험도: 낮음

### Step 3. global/config/security/infra 이동

- 목표: 비즈니스 도메인과 공통 인프라 분리
- 대상 파일: `Config/*`, `Security/*`, `Util/*`, `Controller/Advice/*`, `Dto/Common/*`, `Entity/Base/*`, `S3Uploader`
- 검증: `.\gradlew.bat compileJava`, `.\gradlew.bat test`
- 위험도: 중

### Step 4. auth/user 이동

- 목표: 인증과 사용자 도메인 분리
- 대상 파일: `AuthController`, `AuthService`, `UserController`, `UserService`, `UserRepository`, `User`, `UserRole`, 관련 DTO
- 검증: 회원가입, 로그인, 프로필 조회/수정 API smoke test
- 위험도: 중

### Step 5. run.post/run.home/run.pacer 이동

- 목표: 런 게시글 중심 도메인 분리
- 대상 파일: `Controller/Post/*`, `Service/Post/*`, `HomeController`, `HomeService`, post/pacer/attachment entity/repository/dto
- 검증: 게시글 목록/상세/생성/수정/삭제 API smoke test
- 위험도: 중~높음

### Step 6. run.participant/run.comment 이동

- 목표: 참여/출석/댓글 도메인 분리
- 대상 파일: `ParticipantController`, `ParticipantService`, `CommentController`, `CommentService`, participant/comment entity/repository/dto
- 검증: 참여, 출석 코드 생성, 출석, 마감, 댓글 생성/삭제 API smoke test
- 위험도: 중

### Step 7. calendar/point/admin/season 이동

- 목표: 나머지 도메인 정리
- 대상 파일: `Calendar*`, `UserPoint*`, `Admin*`, `SeasonProvider`, 관련 DTO/entity/repository
- 검증: 캘린더, 랭킹, 마이페이지 참여 내역, 관리자 API smoke test
- 위험도: 중

### Step 8. 중복 로직 추출

- 목표: package 이동 후 서비스 책임 분리
- 대상 파일: `PostService`, `RegularPostService`, `TrainingPostService`, `EventPostService`, `FlashPostService`, `ParticipantService`
- 검증: 기존 API 응답 비교, 서비스 단위 테스트
- 위험도: 중~높음

## 8. 테스트 및 검증 계획

현재 확인된 테스트는 `ServerApplicationTests` 하나뿐입니다. 리팩터링 전 다음 테스트를 우선 추가하는 것이 안전합니다.

- Auth: 로그인 성공/실패, JWT 발급
- User: 회원가입 중복 학번, 프로필 조회/수정, 일일 출석 중복
- Post: 정규/훈련/행사/번개 생성, 상세 조회, 수정, 삭제
- Participant: 참여/취소, 출석 코드 생성, 출석, 마감 시 포인트 지급
- Comment: 댓글/대댓글 생성, 삭제 댓글 조회 제외
- Point: 랭킹 동점 처리, 제외 사용자 처리, 기간별 이벤트 랭킹
- Security: 인증이 필요한 `/user/profile`, `/user/attend`, `/admin`, `/run/**` 접근 제어

실행 명령:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat test
.\gradlew.bat build
```

DB/S3/JWT 환경변수 때문에 테스트 실행이 실패할 수 있습니다. 테스트용 H2/profile/mock S3 구성은 별도 확인이 필요합니다.

## 9. 리팩터링 전 확인 필요 사항

- `/user/{userId}/role` API가 운영에서 필요한지, 테스트용 잔재인지
- `/user/**` 전체 permitAll이 의도된 설정인지
- `domains` 빈 디렉터리를 그대로 사용할지 새 구조로 정리할지
- 도메인 명칭을 `run` 중심으로 할지 `post`, `attendance` 등으로 더 나눌지
- `AdminService.getPacers()`가 admin 도메인인지 pacer 도메인인지
- 삭제 댓글을 API 응답에서 숨겨야 하는지
- 하드코딩된 제외 사용자 ID가 운영 정책인지 임시 코드인지
- 테스트용 local profile/H2/mock S3 구성을 추가해도 되는지

## 10. 최종 권장 진행 순서

1. 보안 설정과 테스트용 권한 변경 API를 먼저 리뷰합니다.
2. 최소한의 compile/test 기준선을 확보합니다.
3. 동작 변경 없이 `global`, `security`, `infra`부터 이동합니다.
4. `auth`, `user`를 이동합니다.
5. `run.post`, `run.pacer`, `run.home`을 이동합니다.
6. `run.participant`, `run.comment`를 이동합니다.
7. `calendar`, `point`, `admin`을 이동합니다.
8. package 이동 완료 후 중복 로직 추출과 서비스 분리를 별도 커밋으로 진행합니다.
9. 보안 수정, 댓글 soft delete 필터, 포인트 쿼리 최적화는 패키지 이동과 섞지 않고 별도 변경으로 처리합니다.
