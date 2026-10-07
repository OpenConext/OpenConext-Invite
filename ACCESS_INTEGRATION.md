# Invite ⇄ Access integration: options, risks and plan

> Discussion + hand-off document. Written 2026-10-07. Self-contained: a new session can execute the plan from this file alone.

---------------------------------------------------------------------------------------------------

## 0. Update 2026-10-07: confirmed by the team

| # | Statement | Effect on the plan |
|---|---|---|
| D1 | The backend (OIDC-NG/Manage) can be configured so **Invite's resource server accepts Access's access_token** | Gotcha 2 is resolved by configuration (still to be done and tested per environment); spike 0a shrinks to *verification* |
| D2 | A **refresh grant** can be added to Access's OpenID client | Gotcha 1 is resolved by design: Access must store and use the refresh token (see §4a) |
| D3 | The **`sub` is identical** for Access and Invite | Gotcha 4 is resolved; Invite's lookup by `sub` (`findBySubIgnoreCase`) keeps working |

Consequence: option A is **feasible without a go/no-go blocker**; the remaining work is engineering plus the open
risks and assumptions in §7a. Option A is now a serious candidate next to B; the choice depends on the answer to
open question 2 (independent deployability of the Invite UI) and 1 (is a page refresh acceptable).

---------------------------------------------------------------------------------------------------

## 1. Original goal and the questions asked

**Goal (first request):** give the Invite application a UI overhaul so it resembles SURF Access
(Figma: `https://www.figma.com/design/81StIVqfOKfwhWVjx7Ew81/SURF-Access?node-id=9904-2329&m=dev`).
Code from Access is duplicated into Invite (Access is checked out in `invite/access`, git-ignored). Invite uses the
same libs as Access (`@phosphor-icons/react ^2.1.10`, `@surfnet/curve-react ^0.5.1`). The left-side menu in Invite must be
pixel-perfect the same as Access and its links point to the Access application. The Access menu item "Roles"
(`/external/invite`) must point to the locally running Invite (`http://localhost:3000/home/roles`).

**Follow-up requests already done:** logo, font size / weight / font-smoothing alignment, organisation dropdown in Invite
(from `surf-crm-id` stored on the Invite `users` table, resolved via Manage), redirect to Access
`/home?organizationId=…`, separate session cookie name for Invite (`INVITE_SESSION`), feature toggle
`config.invite_same_tab` in Access, hide the menu after logout.

**The question that triggered this document (verbatim intent):**
> We have decided that we don't want to duplicate the left-side menu, but instead use a web component wrapper to load
> the Invite application in the right-side content panel when the user clicks the Roles link. For this the Invite security
> has to become a resource server and the Invite client needs to use proxy endpoints in the Access server to reroute them
> server-to-server to Invite, adding the access_token obtained from the initial login.
> 1. How feasible is this?  2. What are the gotchas?  3. Is there a better way to prevent page refreshes and not
> duplicate the entire menu?  4. Draft a plan.

**Follow-up question:** *In option C (shared shell) how would Invite know which menu items to render? Visibility of the
links is based on the logged-in user in Access (see `access/client/src/utils/MenuItems.js`).* → answered in §5.

---------------------------------------------------------------------------------------------------

## 2. Current state (what exists today)

### Repos and processes (local dev)
| Thing | Location | Port | Notes |
|---|---|---|---|
| Invite repo (branch `feature/access`) | `/Users/harst001/projects/invite` | | `client/` (Vite + React 19.3), `server/` (Spring Boot, Maven) |
| Invite client dev server | `invite/client` | 3000 | proxies `/api/v1`, `/config` to 8888 |
| Invite server | `invite/server` | 8888 | run from IntelliJ, **no `local` profile** → real OIDC login (connect.test2) |
| Access checkout used by the dev servers | `/Users/harst001/projects/access` (branch `main`) | client 3002, server 8886 | runs with `profiles.active=local` |
| Access copy inside Invite (git-ignored) | `/Users/harst001/projects/invite/access` | – | **not** what runs; edits must be applied to `/Users/harst001/projects/access` too |

