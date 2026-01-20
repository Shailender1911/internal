# 🚀 Deployment Strategies - Complete Guide

## Overview

Deployment strategy determines HOW you release new code to production. The choice depends on:
- Risk tolerance
- Downtime requirements
- Rollback needs
- Infrastructure capabilities

---

## 1. Big Bang Deployment (Recreate)

### What is it?
Shut down old version completely → Deploy new version → Start new version

```
[Old Version v1] ──STOP──> [Downtime] ──START──> [New Version v2]
```

### Characteristics
| Aspect | Details |
|--------|---------|
| **Downtime** | YES - during deployment |
| **Risk** | HIGH - all users affected if issues |
| **Rollback** | Slow - need to redeploy old version |
| **Complexity** | LOW - simple to implement |
| **Cost** | LOW - no extra infrastructure |

### When to use?
- Development/staging environments
- Non-critical applications
- Scheduled maintenance windows acceptable
- Small applications with minimal traffic

### Example (Kubernetes)
```yaml
spec:
  replicas: 3
  strategy:
    type: Recreate  # All pods killed, then new ones created
```

### Real-world example from PayU:
> "For internal admin tools, we use recreate deployment during off-hours (2 AM IST) when no one is using the system."

---

## 2. Rolling Deployment

### What is it?
Gradually replace old instances with new ones, one at a time.

```
Time 0: [v1] [v1] [v1] [v1]
Time 1: [v2] [v1] [v1] [v1]  ← 1 new instance
Time 2: [v2] [v2] [v1] [v1]  ← 2 new instances
Time 3: [v2] [v2] [v2] [v1]  ← 3 new instances
Time 4: [v2] [v2] [v2] [v2]  ← Complete
```

### Characteristics
| Aspect | Details |
|--------|---------|
| **Downtime** | NO - zero downtime |
| **Risk** | MEDIUM - gradual exposure |
| **Rollback** | MEDIUM - can stop and roll back |
| **Complexity** | MEDIUM |
| **Cost** | LOW - reuses same infrastructure |

### Configuration options
```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # Max extra pods during update
      maxUnavailable: 0  # Zero downtime
```

### When to use?
- Most production deployments
- Stateless applications
- When you need zero downtime but simple setup

### Real-world example from PayU:
> "ZipCredit backend uses rolling deployment. We deploy to 1 pod first, run health checks, then continue to others. If health check fails, deployment stops automatically."

### Challenges
- **Both versions run simultaneously** - need backward compatibility
- **Database migrations** - must be backward compatible
- **Session handling** - sessions must work across versions

---

## 3. Blue-Green Deployment

### What is it?
Maintain TWO identical production environments. Switch traffic instantly.

```
                    ┌─────────────┐
                    │   Router    │
                    │ (100% Blue) │
                    └──────┬──────┘
                           │
           ┌───────────────┴───────────────┐
           │                               │
    ┌──────▼──────┐                 ┌──────▼──────┐
    │    BLUE     │                 │    GREEN    │
    │   (v1)      │                 │    (v2)     │
    │   ACTIVE    │                 │   STANDBY   │
    └─────────────┘                 └─────────────┘

After switch:
                    ┌─────────────┐
                    │   Router    │
                    │(100% Green) │
                    └──────┬──────┘
                           │
           ┌───────────────┴───────────────┐
           │                               │
    ┌──────▼──────┐                 ┌──────▼──────┐
    │    BLUE     │                 │    GREEN    │
    │   (v1)      │                 │    (v2)     │
    │   STANDBY   │                 │   ACTIVE    │
    └─────────────┘                 └─────────────┘
```

### Characteristics
| Aspect | Details |
|--------|---------|
| **Downtime** | NO - instant switch |
| **Risk** | LOW - instant rollback |
| **Rollback** | INSTANT - just switch back |
| **Complexity** | HIGH - maintain 2 environments |
| **Cost** | HIGH - 2x infrastructure |

### When to use?
- Critical applications
- When instant rollback is essential
- Large releases with significant changes
- Compliance requirements

### Real-world example:
> "For major releases (quarterly), Orchestration uses blue-green. We deploy to green, run smoke tests, then switch DNS. If issues, switch back in seconds."

### Implementation approaches
1. **DNS switching** - Change DNS to point to new environment
2. **Load balancer** - Switch backend pool
3. **Router/Gateway** - Change routing rules

---

## 4. Canary Deployment

### What is it?
Release to a SMALL percentage of users first, monitor, then gradually increase.

```
Phase 1: 5% traffic to v2
┌─────────────┐
│   Router    │
│  5% → v2    │
│ 95% → v1    │
└─────────────┘

Phase 2: 25% traffic to v2
┌─────────────┐
│   Router    │
│ 25% → v2    │
│ 75% → v1    │
└─────────────┘

Phase 3: 100% traffic to v2
┌─────────────┐
│   Router    │
│ 100% → v2   │
└─────────────┘
```

### Characteristics
| Aspect | Details |
|--------|---------|
| **Downtime** | NO |
| **Risk** | VERY LOW - limited blast radius |
| **Rollback** | EASY - route traffic back |
| **Complexity** | HIGH - traffic splitting, monitoring |
| **Cost** | MEDIUM - extra instances for canary |

