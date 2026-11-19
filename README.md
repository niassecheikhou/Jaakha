# Jaankha

Jaankha est une plateforme de mise en relation a Dakar entre employeurs et prestataires de services.

## Objectif MVP

Le MVP couvre les parcours essentiels sur 90 jours :

- inscription et connexion par email ou telephone avec roles `EMPLOYEUR`, `PRESTATAIRE`, `ADMIN`
- profils prestataires avec photo, bio, zone, tarif horaire, competences, experience et disponibilites
- recherche publique avec filtres par zone, prix, note, competence et jour disponible
- reservation avec statut `REQUESTED`, `ACCEPTED`, `IN_PROGRESS`, `DONE`, `CANCELLED`
- notification dashboard et email admin lorsqu'une reservation est demandee
- validation admin des prestataires et gestion des reservations/utilisateurs/signalements
- avis note 1 a 5 uniquement apres une reservation terminee

## Architecture

```text
backend/   API Spring Boot, JPA, Flyway, Security, Mail
web/       Application React/Vite
mobile/    Application Expo
.github/   pipeline CI (ci.yml), livraison (deploy.yml) et tests de fumee
```

Le backend suit la structure deja initiee :

- `domain` : modeles, enums, repositories, exceptions
- `application` : services metier et DTO
- `infrastructure` : securite, mail, bootstrap admin
- `presentation` : controleurs REST et gestion d'erreurs

## Demarrage local

1. Creer le fichier local d'environnement :

```bash
cp .env.example .env
```

2. Adapter au minimum `POSTGRES_PASSWORD`, `DATABASE_PASSWORD`, `ADMIN_EMAIL` et `ADMIN_PASSWORD`.

3. Lancer Postgres et l'API :

```bash
docker compose up --build
```

L'API sera disponible sur `http://localhost:8080`.

Documentation OpenAPI :

```text
http://localhost:8080/swagger-ui.html
```

Healthcheck :

```text
GET http://localhost:8080/actuator/health
```

## Backend sans Docker

```bash
cd backend
./mvnw spring-boot:run
```

Sous Windows :

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

## Variables principales

Voir `.env.example` pour la liste complete.

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`
- `ADMIN_EMAIL`, `ADMIN_PHONE`, `ADMIN_PASSWORD`
- `VITE_API_BASE_URL`, `EXPO_PUBLIC_API_BASE_URL`

Si `ADMIN_EMAIL` est renseigne et qu'aucun utilisateur avec cet email n'existe, le backend cree un compte admin au demarrage.

## Auth API

Les routes protegees attendent :

```text
Authorization: Bearer <token>
```

Routes principales :

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/request-otp`
- `POST /api/auth/verify-email`
- `GET /api/providers`
- `GET /api/providers/{id}`
- `PUT /api/providers/me`
- `POST /api/bookings`
- `GET /api/bookings/me`
- `PATCH /api/bookings/{id}/status`
- `POST /api/reviews`
- `POST /api/reports`
- `GET /api/notifications`
- `PATCH /api/notifications/{id}/read`

Routes admin :

- `GET /api/admin/users`
- `POST /api/admin/users`
- `PATCH /api/admin/users/{id}/active?active=true`
- `GET /api/admin/providers`
- `PATCH /api/admin/providers/{id}/status`
- `GET /api/admin/bookings`
- `PATCH /api/admin/bookings/{id}/status`
- `GET /api/admin/reports`
- `PATCH /api/admin/reports/{id}/status`

## CI/CD

### Integration continue : `.github/workflows/ci.yml`

Lancee sur **chaque pull request** (sans filtre de chemins) :

| Job | Contenu |
|-----|---------|
| `Backend (tests Maven)` | `./mvnw verify` : compilation, tests JUnit/Mockito (H2 en memoire), packaging |
| `Web (lint, build, audit)` | `npm ci`, ESLint, build TypeScript strict + Vite, `npm audit` (niveau high) |
| `Docker (stack complete + tests de fumee)` | `docker compose up --wait` puis `.github/scripts/smoke-test.sh` : Nginx, SPA, API via proxy, inscription en base, 401/403, dossier prive non expose |
| `CI OK` | Vert seulement si les trois jobs precedents sont verts |

**Regle de fusion :** `CI OK` est le controle obligatoire de la protection de la branche `main`.
Une PR ne peut etre fusionnee que si tout le pipeline est vert.

Le mobile (`mobile/`) est un depot git separe : sa CI doit vivre dans son propre depot.

### Livraison continue : `.github/workflows/deploy.yml`

- **Fusion sur `main`** : relance `ci.yml` sur le commit fusionne, publie les images
  `ghcr.io/<owner>/jaakha-backend` et `ghcr.io/<owner>/jaakha-web` (tags `<sha>` et `staging`),
  puis appelle les deploy hooks de l'environnement `staging`.
- **Production** : Actions > Deploy > Run workflow > `production` (validation manuelle possible
  via les regles de l'environnement GitHub `production`).
- Secrets par environnement : `BACKEND_DEPLOY_HOOK_URL`, `WEB_DEPLOY_HOOK_URL` (URL de deploy hook
  Render/Railway). Sans hook configure, les images sont publiees et un avertissement est affiche.

### Tester la stack complete en local

```bash
docker compose up --build -d --wait
bash .github/scripts/smoke-test.sh http://localhost:8081 http://localhost:8080
```
