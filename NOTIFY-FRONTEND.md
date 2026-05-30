# NOTIFY: API changes → update Angular frontend

> **BACKEND API (Spring Boot / Kotlin):** `f:/src/pos/api/pos`  
> **FRONTEND (Angular):** `f:/src/pos/posfont`  
> This file lives in the **backend** repo. Read it whenever you change the API.

**Read this file whenever you change the POS API** so the Angular app stays in sync.

| Item | Location |
|------|----------|
| **Backend API** | **`f:/src/pos/api/pos`** ← Spring Boot, Kotlin, Maven |
| **Frontend (posfont)** | `f:/src/pos/posfont` ← Angular (separate folder) |
| **Default API URL in frontend** | `http://localhost:8080` — `posfont/src/app/app.config.ts` → `POS_API_BASE_URL` |
| **Run API** | `cd f:/src/pos/api/pos && mvn spring-boot:run` |
| **Run API tests** | `cd f:/src/pos/api/pos && mvn test` |
| **Run frontend** | `cd f:/src/pos/posfont && ng serve` |

---

## Checklist (do every API change)

1. **Entity** (`src/main/kotlin/me/pixka/pos/*/model/`)  
   - Add column / field on JPA entity.  
   - `spring.jpa.hibernate.ddl-auto=update` adds columns on restart (H2 file DB: `./data/posdb10`).

2. **Request DTO** (`*/api/*Request.kt`)  
   - Add field with validation.  
   - Use **`@field:JsonAlias("snake_case")` and `@param:JsonAlias("snake_case")`** on Kotlin data classes (Jackson constructor binding).

3. **Service** (`*/service/*Service.kt`)  
   - Map request → entity on create/update.  
   - Throw domain exceptions (`*NotFoundException`) — frontend shows `message` from JSON error body.

4. **Controller** (`*/api/*Controller.kt`)  
   - New routes under `/api/...`.  
   - Keep route order: static paths (e.g. `/report`) **before** `/{id}`.

5. **Tests** (`src/test/kotlin/...`)  
   - Add or update `*ServiceTest` / controller tests for new behaviour.

6. **Frontend mirror (required)** — see mapping table below.

7. **Update this file** — add a row to **Recent API ↔ frontend contracts** at the bottom.

---

## API package → Angular files

| API area | Kotlin base package | Angular model | Angular service | Angular UI |
|----------|---------------------|---------------|-----------------|------------|
| Orders | `me.pixka.pos.order` | `posfont/src/app/order/order.model.ts` | `order.service.ts` | `order-list`, `order-add-new`, `order-edit`, `table-list` |
| Order lines | `order.model.OrderLine` | same `order.model.ts` (`OrderLine`, `OrderLineRequest`) | via order PUT/POST | line picker, list expand |
| Tables | `me.pixka.pos.table` | `table/table.model.ts` | `table.service.ts` | `table-list`, … |
| Foods | `me.pixka.pos.food` | `food/food.model.ts` | `food.service.ts` | `food-list`, … |
| Food categories | `me.pixka.pos.foodcategory` | `food/food.model.ts` (`FoodCategory`) | `food-category.service.ts` | … |
| Kitchens | `me.pixka.pos.kitchen` | `food/food.model.ts` (`Kitchen`) | `kitchen.service.ts` | … |
| Zones | `me.pixka.pos.zone` | `zone/zone.model.ts` | `zone.service.ts` | … |
| Users / auth | `me.pixka.pos.auth` | *(no full auth UI yet)* | — | header `User ID` only (`auth/pos-current-user.service.ts`) |
| Reports | `me.pixka.pos.report` | — | — | not in posfont yet |
| Backup | `me.pixka.pos.backup` | — | — | not in posfont yet |
| Printers | `me.pixka.pos.printer` | — | — | not in posfont yet |

**Shared order helpers (frontend):**  
`order-line-status.util.ts`, `order-merge.util.ts`, `order-datetime.util.ts`

---

## JSON naming rules

- **API JSON:** camelCase by default (`orderNo`, `userId`, `paidAt`).  
- **Also accept on requests:** snake_case via `@JsonAlias` (`user_id`, `paid_at`, `table_id`, …).  
- **Frontend:** sends **both** camelCase and snake_case for fields the backend might bind either way (pay settlement, `userId`, `is_paid`).

When adding a new persisted field:

```kotlin
// OrderRequest.kt example
@field:JsonAlias("user_id")
@param:JsonAlias("user_id")
val userId: Long? = null,
```

```typescript
// order.model.ts — PosOrder + OrderRequest + optional snake_case alias
userId?: number | null;
user_id?: number | null;
```

