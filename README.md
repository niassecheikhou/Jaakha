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
.github/   CI backend, web, mobile et workflow deploy manuel
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

GitHub Actions contient :

- `backend-ci.yml` : tests Maven Java 21
- `web-ci.yml` : `npm ci`, lint et build Vite
- `mobile-ci.yml` : `npm ci` et lint Expo
- `deploy.yml` : workflow manuel qui package backend/web et construit l'image Docker backend

Le workflow `deploy.yml` est volontairement cible-neutre pour l'instant. Il faut ajouter les etapes propres a l'hebergeur choisi lorsque la destination staging/production sera connue.
