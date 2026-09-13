# Kheera Backend Testing Strategy

## Windows Docker Desktop readiness and recovery

An installed Docker CLI does not prove that the engine is running. Closing the
Desktop window can leave the engine running; quitting Desktop can stop it.
Treat a successful `docker info` Server response as the readiness check, even
if `docker desktop status` disagrees. Do this before PostgreSQL tests or local
container startup, not after a Maven failure.

1. Run `docker info`. If the sandbox reports access denied, a forbidden socket,
   or inaccessible named pipe, retry with the approved host/user permissions.
   Per-user Docker access failures are not proof that Docker is absent.
2. If the engine is stopped or its pipe is missing under the host account, run
   `docker desktop start --timeout 60`. Starting the already-installed local
   development runtime is routine environment preparation; do not ask the owner
   to open the app manually before trying this recovery.
3. If the Desktop CLI plugin is unavailable, locate the installed executable
   under Program Files or the user's local Programs directory. Verify the path
   exists, then use PowerShell `Start-Process -FilePath <verified-path>
   -WindowStyle Hidden`. Do not guess a path and report it as verified.
4. Poll `docker info` every five seconds for at most two minutes, using short
   calls so progress remains visible. Require a successful Server response and
   Linux engine before running PostgreSQL Testcontainers. Inspect
   `docker context ls` and `docker context inspect` if the selected endpoint is
   wrong; do not reset Docker, delete data, or change unrelated workloads.
5. Run `.\mvnw.cmd clean verify` from the same host/user environment that passed
   `docker info`. Require all tests to execute with zero skips. An API-version
   mismatch after connection is a separate compatibility problem (see below).

If startup times out, inspect Desktop diagnostics and report the exact failure
and attempted recovery. Missing installation, virtualization/WSL faults, and
permissions that remain unavailable are blockers; installation requires owner
permission. Never replace PostgreSQL, skip tests, or weaken the verification
gate to compensate. Do not stop a healthy engine merely to test these steps.

Validated on 2026-09-13: the installed CLI exposes `docker desktop start
--timeout`; host-account `docker info` reaches the Linux engine. Desktop status
reported stopped while the engine answered, so engine readiness takes priority.

## Docker API Compatibility

Use the Testcontainers 1.21.4 BOM for all Testcontainers modules. Version 1.20.4
sent Docker API 1.32, which the development host rejected (minimum 1.40) in
deployment run 34723314984 after PR #64 enabled tests. The upstream 1.21.4
release restores compatibility with recent Docker Engines:
https://github.com/testcontainers/testcontainers-java/releases/tag/1.21.4.
Keep PostgreSQL tests and clean builds enabled; do not lower the Docker daemon
API minimum or skip tests to work around client incompatibility. Validate with
`docker info` and `./mvnw clean verify`; dependency compatibility must be checked
on the deployment runner as well as the local machine. Tracked by #71.

## Purpose

This guide defines the ideal automated-test approach for the current Kheera
Spring Boot backend. It is intentionally based on the code that exists today:
authentication, OTPs, users, spaces, email processing, JPA repositories, the
JWT filter, security configuration, and CORS configuration.

The goal is confidence in behavior, not a large test count. Test public
behavior and important database rules. Do not write tests for Lombok getters,
Spring framework internals, or trivial delegation that has no business risk.

## Current Baseline

- The repository has unit, controller, security, worker, and PostgreSQL
  Testcontainers repository coverage for the current backend baseline.
- `spring-boot-starter-test` supplies JUnit Jupiter, Mockito, AssertJ, and the
  Spring test framework.
- Testcontainers dependencies are declared through the Testcontainers BOM.
- Deployment workflows run Maven package without test-skip flags before
  rebuilding containers. A failing test or package step must stop deployment.
- The backend uses PostgreSQL, Flyway, Hibernate validation, UUIDs, and JPQL
  projections. Repository tests must therefore use PostgreSQL, not H2.

## Testing Pyramid

```text
                 Few full API journeys
              SpringBootTest + MockMvc

          Repository and security integration tests
           PostgreSQL Testcontainers + Flyway

      Many fast unit tests: service rules, worker behavior,
      token logic, mapping, request validation helpers
```