### Changes made in Invite so far (uncommitted, branch `feature/access`)
- Client: `@surfnet/curve-react`, `@phosphor-icons/react`, Tailwind 4 (`@tailwindcss/vite`), react 19.3; `vite.config.js` (tailwind plugin,
  `preserveSymlinks`); `index.html` class `theme-surf-green`; `main.jsx` (TooltipProvider, Toaster, curve + tailwind CSS);
  `styles/sds-layered.css` (SDS CSS pushed into cascade layer `sds` between `components` and `utilities` so the curve preflight
  and Tailwind utilities do not fight SDS); `tailwind.css` (copied); `utils/MenuItems.js` (static copy of Access menu, all links → `ACCESS_URL`,
  only "Roles" internal); components `SharedMenu`, `AuthorizedHeader`, `UserMenu`, `LanguageSwitcher`, `BreadCrumb`,
  `InviteFooter` (+scss); `icons/logo2.svg` rendered via `?url` (SDS global `svg [stroke]` rule breaks inline SVG);
  `:root{--text-sm:1rem}`; font family `var(--font-sans)` and `-webkit-font-smoothing:auto` on shell elements;
  `api/index.js#organizations`; logout sets `authenticated:false`.
- Server: `UserController#organizations` (`GET /api/v1/users/organizations`, uses `User.surfCrmId` → `manage.identityProvidersByInstitutionalGUID`,
  returns `{id: manageIdentifier, name: name:en}`); `User.surfCrmId` + migration `V67_0__user_surf_crm_id.sql`;
  `UserHandlerMethodArgumentResolver` stores the `surf-crm-id` claim on login; `LocalDevelopmentAuthenticationFilter` adds the claim in the `local` profile;
  `application.yml`: session cookie name `INVITE_SESSION`; tests in `UserControllerTest` (46 pass, full suite 454 pass).
- Access (applied to both `invite/access` and `/Users/harst001/projects/access`): `App.jsx` handles `?organizationId=` (id **or** `manageIdentifier`),
  `SharedMenu.jsx` + `MenuItems.js` (Roles → `config.invite + /home/roles` when `config.inviteSameTab`), `Config.java` field `inviteSameTab`,
  `application.yml` (`invite: http://localhost:3000`, `invite_same_tab: true`).

### Facts about the code that drive the options below
- Invite is **already** an opaque-token resource server, but only for `/api/external/v1/**`
  (`server/.../security/SecurityConfig.java#jwtSecurityFilterChain`, introspection `oidcng.introspect-url`, RS id/secret `oidcng.resource-server-*`;
  local value `myconext.rs` is a placeholder). `UserHandlerMethodArgumentResolver` already handles `BearerTokenAuthentication`.
- The UI API `/api/v1/**` is **session + CSRF** only (`sessionSecurityFilterChain`, `@Order(1)`, matches before any bearer chain).
- Access never reads the access_token; Spring keeps it in the (JDBC-backed, 8h) HTTP session. Scope is only `openid`, no refresh token.
- Invite's `CustomOidcUserService` provisions institution admins and sets `institutionAdmin`, `organizationGUID`, `surfCrmId` from **userinfo claims
  at login time only**. Users are looked up by `sub`.
- Invite client (`client/src/api/index.js`): cookie + `X-CSRF-TOKEN`, `window.location.reload()` when `x-session-alive` (set in `invite/config/AccessFilter`)
  is missing or on 401; `BrowserRouter` with absolute paths (≈27 `navigate`/`Link`), ≈48 uses of `location`/`localStorage`/`document`.
  No multipart, SSE or WebSocket → a plain JSON proxy suffices.
- Menu visibility in Access is client-side: `doMenuItemsForUser(user, currentOrganization, feedbackWidgetEnabled)` in
  `access/client/src/utils/MenuItems.js` (memberships/authority per current organization, `organization.manageIdentifier`, `config.features`, `superUser`).
  The current organization lives in `localStorage` (`organization`) on the Access origin.
- Access login (`LocalDevelopmentAuthenticationFilter`, profile `local`) adds `surf-crm-id`; Invite's real OIDC client only received it after a backend
  change in another system (claims for the Invite client).

---------------------------------------------------------------------------------------------------

## 3. Options

| | A. Web component + token-relaying proxy (the proposal) | B. Shared React package / lazy route | C. Shared shell (menu/header as one package, both apps) | D. iframe |
|---|---|---|---|---|
| Menu duplicated? | no | no | no (one package, one model) | no |
| Page refresh when going Access → Roles | none | none | **yes** (full load, can be masked) | none for shell |
| Invite security change | RS for UI API + userinfo enrichment | none (Invite UI keeps its own API access) or proxy | none | none |
| Access server change | proxy controller, token handling | optional proxy | `/api/v1/menu` endpoint + CORS(credentials) | CSP `frame-ancestors`/headers |
| Complexity | high | medium | low | low-medium |
| Independent deploy of Invite UI | yes | no (versioned package) | yes | yes |

