# 🧪 Testing Types - Complete Guide for SDE-3

## Testing Pyramid

```
                    ┌───────────┐
                   /   E2E      \     ← Slow, Expensive, Few
                  /    Tests     \
                 /───────────────\
                /   Integration   \   ← Medium speed, Medium cost
               /      Tests        \
              /─────────────────────\
             /        Unit           \  ← Fast, Cheap, Many
            /         Tests           \
           /───────────────────────────\
```

**Rule of thumb:**
- 70% Unit tests
- 20% Integration tests
- 10% E2E tests

---

## 1. Unit Testing

### What is it?
Test individual functions/methods in ISOLATION from dependencies.

### Characteristics
| Aspect | Details |
|--------|---------|
| **Scope** | Single function/method |
| **Dependencies** | Mocked/stubbed |
| **Speed** | Very fast (ms) |
| **Who writes** | Developers |
| **When runs** | Every commit, pre-push |

### Example (Java/JUnit)
```java
@Test
void shouldCalculateEMI_whenValidInput() {
    // Given
    double principal = 100000;
    double rate = 12.0;
    int tenure = 12;
    
    // When
    double emi = loanCalculator.calculateEMI(principal, rate, tenure);
    
    // Then
    assertEquals(8884.88, emi, 0.01);
}

@Test
void shouldThrowException_whenNegativePrincipal() {
    assertThrows(IllegalArgumentException.class, () -> {
        loanCalculator.calculateEMI(-100000, 12.0, 12);
    });
}
```

### What to test?
✅ Business logic calculations
✅ Validation methods
✅ Data transformations
✅ Edge cases (null, empty, boundary)
✅ Error handling

### What NOT to test at unit level?
❌ Database interactions
❌ External API calls
❌ File system operations
❌ Complex multi-class flows

### Mocking frameworks
- **Java**: Mockito, EasyMock
- **JavaScript**: Jest mocks, Sinon
- **Python**: unittest.mock, pytest-mock

### Example with mocking
```java
@Test
void shouldSendNotification_whenLoanApproved() {
    // Mock dependencies
    when(configService.getConfig(anyInt(), eq("NOTIFICATION_ENABLED")))
        .thenReturn("YES");
    when(notificationClient.send(any())).thenReturn(true);
    
    // Execute
    boolean result = loanService.approveLoan(applicationId);
    
    // Verify
    assertTrue(result);
    verify(notificationClient, times(1)).send(any());
}
```

---

## 2. Integration Testing

### What is it?
Test how multiple components work TOGETHER.

### Types of Integration Testing

#### 2.1 Component Integration
Test service layer with real repository (but test DB)

```java
@SpringBootTest
@Testcontainers
class LoanServiceIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Autowired
    private LoanService loanService;
    
    @Test
    void shouldCreateLoan_andPersistToDatabase() {
        // Given
        LoanRequest request = createValidLoanRequest();
        
        // When
        LoanResponse response = loanService.createLoan(request);
        
        // Then
        assertNotNull(response.getLoanId());
        
        // Verify in DB
        Optional<LoanEntity> entity = loanRepository.findById(response.getLoanId());
        assertTrue(entity.isPresent());
    }
}
```

#### 2.2 API Integration
Test REST endpoints with real HTTP calls

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class LoanControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void shouldReturn201_whenValidLoanRequest() {
        // Given
        LoanRequest request = createValidRequest();
        
        // When
        ResponseEntity<LoanResponse> response = restTemplate.postForEntity(
            "/api/v1/loans", request, LoanResponse.class);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getLoanId());
    }
}
```

#### 2.3 Contract Testing
Verify API contracts between services (Producer-Consumer)

```java
// Consumer side - Pact
@PactTestFor(providerName = "zipcredit-service")
class OrchestrationConsumerPactTest {
    