### Traffic splitting strategies
```yaml
# Istio example
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
spec:
  http:
  - route:
    - destination:
        host: my-service
        subset: v1
      weight: 95
    - destination:
        host: my-service
        subset: v2
      weight: 5
```

### Canary criteria for promotion
- Error rate < 1%
- Latency p99 < 200ms
- No increase in 5xx errors
- Business metrics stable

### When to use?
- High-traffic applications
- When you need real user validation
- Risk-averse environments
- A/B testing scenarios

---

## 5. A/B Testing Deployment

### What is it?
Route specific USER SEGMENTS to different versions based on criteria.

```
┌─────────────────────────────────┐
│            Router               │
│  User ID % 10 < 2 → v2 (20%)   │
│  User ID % 10 >= 2 → v1 (80%)  │
└─────────────────────────────────┘
```

### Difference from Canary
| Canary | A/B Testing |
|--------|-------------|
| Random traffic split | User-based split |
| Deployment validation | Feature validation |
| Technical metrics | Business metrics |
| Temporary | Can be permanent |

### Routing criteria
- User ID hash
- Geographic location
- User tier (premium vs free)
- Device type
- Custom attributes

### When to use?
- Feature experimentation
- Business metrics validation
- Personalization testing

---

## 6. Shadow Deployment (Dark Launch)

### What is it?
Deploy new version but DON'T serve responses. Mirror traffic for testing.

```
┌─────────────┐
│   Request   │
└──────┬──────┘
       │
       ├────────────────┐
       │                │ (mirror)
       ▼                ▼
┌──────────────┐  ┌──────────────┐
│   v1 (live)  │  │  v2 (shadow) │
│   RESPONDS   │  │  DISCARDED   │
└──────────────┘  └──────────────┘
```

### Characteristics
| Aspect | Details |
|--------|---------|
| **Downtime** | NO |
| **Risk** | ZERO - shadow doesn't affect users |
| **Complexity** | HIGH - traffic mirroring setup |
| **Cost** | HIGH - processing traffic twice |

### When to use?
- Major refactoring
- Performance testing with real traffic
- Database migration validation
- New service replacing old one

### Real-world example:
> "When migrating DLS NACH from monolith, we ran shadow mode for 2 weeks. Mirrored 100% traffic to new service, compared responses, fixed discrepancies."

---

## 7. Feature Toggles (Feature Flags)

### What is it?
Deploy code but control activation via configuration.

```java
// Code is deployed but inactive
if (featureToggle.isEnabled("NEW_PAYMENT_FLOW", userId)) {
    return newPaymentFlow.process(request);
} else {
    return oldPaymentFlow.process(request);
}
```

### Types of feature flags
| Type | Purpose | Lifespan |
|------|---------|----------|
| **Release toggle** | Dark launch new features | Short |
| **Experiment toggle** | A/B testing | Medium |
| **Ops toggle** | Kill switch for features | Long |
| **Permission toggle** | Premium features | Permanent |

### Tools
- LaunchDarkly
- Unleash
- ConfigCat
- Custom DB-based flags

### Real-world example from PayU:
```java
// From ConfigService
String enableNewFlow = configService.getConfig(tenantId, "ENABLE_NEW_NACH_FLOW");
if ("YES".equals(enableNewFlow)) {
    // New flow
}
```

---

## 📊 Comparison Matrix

| Strategy | Downtime | Risk | Rollback Speed | Cost | Complexity |
|----------|----------|------|----------------|------|------------|
| **Recreate** | YES | HIGH | Slow | Low | Low |
| **Rolling** | NO | Medium | Medium | Low | Medium |
| **Blue-Green** | NO | Low | Instant | High | High |
| **Canary** | NO | Very Low | Fast | Medium | High |
| **Shadow** | NO | Zero | N/A | High | High |
| **Feature Flag** | NO | Very Low | Instant | Low | Medium |

---

## 🏢 What PayU SMB Lending Uses

### Day-to-day deployments (ZipCredit, Orchestration)
- **Rolling deployment** with health checks
- Zero downtime
- Automated via Jenkins/ArgoCD

### Major releases (quarterly)
- **Blue-Green** for orchestration layer
- Full smoke test suite before switch

### New feature releases
- **Feature flags** via config table
- Gradual rollout per partner

### Critical changes (payment flows)
- **Canary** with 5% → 25% → 50% → 100%
- Monitor error rates at each phase

---

## 🎤 Interview Answer Template

> "For deployment, we primarily use **rolling deployment** for day-to-day releases because it provides zero downtime with minimal infrastructure overhead.
>
> For major releases, we use **blue-green** because we can run full test suites on the green environment and switch instantly, with instant rollback if needed.
>
> For risky changes like payment flow modifications, we use **canary deployment** - starting with 5% traffic, monitoring error rates and latency, then gradually increasing.
>
> We also use **feature flags** extensively - code is deployed but features are activated per-tenant via database configuration. This gives us granular control without redeployment."

---

## 📚 Resources

- [Martin Fowler - Blue Green Deployment](https://martinfowler.com/bliki/BlueGreenDeployment.html)
- [Kubernetes Deployment Strategies](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
- [Istio Traffic Management](https://istio.io/latest/docs/concepts/traffic-management/)