### A. Web component + proxy (what was asked)
Access mounts `<surf-invite>` (bundle served by Invite or Access) in its content panel; the element calls `/api/v1/invite-proxy/**` on Access;
Access forwards to Invite with `Authorization: Bearer <access_token from the OIDC login>`; Invite validates it as resource server.

### B. Shared React package or Module Federation remote
Invite UI exported as `@surf/invite-ui` (or a federated remote) and mounted by Access on route `/invite/*` in the same React tree: shared router,
breadcrumb, CSRF, theme and menu. Both already run React 19.3 + curve-react. Invite UI keeps talking to the Invite API (CORS + cookie/bearer) or via a proxy.

### C. Shared shell
Extract sidebar + header + breadcrumb + footer into one package (`@surf/access-shell`). Both apps render it; navigation between them is a full document
load (masked with View Transitions API / prefetch / identical skeleton). Replaces the duplicated `client/src/components/SharedMenu.jsx`.

### D. iframe
Invite inside Access; postMessage for URL/height/breadcrumb. Own session, no proxy.

---------------------------------------------------------------------------------------------------

## 4. Feasibility of option A and its gotchas (ordered by risk)

**Verdict: feasible, a medium-size project. The three original go/no-go dependencies (token acceptance, token lifetime, `sub`) are resolved by D1-D3 (§0); claims via userinfo remain to be verified (S5).**

1. **Token lifetime vs session.** *(Mitigated by D2, see §4a.)* Access session 8h; access_token typically ~1h; scope `openid` only, no refresh token. After expiry every proxied call fails.
   Needs `offline_access`/refresh (is it allowed for the Access RP in OIDC-NG?) or a top-level re-auth on 401. XHR cannot re-login silently.
2. **Token acceptance by Invite's RS.** *(Resolved by configuration, D1 – verify per environment.)* The Access RP in Manage must allow Invite's resource server (`allowedResourceServers`), and the RS id/secret must be
   registered (change in another system). Verify the audience/scope semantics of OIDC-NG introspection.
3. **Claims.** Introspection returns token data, not the userinfo claims Invite needs (name, email, `schac_home_organization`, `surf-crm-id`,
   `eduperson_entitlement` for institution-admin provisioning). Invite must call `/userinfo` with the bearer (cache by token hash) and re-run the provisioning
   currently done only in `CustomOidcUserService` at login. Extract that logic into a shared service.
4. **Identity mapping.** *(Resolved, D3.)* Invite keys users by `sub`. Access and Invite are different OIDC clients – verify `sub` is identical (not pairwise) or every user is "not found".
5. **Flows that must stay top-level**: invitation accept (`/invitation/accept`, eduID enforcement, `login_hint` in `AuthorizationRequestCustomizer`), e-mail deep links,
   first-time users without an Invite account. Invite must remain deployable standalone; embedded mode is additional.
6. **Two chains on `/api/v1/**`.** A bearer chain must be ordered **before** the session chain and selected by the `Authorization: Bearer` header
   (stateless, CSRF off). Otherwise the session chain answers 302/401.
7. **CSRF.** The Access proxy is a session endpoint → protected by Access CSRF. The Invite UI must use Access's CSRF token, not Invite's `/api/v1/csrf`.
8. **Proxy security.** Strict path allow-list (never `/api/external/**`, `/internal/**`); strip `Cookie`, `Set-Cookie`, `Authorization` from the browser request/response;
   add the bearer server-side; forward only allow-listed headers; re-add `x-session-alive`; map 401 → Access re-auth signal; timeouts and size limits;
   audit log with the *user* (Invite will otherwise log the proxy); tokens at rest in the session table should be encrypted.
9. **Impersonation.** Invite's `X-IMPERSONATE-ID` uses Invite user ids; Access impersonation is a different concept. Disable in embedded mode or map explicitly.
10. **CSS isolation.** Shadow DOM isolates SDS global CSS, but SDS is written for `body.sds--color-palette--green`, `:root` and global `@font-face` → variables vanish
    in a shadow root. Portals (react-select, react-datepicker, react-tooltip, SDS modals) render to `document.body`, outside the shadow root, and lose styling.
    A light-DOM element avoids this but keeps today's `@layer sds` workaround. Best fix: finish the SDS → curve-react migration of the Invite pages first.