    @Pact(consumer = "orchestration")
    public RequestResponsePact createLoanPact(PactDslWithProvider builder) {
        return builder
            .given("application exists")
            .uponReceiving("create loan request")
            .path("/api/v4/loans")
            .method("POST")
            .willRespondWith()
            .status(201)
            .body(new PactDslJsonBody()
                .stringType("loan_id")
                .stringValue("status", "CREATED"))
            .toPact();
    }
}
```

### Characteristics
| Aspect | Details |
|--------|---------|
| **Scope** | Multiple components |
| **Dependencies** | Real (test instances) |
| **Speed** | Medium (seconds) |
| **Who writes** | Developers |
| **When runs** | PR merge, nightly |

---

## 3. End-to-End (E2E) Testing

### What is it?
Test complete user flows from UI to database.

### Example flow
```
User Login → Create Application → Upload Documents → 
KYC Verification → Loan Offer → Accept → Disbursement
```

### Tools
- **UI automation**: Selenium, Cypress, Playwright
- **API E2E**: Postman/Newman, REST Assured
- **Mobile**: Appium

### Example (Cypress)
```javascript
describe('Loan Application Flow', () => {
  it('should complete loan application successfully', () => {
    // Login
    cy.visit('/login');
    cy.get('[data-cy=email]').type('test@example.com');
    cy.get('[data-cy=password]').type('password123');
    cy.get('[data-cy=login-btn]').click();
    
    // Create application
    cy.get('[data-cy=new-application]').click();
    cy.get('[data-cy=pan]').type('ABCDE1234F');
    cy.get('[data-cy=submit]').click();
    
    // Verify
    cy.url().should('include', '/application/');
    cy.get('[data-cy=status]').should('contain', 'CREATED');
  });
});
```

### Characteristics
| Aspect | Details |
|--------|---------|
| **Scope** | Complete user journey |
| **Dependencies** | All real |
| **Speed** | Slow (minutes) |
| **Who writes** | QA / SDET |
| **When runs** | Pre-release, nightly |

---

## 4. Regression Testing

### What is it?
Re-run existing tests to ensure new changes don't break existing functionality.

### Types
| Type | Description | When |
|------|-------------|------|
| **Full regression** | All test cases | Major release |
| **Partial regression** | Affected areas only | Minor release |
| **Sanity regression** | Critical paths only | Hotfix |

### Automation strategy
```
Smoke Tests (5 min) → Runs on every commit
Sanity Suite (30 min) → Runs on PR merge
Full Regression (4 hrs) → Runs nightly
```

### Real-world from PayU:
> "Before every production deployment, QA runs regression suite. We have ~500 automated API tests + ~100 UI tests. Takes about 2 hours. Deployment blocked if failure > 0."

---

## 5. Smoke Testing

### What is it?
Quick sanity check to verify basic functionality works.

### Characteristics
- Shallow and wide (not deep)
- Fast execution (< 15 min)
- Critical paths only
- Run after deployment

### Example smoke test suite
```
1. Health check endpoint returns 200 ✓
2. Login works ✓
3. Create application returns 201 ✓
4. Get application returns data ✓
5. Database connectivity ✓
6. Redis connectivity ✓
7. External API (Digio) reachable ✓
```

### Real-world from PayU:
> "After every deployment, we run smoke tests. If any fail, we immediately rollback. Smoke suite has 20 tests, runs in 5 minutes."

---

## 6. Performance Testing

### Types of Performance Testing

#### 6.1 Load Testing
Test system under expected load

```yaml
# k6 load test
export default function() {
  http.post('https://api.example.com/loans', payload);
}

export let options = {
  vus: 100,          # 100 virtual users
  duration: '10m',   # for 10 minutes
};
```

#### 6.2 Stress Testing
Test system beyond normal capacity to find breaking point

```yaml
export let options = {
  stages: [
    { duration: '2m', target: 100 },   # Ramp up to 100
    { duration: '5m', target: 500 },   # Increase to 500
    { duration: '5m', target: 1000 },  # Increase to 1000
    { duration: '2m', target: 0 },     # Ramp down
  ],
};
```

#### 6.3 Spike Testing
Test sudden traffic spikes

```yaml
export let options = {
  stages: [
    { duration: '1m', target: 100 },
    { duration: '10s', target: 1000 },  # Sudden spike!
    { duration: '1m', target: 100 },
  ],
};
```

#### 6.4 Soak Testing (Endurance)
Test system over extended period for memory leaks

```yaml
export let options = {
  vus: 100,
  duration: '24h',  # 24 hours continuous
};
```

### Metrics to measure
| Metric | Target |
|--------|--------|
| **Response time (p50)** | < 100ms |
| **Response time (p95)** | < 500ms |
| **Response time (p99)** | < 1s |
| **Throughput** | > 1000 RPS |
| **Error rate** | < 1% |
| **CPU utilization** | < 70% |
| **Memory utilization** | < 80% |

### Tools
- k6, JMeter, Gatling, Locust
- APM: New Relic, Datadog, SigNoz

---

## 7. Security Testing

### Types

#### 7.1 Static Application Security Testing (SAST)
Analyze source code for vulnerabilities

**Tools**: SonarQube, Checkmarx, Fortify

```bash
# SonarQube scan
mvn sonar:sonar \
  -Dsonar.projectKey=zipcredit \
  -Dsonar.host.url=http://sonar.example.com
```

#### 7.2 Dynamic Application Security Testing (DAST)
Test running application for vulnerabilities

**Tools**: OWASP ZAP, Burp Suite

#### 7.3 Dependency Scanning
Check for vulnerable dependencies

```bash
# OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check
```

### Common security tests
| Test | What it checks |
|------|----------------|
| **SQL Injection** | `'; DROP TABLE users; --` |
| **XSS** | `<script>alert('xss')</script>` |
| **CSRF** | Token validation |
| **Auth bypass** | Accessing without token |
| **Sensitive data exposure** | PII in logs/responses |

### Real-world from PayU:
> "Every PR runs SonarQube. Security vulnerabilities block merge. DAST runs weekly against staging."

