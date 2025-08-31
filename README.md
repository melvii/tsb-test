TSB Banking API — Quick README

A minimal Spring Boot 3.3 app that models a small banking backend with JWT auth, OTP (Twilio Verify), encrypted account numbers, and a clean transfer ledger.

What this app does (in one glance)

AuthN/Z: JWT access + refresh tokens; roles (ROLE_CUSTOMER)
Password reset: SMS OTP via Twilio Verify → short-lived Reset JWT (typ=RESET) → change password
Data security: Account numbers encrypted at rest (AES-GCM via JPA converter) + HMAC-SHA256 blind index (account_number_hash) for fast lookups
Transfers: transfers (1 row per transfer) + txns (2 ledger lines, DEBIT/CREDIT)
Rate limiting: Bucket4j on login/OTP/reset endpoints
Auditing: Register/login/OTP/transfer/reset events recorded

Quick start
1) Prereqs

Java 21, Maven
Docker (for Postgres) or your own Postgres 16+

2) Run Postgres (compose)
# docker-compose.yml (db only)
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: tsb
      POSTGRES_USER: tsb
      POSTGRES_PASSWORD: tsbpass
    ports: ["5432:5432"]

3) Set env vars (.env or shell)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tsb
SPRING_DATASOURCE_USERNAME=tsb
SPRING_DATASOURCE_PASSWORD=tsb


APP_OTP_PROVIDER=twilio
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_VERIFY_SERVICE_SID=VAxxxxxxxxxxxxxxxxxxxxxxxxxxxx

4) Run the app
mvn clean spring-boot:run
# or build a jar: mvn -DskipTests package && java -jar target/*-SNAPSHOT.jar


Flyway runs automatically; spring.jpa.hibernate.ddl-auto=validate keeps schema honest.

Configuration (snippet)
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    open-in-view: false
    hibernate.ddl-auto: validate
  flyway.enabled: true

app:
  security.jwtSecretBase64: ${APP_SECURITY_JWT_SECRET_BASE64}
  crypto.hmacKeyB64: ${APP_CRYPTO_HMACKEYB64}
  otp.provider: ${APP_OTP_PROVIDER:twilio}
  otp.twilio:
    accountSid: ${TWILIO_ACCOUNT_SID}
    authToken: ${TWILIO_AUTH_TOKEN}
    verifyServiceSid: ${TWILIO_VERIFY_SERVICE_SID}
  cors.allowedOriginsCsv: http://localhost:5173,http://localhost:3000

How the security works (short)

Login → access (15m) + refresh (7d, stored in DB). Old refresh tokens for the user are revoked on login.

Password reset:

POST /api/auth/request-reset (anti-enumeration 200; sends OTP to stored E.164 phone)
POST /api/auth/verify-reset (Twilio Verify OK → returns resetToken JWT with typ=RESET, 10 min)
POST /api/auth/reset-password (consumes resetToken, updates password, deletes all refresh tokens)

Account number privacy: the plaintext is encrypted at rest; queries use HMAC hash so you never decrypt to compare.

Rate limiting: Bucket4j (e.g., 5/min per IP) on login/OTP/reset endpoints.

CORS: whitelisted origins from config.

Data model (minimal)

customers(id, full_name, email*, phone[E.164], password_hash, role, created_at)
accounts(id, customer_id, account_number_enc, account_number_hash*, type, balance, created_at)
transfers(id, created_at, from_account_id, to_account_id, amount, description, actor)
txns(id, created_at, account_id, transfer_id, kind[DEBIT|CREDIT], amount, description)
refresh_tokens(id, customer_id, token, expiry)
audit_events(id, created_at, actor, action, target, details jsonb)


Lookups by account number: compute hmacHex(plaintext) and query accounts.account_number_hash.

Key endpoints
Auth & Reset

POST /api/auth/register — {fullName,email,phone,password} (phone normalized to E.164)
POST /api/auth/login — {email,password} → {access,refresh}
POST /api/auth/refresh — {refreshToken} → new {access,refresh}
POST /api/auth/request-reset — {emailOrPhone} → always 200
POST /api/auth/verify-reset — {email, code} → {resetToken, expiresInSeconds}
POST /api/auth/reset-password — {resetToken, newPassword} → 200