11. **Host integration.** URL ownership (`/invite/*` with router `basename` or memory router + sync), breadcrumb/flash/locale/active-menu bridging
    (custom events + attributes), scroll lock, z-index, focus, `lang` cookie, organisation context, unsaved-change prompts.
12. **Operations.** Remote script = supply chain/CSP (`script-src`, SRI, versioned URL), CORS for the bundle, version skew between element and API, double-hop latency,
    Access now depends on Invite availability for that panel.

### 4a. Design consequences of D1-D3 for option A

**Refresh grant (D2) – what Access must do**
- Add `refresh_token` to the grant types of the Access OpenID client (Manage) and request the scope that yields a refresh token
  (`offline_access` in many OPs; confirm for OIDC-NG). Add the scope to `spring.security.oauth2.client.registration.oidcng.scope`.
- Access must **use** the token: register an `OAuth2AuthorizedClientManager` (`.refreshToken()` provider) and an explicit `OAuth2AuthorizedClientService`/repository
  instead of the implicit session copy. The proxy asks the manager for the authorized client per request; Spring refreshes when expired (clock skew set).
- Store the authorized client **server-side with encryption at rest** (JDBC `OAuth2AuthorizedClientService`, tokens encrypted with the existing `java-crypto`
  KeyStore pattern) rather than in the serialized HTTP session row. Delete it on logout (`/api/v1/users/logout`) and revoke at the OP if supported.
- Concurrency: several parallel XHRs may refresh simultaneously; with refresh-token rotation a race invalidates the token. Serialize refreshes per session
  (lock per principal) and treat `invalid_grant` as "session over" → 401 with re-auth signal.
- The refresh token outlives the 8h session unless limited; align its TTL with the session timeout and with the OP's SSO session policy.