| Layer | Main style | Database | Spring context | Primary purpose |
| --- | --- | --- | --- | --- |
| Service | JUnit 5 + Mockito | No | No | Business decisions and failure paths. |
| Repository | `@DataJpaTest` + Testcontainers | PostgreSQL | Narrow JPA slice | Query correctness, constraints, mappings. |
| Controller | `@WebMvcTest` + MockMvc | No | MVC slice | HTTP contract, JSON, status codes, validation. |
| Worker | JUnit 5 + Mockito; a few integration tests | Usually no | No | Queue selection, retries, persistence effects. |
| Middleware | Direct filter test + MVC security test | No | Small where needed | Authentication and CORS behavior. |
| Critical journey | `@SpringBootTest` + MockMvc + Testcontainers | PostgreSQL | Full | Components work together through real configuration. |

## Test Structure

Create packages that mirror production code:

```text
src/test/java/com/knightdevelopers/kheerabackend/
  support/
    PostgreSqlIntegrationTest.java
    TestDataFactory.java
    FixedClockConfiguration.java
  service/
    UserServiceTest.java
    OTPVerificationServiceTest.java
    AuthenticationServiceTest.java
    SpaceServiceTest.java
    EmailSenderServiceTest.java
  repository/
    UserRepositoryTest.java
    OtpRepositoryTest.java
    EmailRepositoryTest.java
    SpaceMembersRepositoryTest.java
  controller/
    AuthenticationControllerTest.java
    UserControllerTest.java
    SpaceControllerTest.java
    HealthControllerTest.java
  worker/
    EmailWorkerServiceTest.java
  security/
    JwtAuthenticationFilterTest.java
    SecurityConfigurationTest.java
    CorsConfigurationTest.java
  integration/
    AuthenticationJourneyIntegrationTest.java
    SpaceAccessIntegrationTest.java
```

Keep builders and fixture helpers under `support`. Fixtures should use explicit
values and names, for example `memberIn(space, user, role)`, instead of opaque
large object graphs in individual tests.

## Common Rules

### Write tests in Arrange, Act, Assert form

```java
// Arrange
when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

// Act
String token = userService.authenticateLoginRequest(request);

// Assert
assertThat(token).isNotBlank();
verify(authenticationService).generateToken(email);
```

Use descriptive names that state the observable result:

```text
authenticateLoginRequest_returnsToken_whenPasswordMatches
validateOtp_returnsFalse_whenLatestOtpIsExpired
getUserSpaces_returnsOnlyMembershipsOfAuthenticatedUser
processEmails_marksEmailFailed_afterFourthDeliveryFailure
```

### Test edge cases and authorization first

For every new endpoint or service method, cover:

1. Normal successful path.
2. Invalid or missing input.
3. Not found.
4. Unauthenticated request.
5. Authenticated but unauthorized request.
6. Duplicate/conflicting state where applicable.
7. Soft-deleted record behavior.

### Never make a test wait for time or a scheduler

- Call `EmailWorkerService.processEmails()` directly.
- Inject `Clock` into future OTP/JWT time-sensitive services rather than using
  `System.currentTimeMillis()` directly.
- Use a fixed clock in tests to make expiry deterministic.
- Do not use `Thread.sleep()`.

### Do not send real email or hit production services

- Mock `JavaMailSender` for `EmailSenderService` tests.
- Test the generated `SimpleMailMessage` contents and verify `send()`.
- Email worker tests mock `EmailSenderService`; they never call SMTP.

### Do not use an in-memory H2 database

Kheera's schema relies on PostgreSQL behavior such as `pgcrypto`, UUID defaults,
Flyway migrations, and PostgreSQL query behavior. Use a disposable PostgreSQL
Testcontainer for persistence and full-stack tests.

## Service Testing

### Style

