# Spring Plus

## 주요 기능

### 1. 인증 및 사용자 관리
- 회원 가입 및 로그인 (JWT 기반)
- 사용자 역할 관리 (USER, ADMIN)
- 비밀번호 암호화 (BCrypt)
- 관리자 전용 사용자 역할 변경

### 2. Todo 관리
- Todo 생성, 조회
- 날씨 정보 자동 포함
- 다양한 조건으로 검색 (제목, 생성일, 담당자)
- 페이징 처리

### 3. 담당자 관리
- Todo에 담당자 할당
- 담당자 목록 조회 및 삭제
- N+1 문제 해결을 통한 효율적인 조회

### 4. 댓글 기능
- Todo에 댓글 작성
- 댓글 목록 조회
- 작성자 정보 함께 조회 (fetchJoin)

## 프로젝트 구조

```
src/main/java/org/example/expert/
├── ExpertApplication.java
├── aop/
│   └── AdminAccessLoggingAspect.java          # 관리자 접근 로깅
├── client/
│   ├── WeatherClient.java                     # 날씨 API 클라이언트
│   └── dto/WeatherDto.java
├── config/
│   ├── GlobalExceptionHandler.java            # 전역 예외 처리
│   ├── JwtAuthenticationFilter.java           # JWT 인증 필터
│   ├── JwtUtil.java                           # JWT 유틸리티
│   ├── PersistenceConfig.java                 # JPA Auditing 설정
│   ├── QueryDslConfig.java                    # QueryDSL 설정
│   └── SecurityConfig.java                    # Spring Security 설정
└── domain/
    ├── auth/                                   # 인증
    ├── comment/                                # 댓글
    ├── manager/                                # 담당자
    ├── todo/                                   # 할일
    ├── user/                                   # 사용자
    └── common/                                 # 공통
```

## API 명세

### 인증 API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/auth/signup` | 회원가입 | No |
| POST | `/auth/signin` | 로그인 | No |

### Todo API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/todos` | Todo 생성 | Yes |
| GET | `/todos` | Todo 목록 조회 (필터: weather, startDate, endDate) | Yes |
| GET | `/todos/{todoId}` | Todo 단건 조회 | Yes |
| GET | `/todos/search` | Todo 검색 (QueryDSL) | Yes |

### 댓글 API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/todos/{todoId}/comments` | 댓글 작성 | Yes |
| GET | `/todos/{todoId}/comments` | 댓글 목록 조회 | Yes |

### 담당자 API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/todos/{todoId}/managers` | 담당자 추가 | Yes |
| GET | `/todos/{todoId}/managers` | 담당자 목록 조회 | Yes |
| DELETE | `/todos/{todoId}/managers/{managerId}` | 담당자 삭제 | Yes |

### 사용자 API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/users/{userId}` | 사용자 조회 | Yes |
| PUT | `/users` | 비밀번호 변경 | Yes |

### 관리자 API

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| PATCH | `/admin/users/{userId}` | 사용자 역할 변경 | ADMIN |

## 구현 특징

### 1. Spring Security + JWT 인증
```java
// Stateless 세션 관리
http.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

// JWT 필터 적용
http.addFilterBefore(jwtAuthenticationFilter,
    UsernamePasswordAuthenticationFilter.class);
```

- JWT 토큰 기반 무상태 인증
- Bearer Token 방식
- 토큰 유효 시간: 60분
- 역할 기반 접근 제어 (ADMIN, USER)

### 2. QueryDSL을 활용한 동적 쿼리
```java
// 복합 검색 조건
BooleanBuilder builder = new BooleanBuilder();
if (title != null) {
    builder.and(todo.title.containsIgnoreCase(title));
}
if (startDate != null && endDate != null) {
    builder.and(todo.createdAt.between(startDate, endDate));
}
```

- Custom Repository 패턴
- 동적 검색 조건 처리
- Projection을 통한 DTO 직접 조회
- 집계 함수 활용 (담당자 수, 댓글 수)

### 3. N+1 문제 해결
```java
// fetchJoin을 통한 N+1 방지
@Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user WHERE t.id = :id")
Optional<Todo> findByIdWithUser(@Param("id") Long id);

// QueryDSL fetchJoin
query.select(todo)
     .leftJoin(todo.user, user).fetchJoin()
     .where(todo.id.eq(todoId));
```

- LEFT JOIN FETCH 사용
- QueryDSL fetchJoin 적용
- 연관 엔티티를 한 번의 쿼리로 조회

### 4. JPA Cascade 설정
```java
@OneToMany(mappedBy = "todo", cascade = CascadeType.REMOVE)
private List<Comment> comments;  // Todo 삭제 시 댓글도 삭제

@OneToMany(mappedBy = "todo", cascade = CascadeType.PERSIST)
private List<Manager> managers;  // Todo 생성 시 담당자 자동 저장
```

### 5. AOP를 활용한 관리자 접근 로깅
```java
@Before("execution(* ..UserAdminController.changeUserRole(..))")
public void logBeforeChangeUserRole(JoinPoint joinPoint) {
    // 관리자의 사용자 역할 변경 요청 로깅
    log.info("User ID: {}, Request Time: {}, URL: {}", ...);
}
```

- 민감한 관리자 작업 추적
- 감사 추적(Audit Trail) 구현

## 엔티티 관계도

```
User (1) ──< (N) Todo
     └──< (N) Comment
     └──< (N) Manager

Todo (1) ──< (N) Comment (cascade: REMOVE)
     └──< (N) Manager (cascade: PERSIST)

Manager (N) >── (1) Todo
        └>── (1) User

Comment (N) >── (1) Todo
        └>── (1) User
```


## 과제 이력

| Level | 주제 | 구현 내용 |
|-------|------|---------|
| Lv3 | QueryDSL 검색 | 동적 쿼리, 복합 검색, 집계함수 |
| Lv2 | Spring Security | JWT 기반 인증, 권한 관리 |
| Lv2 | QueryDSL | N+1 해결, fetchJoin, Custom Repository |
| Lv2 | N+1 | LEFT JOIN FETCH, 쿼리 최적화 |
| Lv2 | JPA Cascade | PERSIST, REMOVE 설정 |
| Lv1 | AOP | 관리자 접근 로깅 |
| Lv1 | 테스트 코드 | WebMvcTest, MockMvc |
| Lv1 | JPA | 엔티티 관계 설정 |
| Lv1 | JWT | 토큰 생성, 검증 |
| Lv1 | Transactional | 읽기/쓰기 트랜잭션 분리 |

