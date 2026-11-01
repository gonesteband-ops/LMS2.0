---
name: flyway-hibernate-schema-drift
description: Fix Flyway migration failures caused by schema drift between Hibernate ddl-auto and production ddl-none environments.
source: auto-skill
extracted_at: '2026-07-21T03:52:12.416Z'
---

# Fixing Flyway ↔ Hibernate Schema Drift

## When to Apply
- Flyway migration fails with "relation already exists" or "column does not exist"
- The app worked in development (Hibernate `ddl-auto: update`) but crashes in production (`ddl-auto: none`)
- A Docker volume has stale schema created by Hibernate that doesn't match Flyway migrations

## Root Cause
Hibernate's `ddl-auto: update` silently adds/removes columns to match JPA entities. Flyway migrations are static SQL. When the DB volume was created by Hibernate, the schema may have columns that Flyway doesn't know about, or be missing columns that Flyway expects later migrations to add.

## Diagnostic Procedure

### Step 1: Identify the Failing Migration
Check container logs for:
```
Migration V#__xxx.sql failed
Message: ERROR: relation "idx_xxx" already exists
Message: ERROR: column xxx does not exist
```

### Step 2: Compare Entity vs Migration SQL
For the failing table, compare:
- The JPA `@Entity` class fields (what Hibernate expects)
- The Flyway `CREATE TABLE` in the original migration
- Any `ALTER TABLE` in subsequent migrations

Common gaps: columns added to the entity over time (like `deleted_at`) that were never added via migration because Hibernate handled it.

### Step 3: Make Migrations Idempotent
Rewrite the failing migration to handle both fresh and existing schemas:

```sql
-- Instead of: CREATE INDEX idx_foo ...
CREATE INDEX IF NOT EXISTS idx_foo ...

-- Instead of: DROP INDEX idx_old; CREATE INDEX idx_new ...
DROP INDEX IF EXISTS idx_old;
DROP INDEX IF EXISTS idx_new;
CREATE INDEX idx_new ...

-- Instead of: ALTER TABLE x ALTER COLUMN y TYPE jsonb USING y::jsonb;
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'x' AND column_name = 'y' AND data_type = 'text'
    ) THEN
        ALTER TABLE x ALTER COLUMN y TYPE jsonb USING y::jsonb;
    END IF;
END $$;

-- Add missing columns
ALTER TABLE x ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
```

### Step 4: Add Missing Columns via New Migration
If a column exists in the entity but not in any migration, create a new migration:
```sql
-- V19__agregar_deleted_at_exportaciones.sql
ALTER TABLE exportaciones ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
```

### Step 5: Handle Flyway Checksum Changes
After editing an existing migration, the checksum changes. Ensure the production profile has:
```yaml
spring:
  flyway:
    repair-on-migrate: true
    validate-on-migrate: false
```

## Prevention Rules
1. **Never rely on `ddl-auto: update` for schema evolution** — it creates invisible drift
2. **Every entity field must have a corresponding migration** — if you add a column to an entity, add a `V#__*.sql` migration
3. **Use `IF NOT EXISTS` / `IF EXISTS` in all migrations** — makes them safe to re-run
4. **Test with `ddl-auto: none` locally** before deploying to catch missing migrations early
5. **Keep `baseline-version` in sync** — if baseline is V3, ensure V1-V3 were actually applied to the DB