Use pure JUnit 5 tests with Mockito. Do not use `@SpringBootTest` for a service
test.

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private OTPVerificationService otpVerificationService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationService authenticationService;

    @InjectMocks private UserService userService;
}
```

Prefer constructor injection in production code. It makes service dependencies
obvious and tests simple to construct. Avoid field injection in new code.

### `UserService`

Test these behaviors:

| Method / behavior | Essential tests |
| --- | --- |
| Login | Unknown email and wrong password fail; matching password returns token; token generation receives the stored email. |
| Signup | Existing email rejected; invalid OTP rejected; valid OTP saves a password-encoded user and returns token. |
| Password reset | Invalid OTP rejected; valid OTP replaces password with encoded value and returns token. |
| User list | Maps users to `UserResponse` without password/profile internals. |

Avoid asserting every repository interaction. Assert the saved user's meaningful
state: encoded password, email, and name.

### `OTPVerificationService`

Test these behaviors:

| Behavior | Essential tests |
| --- | --- |
| OTP generation | Each result is a six-digit number from `100000` through `999999`. |
| Signup OTP request | Saves OTP record with email, expiry, and queued email content. |
| Password-reset OTP request | Saves OTP record and reset-specific email subject/body. |
| Validation | No stored OTP fails; wrong OTP fails; latest matching OTP succeeds; expired latest OTP fails. |

The last case is a required regression test and currently reveals a defect:
`validateOtp()` compares only values and does not inspect `expiresAt`.

### `AuthenticationService`

Test:

- Generated JWT has expected email subject.
- A token created with the configured test secret validates.
- Altered token and token signed with a different secret fail validation.
- Expired token fails validation.
- `getEmailFromToken()` returns subject for a valid token and fails safely for an
  invalid token according to its defined contract.

Refactor the secret and token lifetime into constructor-provided configuration
or a configuration-properties object before broad test coverage. Avoid setting a
private `@Value` field through reflection in most tests.

### `SpaceService`

The current service only delegates to the repository. Keep a narrow unit test
only if it gains authorization/filtering/mapping behavior. Its highest-value
coverage today belongs in repository and controller tests.

### `EmailSenderService`

Mock `JavaMailSender` and assert one outgoing `SimpleMailMessage` contains:

- configured sender address;
- recipient email;
- subject; and
- body.

Also test and document what happens when `mailSender.send()` throws; the worker,
not this sender service, owns retry behavior.

## Repository Testing

### Style

Use `@DataJpaTest` against PostgreSQL Testcontainers. Do not mock repositories
when verifying query semantics.

Add Testcontainers using a BOM and test-scoped dependencies:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>testcontainers-bom</artifactId>
      <version>${testcontainers.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>junit-jupiter</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
```