---

## 8. Chaos Engineering / Resilience Testing

### What is it?
Intentionally inject failures to test system resilience.

### Experiments
| Experiment | What it tests |
|------------|---------------|
| **Kill pod** | Auto-recovery, health checks |
| **Network latency** | Timeout handling |
| **Database outage** | Graceful degradation |
| **Memory pressure** | OOM handling |
| **CPU throttle** | Performance under load |

### Tools
- Chaos Monkey (Netflix)
- Gremlin
- LitmusChaos (Kubernetes)

### Example
```yaml
# LitmusChaos - Pod kill experiment
apiVersion: litmuschaos.io/v1alpha1
kind: ChaosEngine
spec:
  appinfo:
    appns: production
    applabel: "app=orchestration"
  experiments:
    - name: pod-delete
      spec:
        components:
          env:
            - name: TOTAL_CHAOS_DURATION
              value: '30'
```

---

## 9. User Acceptance Testing (UAT)

### What is it?
Business users validate the system meets requirements.

### Characteristics
| Aspect | Details |
|--------|---------|
| **Who performs** | Business users, Product team |
| **Environment** | UAT/Staging |
| **When** | Before production release |
| **Focus** | Business flows, usability |

### UAT checklist example
```
[ ] Loan application can be created by customer
[ ] Documents upload correctly
[ ] Offer displayed matches business rules
[ ] Loan agreement shows correct terms
[ ] Disbursement reflects in partner system
[ ] Reports show accurate data
```

---

## 10. API Testing

### What is it?
Test REST/GraphQL APIs directly (no UI).

### Tools
- Postman / Newman
- REST Assured (Java)
- pytest + requests (Python)

### What to test?
| Test Type | Example |
|-----------|---------|
| **Happy path** | Valid request → 200 |
| **Validation** | Invalid email → 400 |
| **Auth** | No token → 401, Wrong role → 403 |
| **Not found** | Invalid ID → 404 |
| **Error handling** | Server error → 500 with message |
| **Headers** | Content-Type, CORS |
| **Response schema** | JSON structure matches spec |

### Example (REST Assured)
```java
@Test
void shouldReturn400_whenInvalidPan() {
    given()
        .contentType(ContentType.JSON)
        .body("""
            {
                "pan": "INVALID",
                "mobile": "9876543210"
            }
            """)
    .when()
        .post("/api/v4/applications")
    .then()
        .statusCode(400)
        .body("error.code", equalTo("INVALID_PAN"))
        .body("error.message", containsString("PAN"));
}
```

---

## 📊 Testing Strategy by Development Phase

| Phase | Tests | Who | Automation |
|-------|-------|-----|------------|
| **Development** | Unit tests | Developer | Yes |
| **PR/Code Review** | Unit + Integration | Developer + CI | Yes |
| **Merge to main** | Regression suite | CI/CD | Yes |
| **Pre-deployment** | Smoke + Sanity | QA | Yes |
| **Post-deployment** | Smoke tests | CI/CD | Yes |
| **Weekly** | Full regression + Security | QA + Security | Yes |
| **Pre-release** | UAT + Performance | Business + QA | Partial |

---

## 🏢 What PayU SMB Lending Follows

### Developer responsibility
1. **Write unit tests** for all new code (>80% coverage)
2. **Run locally** before pushing
3. **Fix broken tests** immediately

### QA responsibility
1. **Integration tests** for API contracts
2. **E2E tests** for critical flows
3. **Regression suite** maintenance
4. **Performance testing** quarterly

### CI/CD Pipeline
```
PR Created
    ↓
Unit Tests (must pass) ← Block merge if fail
    ↓
Integration Tests
    ↓
SonarQube Scan ← Block if critical issues
    ↓
Merge to main
    ↓
Deploy to staging
    ↓
Smoke Tests ← Rollback if fail
    ↓
QA Sign-off
    ↓
Deploy to production
    ↓
Production Smoke Tests
```

---

## 🎤 Interview Answer Template

> "Our testing strategy follows the testing pyramid:
>
> **Unit tests (70%)**: Developers write these for all business logic. We aim for 80%+ coverage. Run on every commit, takes ~2 minutes.
>
> **Integration tests (20%)**: Test API contracts and database interactions. We use Testcontainers for realistic DB testing. Run on PR merge.
>
> **E2E tests (10%)**: QA maintains these for critical user journeys - loan creation, disbursement, repayment. Run nightly and before release.
>
> **For deployment validation**: We run smoke tests after every deployment. 20 tests, 5 minutes. Any failure triggers automatic rollback.
>
> **For releases**: Full regression (500 tests), performance testing with expected load, and UAT sign-off from business."

---

## 📚 Resources

- [Testing Pyramid - Martin Fowler](https://martinfowler.com/articles/practical-test-pyramid.html)
- [Test Driven Development](https://www.agilealliance.org/glossary/tdd/)
- [k6 Load Testing](https://k6.io/docs/)
- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