**Resource server (D1) – what Invite must do**
- The Invite RS id/secret must be known to the OP and listed as allowed resource server for Access's client; the introspection response must contain at
  least `active`, `sub`, `scope`, `exp` (+ `client_id`/`aud` to verify it was issued to Access's client; reject tokens of other clients).
- Add a dedicated scope for the Invite UI API (e.g. `invite`) and require it in the bearer chain, so the existing external-API tokens cannot call the UI API
  and vice versa.
- Claims are still not in introspection output: call `/userinfo` with the bearer token and cache per token hash for a short TTL (see gotcha 3).

**Same `sub` (D3)**
- Keep `findBySubIgnoreCase(sub)` as the user key. Add a test with an Access-issued token and an existing Invite user.

---------------------------------------------------------------------------------------------------

## 5. Option C in detail – how does Invite know which menu items to render?

Item visibility is derived from the **Access** user (`doMenuItemsForUser`: memberships/authority for the current organization, `manageIdentifier`, `config.features`,
`feedbackWidgetEnabled`, `superUser`). Invite has none of that data (its users/roles and `surfCrmId` are another model), so it must **ask Access**:

1. **Shared package** exports the *presentational* shell only: `<AppSidebar model={…}>`, header, breadcrumb, footer, icons, translations. It takes a **menu model**
   (groups, items, active item, organisation list, current organisation, `href`s) and contains **no visibility rules**.
2. **Single source of truth = Access server.** New slim endpoint `GET /api/v1/menu?organizationId=` →
   `{menuItems:[names], organizations:[{id,name}], currentOrganization, user:{name, role}}`. The rules move from `MenuItems.js` to the server (port of
   `doMenuItemsForUser`, ~40 lines, unit-testable). Access's own client consumes the same endpoint, so both apps render identical menus by construction.
3. **Invite fetches it cross-origin:** `fetch(ACCESS_URL + "/api/v1/menu", {credentials: "include"})`. Works because Access and Invite are same-site
   (dev: `localhost` ports; prod: sibling subdomains), so the Access session cookie is sent. Needs CORS `allowedOrigins=<invite url>` + `allowCredentials` for that single
   read-only GET (no CSRF issue). No token relaying, no resource server.
4. **Organisation context:** Access keeps it in `localStorage` on its own origin, unreadable by Invite. Invite passes the selection back via the existing
   `?organizationId=` redirect; the dropdown list comes from the same `/menu` response. The Invite-side `GET /api/v1/users/organizations`
   (surf-crm-id lookup) and the Manage-identifier mapping then become unnecessary and can be removed.
5. **No Access session** (user only logged in at Invite, or expired): `/menu` → 401 → render a minimal shell (logo, "Roles" active, link to Access login) or top-level
   redirect to Access login (SSO makes it silent). Show a skeleton while loading; cache the last model in `sessionStorage` (cosmetic only).
6. **Security:** menu visibility is cosmetic. Invite endpoints keep enforcing authority; a stale or forged menu cannot grant access.

Remaining downside of C: still a document navigation between the apps (mitigations: `<link rel=prefetch>`, View Transitions API, identical shell → no visible flash).

---------------------------------------------------------------------------------------------------

## 6. Consequences

- **A:** Access becomes a security-critical gateway (token store + proxy); Invite gains a second authentication mode; auth bugs affect both apps; outages couple.
  Gains: one continuous UI, independent deploy of the Invite UI bundle.
- **B:** UI release coupling (package versioning or federation contract); simplest runtime; best UX; requires Invite UI to be build-compatible (React, curve-react, Tailwind versions).
- **C:** Lowest risk; menu logic centralised (positive side effect: removes the client-side duplicated rules); keeps two apps and two sessions; one document navigation remains.
- **D:** Cheap but brittle: IdP cannot be framed (needs an existing Invite session or top-level login first), cookies must be same-site, `frame-ancestors` CSP, scroll/height/a11y quirks.

## 7. Risks (summary)

| Risk | Option | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| Access token not introspectable by Invite RS / missing claims | A | low (D1) | blocker if config differs per environment | Verify per environment (0a), userinfo enrichment |
| Token expiry mid-session | A | low (D2) | high UX | refresh grant + authorized-client manager; re-auth fallback on `invalid_grant` |
| `sub` differs per client | A | none (D3) | – | covered by test |
| SDS CSS ↔ shadow DOM / portals | A | high | medium | light DOM, or finish migration first |
| Cookie/CORS in prod domains (not same-site) | C, D | low | medium | verify the real hostnames before building |
| Menu rules drift between apps | C | n/a after port | – | server-side single source |
| Proxy becomes open relay / leaks tokens | A | low | severe | allow-lists, header stripping, tests |

## 7a. Open risks and assumptions

### Assumptions (to be confirmed or tested; owner in brackets)
| # | Assumption | How to verify |
|---|---|---|
| S1 | D1: configuration of OIDC-NG/Manage for Invite's RS is done **in every environment** (local mock, test, prod), including `allowedResourceServers` and RS credentials [backend team] | scripted introspection call with a real Access token per environment |
| S2 | D2: OIDC-NG issues a refresh token for the Access client and **rotation behaviour** is known (rotating or reusable) [backend team] | login, wait for expiry, refresh via `OAuth2AuthorizedClientManager` |
| S3 | D3: `sub` is the same in all environments and for all authentication paths, including eduID users, guests and institution admins provisioned on the fly | compare `users.sub` in both databases for a sample of users |
| S4 | The introspection response contains `client_id`/`aud` and `scope` so Invite can verify the token was issued to Access's client | inspect real response |
| S5 | `/userinfo` accepts the Access-issued token and returns the claims Invite needs (`surf-crm-id`, entitlements, `schac_home_organization`, name, email) | call userinfo with that token |
| S6 | Access and Invite are same-site in all environments (needed for option C cookie fetch and D) | hostnames per environment |
| S7 | Access has a trusted place to hold tokens (encrypted session store) acceptable to security/privacy review | security review |
| S8 | The invitation-accept and e-mail deep-link flows stay on the standalone Invite app | product decision |

### Open risks
| # | Risk | Likelihood | Impact | Mitigation / decision needed |
|---|---|---|---|---|
| R1 | Refresh-token race with rotation (parallel XHRs) invalidates the session | medium | users logged out at random | per-principal lock; test with parallel calls |
| R2 | Token store at Access becomes a high-value target (refresh tokens = long-lived access to Invite and potentially other RSs) | medium | severe | encryption at rest, narrow scope, revocation on logout, short TTL, audit |
| R3 | Over-broad token: any RS allowed for Access's client accepts the same token | medium | medium | audience restriction/scope `invite`, verify `aud` in Invite |
| R4 | Authorization drift: Access user authority (admin/member) vs Invite authority (role manager, inviter, institution admin) are different models; embedding suggests one, but permissions stay per-app | high (confusion) | medium | keep Invite enforcement as is; document; UI hides nothing on trust |
| R5 | Impersonation semantics differ (Access vs Invite `X-IMPERSONATE-ID`) | medium | medium | disable in embedded mode first, decide later |
| R6 | CSS/popup breakage in embedded mode (SDS global CSS, portals) | high | medium | light DOM element; finish SDS → curve-react migration; visual regression pass |
| R7 | Version skew between the web-component bundle and the Invite API | medium | medium | versioned bundle URL, `/api/v1` backwards compatibility policy, contract tests |
| R8 | Double-hop failure modes: Invite down → Access panel blank; timeouts/retries on non-idempotent POSTs (duplicate invitations/roles) | medium | medium | timeouts, no automatic retry on POST, idempotency keys where possible, error UI |
| R9 | Audit trail: Invite sees calls from the proxy; user attribution must stay correct (`sub` from the bearer, not the proxy) | low | medium | log `sub` on both sides; correlation id header |
| R10 | Local development: the `local` profile bypasses OIDC, so the bearer path cannot be exercised locally without extra work | high | medium | provide a local token stub / use the mock OP; add integration tests with wiremock introspection |
| R11 | Embedded mode needs the Access session to exist; Invite-only users (invitees, role managers without Access account) have no Access session | medium | high UX | keep standalone Invite reachable; Access provisioning for them or a clear fallback |
| R12 | Two repos/checkouts (`/Users/harst001/projects/access` vs `invite/access`) drift | medium | low | one canonical checkout; do not edit the git-ignored copy |


---------------------------------------------------------------------------------------------------

## 8. Recommendation (updated after D1-D3)

The three blockers of option A are removed by configuration/design (D1-D3), so the choice is no longer driven by feasibility:

1. **Still do C first** if a quick win is wanted: it removes the duplicated sidebar and centralises menu rules, and its shared shell is reused by A and B.
2. **A vs B** is now a product/architecture decision:
   - choose **A** (web component + proxy) when the Invite UI must be deployed and versioned independently of Access and the teams accept the proxy/token-store
     responsibility (R1, R2, R3, R7, R8);
   - choose **B** (shared React package) when both UIs may release together; it avoids the proxy and token store entirely.
3. If A is chosen, run the verification spikes (0a-0c) per environment, then build Phase 2 behind a feature toggle (default off).

## 9. Tasks

### Phase 0 – Spikes / decisions (1-2 days)
- [ ] 0a *Verify D1*: introspect a real Access-login access_token as Invite's RS in each environment: accepted, `client_id`/`aud`/`scope` present, `/userinfo` works (S1, S4, S5)
- [ ] 0b *Verify D2*: refresh grant on the Access client; token TTLs; rotation behaviour; parallel-refresh test (S2, R1)
- [ ] 0c *Verify D3*: compare `sub` for a sample of users in both databases incl. eduID/guest/institution admins (S3)
- [ ] 0d Real prod/test hostnames of both apps: same-site? cookie `SameSite`/`Domain` implications (C, D)
- [ ] 0e Prototype `<surf-invite>` as light-DOM custom element inside Access; list broken popups/styles (A)
- [ ] 0f Decide A/B/C/D with the team (open questions in §10)

### Phase 1 – Option C (recommended first step)
- [ ] Access server: `GET /api/v1/menu` (port `doMenuItemsForUser`, organisations, current org, user label); unit tests; CORS(credentials) limited to the Invite origin
- [ ] Shared package `@surf/access-shell` (sidebar, header, breadcrumb, footer, icons, i18n, SCSS incl. the font/smoothing/`--text-sm` fixes found in Invite);
      consume it from Access (replace `components/SharedMenu.jsx`, `AuthorizedHeader.jsx`) and Invite (replace the copies in `client/src/components/`)
- [ ] Invite client: fetch `/menu` from Access, loading + 401 states, organisation dropdown from the response; remove `utils/MenuItems.js`, `organizations()` API call
- [ ] Invite server: remove `GET /api/v1/users/organizations` and the Manage lookup (keep `surf_crm_id` column only if still needed)
- [ ] Masking of the navigation: prefetch + View Transitions; verify no flash
- [ ] Access: keep `invite_same_tab` toggle semantics (same tab vs new tab) or retire it
- [ ] Tests: server (`/menu` per user type), client (shell rendering from model), e2e (menu parity Access vs Invite for admin/member/guest/superuser)

### Phase 2 – Option A (only if chosen; after spikes)
- [ ] Invite server: require scope `invite` and verify `client_id`/`aud` of the introspected token; bearer filter chain for UI paths ordered before the session chain; bearer user resolution with userinfo enrichment + institution-admin provisioning
      (extract from `CustomOidcUserService`); keep `x-session-alive`; tests with `openIDConnectFlow` + wiremock helpers in `server/src/test/java/invite/AbstractTest.java`
- [ ] Access server: add refresh grant scope to the client registration; `OAuth2AuthorizedClientManager` with refresh provider; encrypted JDBC `OAuth2AuthorizedClientService`; delete/revoke on logout; per-principal refresh lock
- [ ] Access server: `InviteProxyController` (`/api/v1/invite-proxy/**`) with `RestClient`, uses the authorized-client manager per request, allow-lists,
      error mapping (401 → re-auth), audit log; feature toggle next to `config.invite_same_tab`
- [ ] Invite client: embedded build (Vite lib → `surf-invite.js`, attributes `base-path`, `locale`, `api-base`; events `invite:breadcrumb|flash|unauthorized|navigate`);
      API layer base URL + CSRF source switch; no `window.location.reload` in embedded mode; router `basename`; standalone mode kept for invitation accept
- [ ] Access client: lazy route `/invite/*`, versioned bundle URL + SRI, breadcrumb/flash/locale/organisation bridging, "Roles" → `/invite`
- [ ] Backend config (other team): RS registration, `allowedResourceServers`, refresh grant + scope for the Access client, per environment
- [ ] Hardening: CSP/CORS, token-expiry UX, e2e (login → Roles → create role → token expiry → back to same page), runbook, toggle default off

### Phase 2' – Option B (alternative to A)
- [ ] Package Invite UI as `@surf/invite-ui` (or Module Federation remote); Access route `/invite/*`; shared router/breadcrumb/CSRF; decide API path (direct CORS+bearer vs proxy)

## 10. Open questions for the team
1. Is "no page refresh" a hard requirement, or is a seamless-looking navigation (option C) sufficient?
2. Must the Invite UI be deployable independently of Access (→ A) or may it be versioned together (→ B)?
3. Are Access and Invite same-site in every environment (test/prod hostnames)?
4. ~~Refresh token / RS acceptance~~ **Answered (D1, D2): configurable.** Remaining: refresh-token rotation, TTLs, scope name for Invite (`invite`?), and in which environments it will be configured and when.
5. ~~Is the `sub` identical?~~ **Answered (D3): yes.** Remaining: confirm for eduID/guest/provisioned users (S3).
6. What should an Access user without an Invite account (or an Invite-only user without an Access session) see?
7. Impersonation: should Access impersonation carry over into embedded Invite?
8. Finish SDS → curve-react migration of the Invite pages before embedding (needed for shadow DOM; nice-to-have otherwise)?

## 11. How to continue in a new session
- Read §2 for the current state; uncommitted work lives on branch `feature/access` of `/Users/harst001/projects/invite`
  (git status shows the client/server changes listed above; nothing committed or pushed).
- Dev servers: Invite client `yarn dev` in `client/` (3000), Invite server from IntelliJ (8888, restart after Java changes, Flyway migration `V67_0` already applied locally),
  Access client/server run from `/Users/harst001/projects/access` (3002 / 8886) – apply Access edits there, not only in `invite/access`.
- Verify with Chrome (Claude in Chrome, existing sessions): `http://localhost:3002/home`, `http://localhost:3000/home/roles`; the sessions are real OIDC logins, never enter credentials.
- Useful checks: computed styles (`getComputedStyle`) of `[data-slot="sidebar-menu-button"]` in both apps; `GET /api/v1/users/organizations`; `mysql -uinvite -psecret -h127.0.0.1 invite`.
- Figma MCP is connected (user okke.harsta@surf.nl); main frame node `9904:2329`, sidebar `9841:35923`. Known Access-vs-Figma deviations (not changed because Access leads):
  row height 32 vs 36, group label 12px vs 14/20, organisation logo, extra "Gebruikers" item.