Use one shared PostgreSQL container base class and register its connection with
`@DynamicPropertySource`. Ensure Flyway runs before tests. For `@DataJpaTest`,
prevent replacement with an embedded database:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SpaceMembersRepositoryTest extends PostgreSqlIntegrationTest {
}
```

Assert exceptions at the boundary exercised by the test. Direct `EntityManager`
operations emit Hibernate/JPA exceptions; Spring Data repository operations
translate them into Spring data-access exceptions. For the duplicate-membership
native insert, verify `ConstraintViolationException`, SQLSTATE `23505`, and
constraint name `uq_space_member` so another integrity failure cannot satisfy
the test.

OTP ordering coverage includes a same-email legacy row with a null timestamp,
which must not supersede a dated OTP. Queue coverage places excluded sent and
failed rows ahead of eligible rows, includes future and exact-cutoff rows, and
asserts the complete ordered twenty-row result. This makes each filter and the
window limit observable instead of allowing excluded rows to fall outside the
window accidentally.

### `UserRepository`

Test:

- `findByEmail()` returns the exact persisted user.
- unknown email returns empty.
- database unique constraint rejects duplicate email.
- soft-deleted user handling follows the chosen contract. If deleted users must
  not authenticate, add an explicit filtered query and tests.

### `OtpRepository`

Test:

- `findTop1ByEmailOrderByCreatedAtDesc()` returns only the newest OTP.
- unknown email returns an empty list.
- records for one email never appear in another user's result.
- table name and mapping validate against Flyway's `one_time_passwords` table.

This test suite should be added after resolving the current JPA mapping mismatch
between `OneTimePassword` and the Flyway table name.

### `EmailRepository`

Test the worker queue query precisely:

- only pending emails are returned;
- already sent and permanently failed emails are excluded;
- future `sendAt` records are excluded;
- urgent emails are before non-urgent emails;
- earliest scheduled email wins within the same urgency;
- result is limited to twenty rows.

Use explicit timestamps and more than twenty queued emails to validate both
ordering and limit.

### `SpaceMembersRepository`

This is a high-value repository test because it carries isolation rules.

Test:

- return only spaces belonging to the requested user;
- exclude soft-deleted memberships;
- return `SpaceListDto` with correct `id` and `name`;
- return an empty list for a user with no memberships;
- database unique constraint rejects duplicate `(user_id, space_id)` membership.

### `SpaceRepository`

It currently has no custom query. Do not add a test merely for inherited
`JpaRepository` methods. Add tests only with custom space reads/writes,
soft-delete filtering, or authorization-aware lookup methods.

## Controller Testing

### Style

Use `@WebMvcTest` and `MockMvc`. Mock the controller's service dependencies,
not repositories. These tests verify HTTP behavior, JSON, status codes,
security, and error messages without starting PostgreSQL.

```java
@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private UserService userService;
    @MockBean private OTPVerificationService otpVerificationService;
}
```

When custom JWT/security configuration prevents an isolated controller test,
either import only the required security configuration or use a full
integration test. Do not disable security globally just to make a test pass.

### `AuthenticationController`

Test every public endpoint contract:

| Endpoint | Required coverage |
| --- | --- |
| `POST /api/auth/login` | Valid credentials return raw token `200`; service exception returns `401` and `Invalid Email Or Password`. |
| `POST /api/auth/signup-email` | Duplicate email returns `400 Email already registered!`; fresh email queues OTP and returns `200`. |
| `POST /api/auth/otp-validation` | Valid OTP returns `200 Valid OTP, Proceed`; invalid/expired returns `400`. |
| `POST /api/auth/signup` | Duplicate email and invalid OTP fail; successful request returns raw token. |
| `POST /api/auth/forgot-password` | Existing and unknown emails both return the same generic success message. |
| `POST /api/auth/reset-password` | Success returns token; validation/service failure returns `400`. |

Add Bean Validation annotations to request DTOs before testing missing/invalid
email and blank-password input. At present the DTOs have no validation rules,
so malformed JSON can reach service code.

### `UserController`

Test:

- authenticated `GET /api/users/me` returns only `id`, `name`, and `email`;
- missing/anonymous authentication returns `401`;
- valid token identity with missing user returns `400 User not found` under the
current contract, or the deliberate replacement status if the API is revised;
- `GET /api/users` requires authentication and does not expose password.

### `SpaceController`

Test:

- valid JWT subject email is resolved to the current user and returns spaces;
- no token receives `401`;
- authenticated user with no spaces receives `[]`;
- no code parses `Authentication.getName()` as a UUID when it contains email.

The final case protects issue #43 and should be implemented after the controller
is fixed.

### `HealthController` and `WeatherController`

- `HealthController`: one lightweight `200 Server is Healthy` test is enough.
- `WeatherController`: it is currently a demo endpoint. Either remove it or
  test its access-control and state behavior as long as it remains deployed.

## Background Worker Testing

### `EmailWorkerService`

Treat `processEmails()` as an ordinary method. The scheduler annotation only
decides when Spring calls it in production.

Use Mockito unit tests with mocked `EmailRepository` and `EmailSenderService`.

| Scenario | Expected result |
| --- | --- |
| No pending emails | Sender and save are never called. |
| Successful delivery | Sender called once; email saved with `isSent=true`, `isFailed=false`. |
| First failed delivery | `tries` increments; `isFailed=false`; email persisted. |
| Fourth failed delivery | `tries` becomes four; `isFailed=true`; email persisted. |
| One failed and one successful item | Processing continues independently; each email gets its own resulting state. |
| Repository queue query | Worker passes the current time and processes returned records only. |

Important hardening tests:

- Verify retries do not throw if `tries` is null. The database migration permits
  null, while `email.getTries() + 1` would throw. Prefer a non-null database
  default plus a defensive model default.
- Verify a save failure and clarify whether transaction rollback is desired.
- Verify scheduled processing is disabled in context tests with
  `spring.task.scheduling.enabled=false` unless a test intentionally exercises
  scheduling configuration.

One PostgreSQL integration test should persist a pending email, call
`processEmails()` directly with a mocked sender bean, and confirm the persisted
state transition.

## Middleware and Security Testing

### `JwtAuthenticationFilter`

Use direct unit tests with `MockHttpServletRequest`, `MockHttpServletResponse`,
`MockFilterChain`, and mocked `AuthenticationService`.

Test:

- no `Authorization` header: chain continues, no authentication is created;
- header without `Bearer ` prefix: chain continues, no authentication;
- invalid bearer token: chain continues, no authentication;
- valid bearer token: chain continues and security context contains email
  principal;
- Swagger/OpenAPI paths skip token processing according to the filter;
- security context is cleared in test setup/teardown so tests do not leak state.

The principal contract is important: the filter puts **email** in
`Authentication.getName()`. Services/controllers must not interpret it as a
UUID. Cover this in both direct filter tests and the `SpaceController` MVC test.

### `SecurityConfig`

Use a small integration/MVC security suite to verify real route behavior:

- `/api/auth/**` and `/api/health` are public;
- `/api/users` and `/api/spaces` reject missing token;
- valid bearer token reaches protected controller;
- invalid bearer token cannot grant access;
- Swagger paths follow the intended deployment policy.

Do not unit test Spring's `HttpSecurity` builder calls. Test resulting request
behavior.

### `CorsConfig`

Use MockMvc preflight requests to test:

- allowed frontend origin is accepted;
- unacceptable origin is not granted CORS headers;
- allowed methods and headers are correct;
- credential settings match the chosen frontend auth transport.

Keep production and development origins externalized by profile/environment.

## Full Integration Journeys

Use only a small number of full-context tests because they are slower. They run
against PostgreSQL Testcontainers, real Flyway migrations, real JPA mappings,
and MockMvc. Mock SMTP only.

### Authentication journey

```text
request signup OTP
  -> OTP and queued email are persisted
  -> submit valid OTP and signup details
  -> JWT returned
  -> GET /api/users/me with JWT returns the new user
```

### Space access journey

```text
persist user, second user, spaces, memberships, and roles
  -> authenticate first user
  -> GET /api/spaces
  -> response includes only first user's active memberships
```

These tests are where Flyway, JPA mapping, JWT filter, controller, service, and
repository assumptions meet. They should catch the defects that isolated mocks
cannot.

## Dependencies and Test Configuration

### Maven additions

- Keep `spring-boot-starter-test` for JUnit, Mockito, AssertJ, and Spring test
  slices.
- Add Testcontainers `junit-jupiter` and `postgresql` as test dependencies,
  versioned via the Testcontainers BOM.
- Add `junit-jupiter-params` only if it is not already resolved transitively and
  parameterized tests are introduced.
- Do not add an H2 dependency for persistence tests.

### Test properties

Create `src/test/resources/application-test.properties` with non-secret test
values. Database URL, username, and password come from Testcontainers at runtime
through dynamic properties.

```properties
jwt.secret=test-secret-must-be-at-least-32-bytes-long
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.show-sql=false
spring.task.scheduling.enabled=false
```

Use a test profile explicitly in integration tests. Never point tests at the
development or production PostgreSQL database.

## CI Policy

PR #63 supplies explicit localhost mail settings to the full-context test so
it starts without an ignored environment file or SMTP credentials. The MVC
security slice imports `TimeConfig` alongside the real `AuthenticationService`
to satisfy its clock dependency after merging the service-test baseline.
The current-user route is checked with missing, malformed, and invalid bearer
tokens. CORS tests exercise both an explicitly allowed origin and rejection of
an untrusted origin without credentialed response headers.
The complete clean verification passes 41 tests with zero skips; the shared
`Verify Backend` PR workflow runs the same command on Ubuntu with Docker.

### Mandatory Docker verification

Docker-backed tests must run locally and in CI whenever they are present on
the branch. Verify the engine with `docker info`, then run
`./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`) against the
committed test harness. Require zero failures, zero errors, and zero skipped
tests. Repeat clean verification after upstream merges and conflict resolution.
Do not disable Testcontainers, exclude repository tests, use skip flags, or
replace the container connection with a local database to claim verification.
If Docker is unavailable or access is denied, fix the environment or report
the blocker; do not push or resolve review comments on compilation alone.
This requirement supersedes earlier Docker-unavailable verification exceptions.
Record actual test counts and the verified commit on the PR.

The backend deployment workflows run tests before deployment:

```text
./mvnw clean package
```

Clean compilation prevents stale tests in persistent deployment checkouts.
Test mail properties are supplied explicitly to the context test; no developer
environment file or SMTP credentials are required. Deployment workflows use
read-only repository permissions, pinned checkout actions, environment-scoped
execution, and non-overlapping concurrency groups for development and
production. Keep the following gates:

1. Pull request: `./mvnw clean verify` for unit, controller, security, worker,
   and PostgreSQL Testcontainers tests.
2. Protected branch and deployment runner: run Maven package with tests enabled
   before rebuilding containers.
3. Require the `Verify Backend` check on ordinary pull requests to `develop`
   through branch protection after the workflow PR merges.
4. Only deploy after required tests pass.
5. Keep a separate explicit deployment step; never treat a successful Docker
   build as a substitute for a test pass.

PR #62 adds `.github/workflows/verify.yml` to run the full Maven `verify`
lifecycle on a GitHub-hosted Ubuntu runner with Java 21 and Docker for pull
requests targeting `develop`. It uses the committed PostgreSQL 16 Testcontainers
harness. Issue #55 hardens the workflows with least-privilege permissions,
pinned actions, deployment environments, concurrency guards, and deployment of
the exact triggering GitHub SHA after environment approval; frontend PR checks
and repository branch-protection settings remain separate repository or admin
tasks.

On 2026-09-12, the committed PostgreSQL 16 Testcontainers harness passed
`mvnw.cmd clean verify` on local Docker Desktop: 34 tests, zero failures,
zero errors, zero skipped. Earlier diagnostic runs against a substitute
PostgreSQL instance are superseded by this Docker-backed verification.
The test environment needs Java 21 and Docker/Testcontainers support. Both are
available on the local Windows host as of 2026-09-12. Per-user Docker Desktop
may require running the command with access to the user's Docker named pipe.

## First Implementation Batch

Implement tests in this order:

1. Fix and test the JWT email-to-space-list flow.
2. Add `OTPVerificationServiceTest`, including expiry. Fix implementation until
   expired OTP test passes.
3. Add `UserServiceTest` for login, signup, and password reset.
4. Add PostgreSQL Testcontainers base class and repository tests for
   `SpaceMembersRepository`, `UserRepository`, and `EmailRepository`.
5. Add `EmailWorkerServiceTest` for success/retry/failure state transitions.
6. Add `AuthenticationControllerTest`, `UserControllerTest`, and
   `SpaceControllerTest`.
7. Add the two critical full integration journeys.
8. Enable these tests in CI before implementing broad project/task APIs.

## Definition of Done for a New Backend Feature

A backend feature is ready only when it includes:

- unit tests for non-trivial service rules;
- repository integration tests for custom queries and constraints;
- controller tests for the HTTP contract and authorization;
- migration test coverage when schema changes;
- at least one end-to-end journey when the feature crosses security, database,
  and HTTP boundaries; and
- passing tests in CI before deployment.

## Space Lifecycle Regression Coverage (#66)

`SpaceLifecycleServiceTest` covers creator bootstrap, field validation, PATCH
omission/null behavior and soft deletion with mocked collaborators.
`SpaceLifecycleControllerTest` runs the real JWT/security chain and checks statuses,
JSON errors, forged fields and caller-email delegation. PostgreSQL-backed
`SpaceLifecycleIntegrationTest` uses Flyway with real transactions to prove
bootstrap commit/rollback, scoped grants, soft-deleted access rejection, persisted
PATCH semantics and retained descendants. Run targeted classes during development,
then the complete `mvnw.cmd clean verify` with Docker and zero skipped tests.

Space lifecycle controller coverage also exercises PATCH preflight from configured and untrusted browser origins, including credentials and the allowed-method response.