Accounts & Transfers

GET /api/customers/{customerId}/accounts
POST /api/accounts/transfer — {fromAccount, toAccount, amount, description}
GET /api/accounts/{accountNumber}/transfers — unified view (both sides), masked numbers
GET /api/accounts/{accountNumber}/transactions?page=0&size=50 — account summary once + lean txn items
All “by account number” endpoints hash the plaintext on the server and query by account_number_hash.

Postman

Import the Updated Flow collection (register/login/refresh + reset flow + transfers).
Set variables: baseUrl, email, phone (E.164), accountNumberFrom/To.
The “Verify Reset” request captures resetToken automatically.

Twilio trial tip

If you see 21608 (unverified number), ensure:
Phone is E.164 (e.g., NZ 022… ⇒ +6422…, not +64022…)
he exact number is verified in the same project as your Account SID
DQ checks (quick)

-- Table: public.accounts

-- DROP TABLE IF EXISTS public.accounts;

CREATE TABLE IF NOT EXISTS public.accounts
(
    id bigint NOT NULL DEFAULT nextval('accounts_id_seq'::regclass),
    customer_id bigint NOT NULL,
    account_number_enc text COLLATE pg_catalog."default" NOT NULL,
    type character varying(32) COLLATE pg_catalog."default" NOT NULL,
    balance numeric(18,2) NOT NULL DEFAULT 0,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_number_hash character varying(64) COLLATE pg_catalog."default",
    CONSTRAINT accounts_pkey PRIMARY KEY (id),
    CONSTRAINT accounts_customer_id_fkey FOREIGN KEY (customer_id)
        REFERENCES public.customers (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.accounts
    OWNER to tsb;
-- Index: ux_accounts_acc_hash

-- DROP INDEX IF EXISTS public.ux_accounts_acc_hash;

CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_acc_hash
    ON public.accounts USING btree
    (account_number_hash COLLATE pg_catalog."default" ASC NULLS LAST)
    WITH (fillfactor=100, deduplicate_items=True)
    TABLESPACE pg_default;
-- Index: ux_accounts_accnum

-- DROP INDEX IF EXISTS public.ux_accounts_accnum;

CREATE UNIQUE INDEX IF NOT EXISTS ux_accounts_accnum
    ON public.accounts USING btree
    (account_number_enc COLLATE pg_catalog."default" ASC NULLS LAST)
    WITH (fillfactor=100, deduplicate_items=True)
    TABLESPACE pg_default;


    -- Table: public.audit_events

-- DROP TABLE IF EXISTS public.audit_events;

CREATE TABLE IF NOT EXISTS public.audit_events
(
    id bigint NOT NULL DEFAULT nextval('audit_events_id_seq'::regclass),
    event_time timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor character varying(255) COLLATE pg_catalog."default" NOT NULL,
    action character varying(64) COLLATE pg_catalog."default" NOT NULL,
    target character varying(255) COLLATE pg_catalog."default",
    details jsonb,
    CONSTRAINT audit_events_pkey PRIMARY KEY (id)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.audit_events
    OWNER to tsb;



-- Table: public.customers

-- DROP TABLE IF EXISTS public.customers;

CREATE TABLE IF NOT EXISTS public.customers
(
    id bigint NOT NULL DEFAULT nextval('customers_id_seq'::regclass),
    full_name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    email character varying(255) COLLATE pg_catalog."default" NOT NULL,
    phone character varying(32) COLLATE pg_catalog."default" NOT NULL,
    password_hash character varying(255) COLLATE pg_catalog."default" NOT NULL,
    role character varying(32) COLLATE pg_catalog."default" NOT NULL DEFAULT 'CUSTOMER'::character varying,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT customers_pkey PRIMARY KEY (id),
    CONSTRAINT customers_email_key UNIQUE (email),
    CONSTRAINT customers_phone_key UNIQUE (phone)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.customers
    OWNER to tsb;


-- Table: public.refresh_tokens

-- DROP TABLE IF EXISTS public.refresh_tokens;

CREATE TABLE IF NOT EXISTS public.refresh_tokens
(
    id bigint NOT NULL DEFAULT nextval('refresh_tokens_id_seq'::regclass),
    customer_id bigint NOT NULL,
    token character varying(600) COLLATE pg_catalog."default" NOT NULL,
    expiry timestamp without time zone NOT NULL,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_customer_id_fkey FOREIGN KEY (customer_id)
        REFERENCES public.customers (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.refresh_tokens
    OWNER to tsb;


-- Table: public.txns

-- DROP TABLE IF EXISTS public.txns;

CREATE TABLE IF NOT EXISTS public.txns
(
    id bigint NOT NULL DEFAULT nextval('txns_id_seq'::regclass),
    account_id bigint NOT NULL,
    kind character varying(16) COLLATE pg_catalog."default" NOT NULL,
    amount numeric(18,2) NOT NULL,
    description text COLLATE pg_catalog."default",
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT txns_pkey PRIMARY KEY (id),
    CONSTRAINT txns_account_id_fkey FOREIGN KEY (account_id)
        REFERENCES public.accounts (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.txns
    OWNER to tsb;




Why jwt refresh token ?

* **Security:** Keep access tokens short-lived (e.g., 15 min) so leaks have tiny impact; use refresh tokens to get new access tokens safely.
* **Better UX:** Silent re-auth without forcing users to log in repeatedly.
* **Control & Revocation:** Store/rotate refresh tokens server-side to revoke sessions (on logout/password reset/compromise).


why twilio?

* **Easy + secure OTP**: Twilio Verify handles code generation, delivery, expiry, and validation—so you don’t store OTPs or build that logic yourself.
* **Reliable delivery at scale**: Global carrier reach, sender compliance tooling, detailed error codes (e.g., 21608), and good deliverability/telemetry.
* **Dev-friendly**: Solid SDKs, console debugging, trial to start, and a clean API—fast to integrate and maintain.


Why postgres?

* **Bank-grade consistency:** Strong ACID transactions, row-level locks, and mature isolation levels—perfect for money transfers and double-entry ledgers.
* **Rich features we use:** `jsonb` for audits, `pgcrypto` for hashing/blind indexes, powerful indexing/CTEs—fast, safe lookups with encrypted data.
* **Ops-friendly & proven:** Open-source, stable, great tooling (Flyway, JPA), easy Docker setup and scaling (read replicas/partitioning) without vendor lock-in.



Important secuiry feature
AuthN/Z: JWT access (short-lived) + refresh tokens with rotation & server-side revocation; role-based authorization.
MFA/Reset: Twilio Verify OTP for password reset + short-lived reset JWT (typ=RESET, ~10 min).
Password safety: BCrypt hashing, strong password policy, refresh-token purge on password change.
Rate limiting: Bucket4j per-IP on login/OTP/reset to block brute force.
Data protection: Account numbers AES-GCM encrypted at rest + HMAC blind index for safe/equal lookups.
CORS: Strict allow-list of frontend origins; credentials controlled.
Audit logging: Login/transfer/reset/OTP events with minimal, masked sensitive data.
Exception handling: Centralized @RestControllerAdvice with safe, consistent error JSON (no sensitive leakage).


Imporant feature for password reset api

Anti-enumeration: /request-reset always returns 200, so you can’t probe which emails/phones exist.
OTP via Twilio Verify (no code stored): Provider handles code gen/expiry/validation; we only send/verify → smaller attack surface.
Short-lived Reset JWT: After OTP, we issue a 10-min token with typ=RESET (cannot be used as access), validated server-side before changing the password.
Aggressive revocation: On successful reset we purge all refresh tokens → logs out every device/session.
Rate limiting: Bucket4j on /request-reset and /verify-reset to throttle brute-force/guessing.
Strong password policy + hashing: Server enforces complexity; passwords stored with BCrypt.
Audit logging: OTP request/verify (OK/FAIL) and reset completion recorded (with masked data).
Phone normalization (E.164): Prevents format tricks and ensures OTP is sent only to the verified, exact number.