---

## Orders — important behaviour

### Endpoints (backend)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/orders?q=` | List / search by `orderNo` |
| GET | `/api/orders/{id}` | Single order |
| POST | `/api/orders` | Create (`OrderRequest`) |
| PUT | `/api/orders/{id}` | Update open order (`OrderRequest`, optimistic `version`) |
| POST | `/api/orders/{id}/pay` | **Settle payment** (`PayOrderRequest` optional body) |
| POST | `/api/orders/{id}/pay/qr-scan` | Pay via QR |
| PATCH | `/api/orders/{id}/note` | Whole-order note only (paid OK) |
| DELETE | `/api/orders/{id}` | Delete |

### Frontend pay flow (current — may drift from API)

- **posfont today:** pay = **`PUT /api/orders/{id}`** with `paid`, `paidAt`, settlement amounts, all lines `COMPLETE`.  
- **API canonical pay:** **`POST /api/orders/{id}/pay`** — `OrderService.pay()` sets `paid`, `paidAt`, `complateOrder`; body optional (`PayOrderRequest`).

If you change pay semantics on the API, **either**:

- update `posfont/src/app/order/order.service.ts` + `order-list` / `table-list` `confirmPayFromDialog`, **or**  
- document here that PUT must still accept pay fields (and implement in `OrderService.update` if needed).

### Fields on `PosOrder` / `OrderRequest`

| Field | API entity | API request | Frontend | Notes |
|-------|------------|-------------|----------|-------|
| `userId` / `user_id` | `orders.user_id` | `OrderRequest.userId` | sent on create/add line | Must exist in `users` table |
| Line `userId` | `order_lines.user_id` | `OrderLineRequest.userId` | per line on create; preserved on PUT | Falls back to order `userId` |
| `paid` | `orders.paid` | not on `OrderRequest` | sent on PUT pay (frontend) | Use `POST …/pay` on API |
| `paidPrice`, `change` | columns | `OrderRequest` | settlement on pay PUT | |
| `complateOrder` | yes (typo kept) | yes | yes | Same spelling both sides |
| `version` | `@Version` | required | required | Mismatch → refresh error |
| `note` / `order_note` | `order_note` | `note` aliases | partial | PATCH note endpoint exists |

### Line status

- API enum: `OrderLineStatus` (`WAIT`, `COMPLETE`, `CANCEL`, …).  
- Frontend: `order-line-status.util.ts` → `resolvedLineStatus`, PUT builders in `orderRequestCompleteAllExceptCanceled`.

---

## Auth

- Most `/api/**` routes require **JWT** (`SecurityConfig`, profile `!test`).  
- Login: `POST /api/auth/login` → Bearer token.  
- **posfont:** no global JWT interceptor yet; dev may hit 401 unless security disabled or token wired.  
- Operator **`userId`** in header is separate from JWT — sent in JSON on orders/lines.

Default admin (when enabled): `admin` / `admin` — `application.properties`.

---

## When you add a new API module

1. Create Kotlin package under `me.pixka.pos.<name>`.  
2. Add controller `@RequestMapping("/api/<name>")`.  
3. In **posfont**: add `*.model.ts`, `*.service.ts`, routes in `app.routes.ts`, list/add/edit components (copy food/table pattern).  
4. Register CORS if needed — `WebCorsConfig.kt`.  
5. Add a row to the mapping table above.

---

## Recent API ↔ frontend contracts

| Date | Change | API files | Frontend files |
|------|--------|-----------|----------------|
| 2026-05-30 | Order + line **`userId`** persisted | `PosOrder.kt`, `OrderLine.kt`, `OrderRequest.kt`, `OrderLineRequest.kt`, `OrderService.kt` | `order.model.ts`, `order-add-new`, `order-list`, `order-line-status.util`, `auth/pos-current-user.service.ts`, `app.html` |
| 2026-05-30 | Pay / paid UI (PUT-based in frontend) | `OrderService.pay`, `PayOrderRequest` | `order-list`, `order.model` (`orderIsPaid`, `orderPayStateForPut`) — **align pay route when possible** |

---

## For AI / code assistants

When the user asks to change the **API**, read **this file first**, then:

1. Implement Kotlin changes + tests in `f:/src/pos/api/pos`.  
2. Mirror types and HTTP calls in `f:/src/pos/posfont` using the mapping table.  
3. Append a line to **Recent API ↔ frontend contracts**.  
4. Do not assume pay = PUT unless this doc says so; check `OrderController` and `order.service.ts`.
