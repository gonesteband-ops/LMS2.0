---
name: debug-async-pipeline
description: Diagnostic methodology for "stuck in pending" bugs in Spring Boot async processing pipelines (Redis pub/sub, @Async workers, conditional config).
source: auto-skill
extracted_at: '2026-07-21T03:19:47.812Z'
---

# Debugging Async Pipeline "Stuck in Pending" Bugs

## When to Apply
- A UI shows a status like "pendiente"/"pending" that never progresses to "completado"/"completed"
- No error appears in the frontend or backend logs
- The system uses async processing (Redis pub/sub, @Async workers, message queues)

## Diagnostic Procedure

### Step 1: Trace the Full Async Chain
Map every hop in the pipeline:
```
Frontend (JS/HTMX) → REST Controller → Service (saves + publishes message)
  → Message Broker (Redis/RabbitMQ/etc) → Listener/Worker → Processing
  → Status update (DB) → Frontend polling detects completion
```
Read each file in order. Identify where the chain breaks.

### Step 2: Check Conditional Configuration
Look for `@ConditionalOnProperty`, `@ConditionalOnBean`, `@Profile`, or `@ConditionalOnClass` annotations on infrastructure config classes. Common trap:
```java
@ConditionalOnProperty(name = "spring.data.redis.host", havingValue = "localhost", matchIfMissing = false)
```
This silently disables ALL beans in the config class when the value doesn't match exactly. In Docker the host is the service name (e.g., `"redis"`), in cloud it's a URL — neither matches `"localhost"`.

### Step 3: Check Profile-Specific Overrides
Inspect `application.yml` for each profile (development, production, test). Look for:
- `spring.autoconfigure.exclude:` that disables auto-configuration (e.g., `RedisAutoConfiguration`)
- Properties set to empty values (`host:` with nothing after the colon)
- Properties that override the default with null/empty

### Step 4: Verify Field Injection Null Guards
Check if the service has null guards like `if (redisTemplate != null)` that silently skip critical operations. These masks make the bug invisible — no exception, just silent failure.

### Step 5: Verify Env Var → Property → Bean Wiring
Confirm the full chain:
```
docker-compose.yml (SPRING_DATA_REDIS_HOST=redis)
  → application.yml (${REDIS_HOST:localhost})
  → @Value / Spring Boot auto-config
  → RedisConnectionFactory bean
  → RedisTemplate bean
  → Injected into service
```

## Common Root Causes (by frequency)
1. `@ConditionalOnProperty` with `havingValue` matching only one environment
2. Production profile excluding auto-configuration classes
3. Missing or misnamed environment variables in docker-compose/deployment
4. `@Async` not enabled (`@EnableAsync` missing on main class)
5. Transaction race condition: message published before DB commit (solved by `TransactionSynchronization.afterCommit()`)

## Fix Pattern
- Remove overly restrictive conditionals; let Spring Boot auto-configuration handle connectivity
- Ensure all profiles define the required properties (use `${ENV_VAR:default}` pattern)
- Never exclude auto-configuration for infrastructure the app critically depends on
- Add startup logging that verifies infrastructure beans are initialized
