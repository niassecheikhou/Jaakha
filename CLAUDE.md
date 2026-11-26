# JAANKHA — Contexte projet pour Claude Code

## Le projet
JAANKHA est une plateforme de mise en relation entre femmes de ménage et employeurs.
Objectif : résoudre le manque de confiance et de sécurité entre les deux parties
(profils vérifiés, avis, historique, contrats clairs).
Projet de soutenance de Licence en Génie Logiciel.

## Structure du dépôt
- `backend/` : API REST Spring Boot (Java, Maven via `mvnw`). Code dans `backend/src/`
- `web/` : application web React + TypeScript + Vite. Code dans `web/src/`
- `mobile/` : application React Native avec Expo (TypeScript, Expo Router dans `mobile/app/`).
  Encore au stade du template Expo. Ce dossier est un dépôt git séparé, avec son propre
  `mobile/CLAUDE.md` et `mobile/AGENTS.md` : les lire avant de toucher au mobile.
- `.github/workflows/` : `ci.yml` (backend, web, stack Docker + tests de fumee, job `CI OK` obligatoire
  pour fusionner sur main) et `deploy.yml` (CI puis images GHCR puis deploy hooks staging/production)
- `data/`, `backend/data/` : données locales (ne pas modifier sans demande)
- `docker-compose.yml` : orchestration des services (à la racine)
- `docs/memoire/` : mémoire de soutenance (créé par l'agent redacteur-memoire)

À ignorer (générés) : `backend/target/`, `web/dist/`, `node_modules/`, `mobile/.expo/`

## Commandes (Windows / PowerShell)
- Backend : `cd backend; .\mvnw.cmd spring-boot:run` — tests : `.\mvnw.cmd test`
- Web : `cd web; npm install; npm run dev` — build : `npm run build` — lint : `npm run lint`
- Mobile : `cd mobile; npm install; npx expo start` — lint : `npm run lint`
- Tout le projet : `docker compose up --build` (depuis la racine) ; web sur http://localhost:8081
  - tests de fumee : `bash .github/scripts/smoke-test.sh http://localhost:8081 http://localhost:8080`

## Conventions
- Code et noms de variables en anglais, commentaires et docs en français
- Backend (`sn.jaakha.backend`) : `presentation` (contrôleurs REST) → `application` (services + DTO)
  → `infrastructure` (persistance JdbcTemplate, sécurité, stockage) / `domain` (entités JPA, repositories)
- Base : PostgreSQL via Docker, H2 fichier en local sans Docker ; schéma géré par `ddl-auto=update`
  + `UserAccountSchemaInitializer` (Flyway présent mais désactivé)
- Rôles : `EMPLOYEUR`, `PRESTATAIRE`, `ADMIN`, `SUPER_ADMIN` (aucun autre). Le `SUPER_ADMIN` (compte initial) a tous les droits
  d'un admin et est seul à créer/gérer les comptes `ADMIN` ; un `ADMIN` gère les employeurs et prestataires
- Sécurité : pas de JWT. Token de session opaque (table `session_token`) envoyé en `Authorization: Bearer`,
  vérifié par `CurrentUserService.requireUser` / `requireAdmin` dans chaque contrôleur
  (Spring Security est en `permitAll`). Aucun secret en dur, uniquement des variables d'environnement
- Pièces d'identité : stockage privé (`PRIVATE_UPLOAD_DIR`), lues uniquement via
  `/api/account/me/identity-photo` et `/api/admin/users/{id}/identity-photo`
- Web et mobile : TypeScript strict, pas de `any` sans justification
- Git : un commit par fonctionnalité, messages `feat:`, `fix:`, `docs:`, `chore:`
- Toujours lancer les tests / le lint avant de dire qu'une tâche est terminée

## Agents disponibles (.claude/agents/)
- `developpeur` : implémente les fonctionnalités et écrit les tests
- `deployeur` : Docker, CI/CD, mise en ligne
- `redacteur-memoire` : rédige le mémoire de soutenance à partir du code réel

## Règles importantes
- Ne jamais pousser sur `main` ni déployer sans ma validation explicite
- Ne jamais lire, modifier ni afficher `.env` et `web/.env` ; utiliser `.env.example` comme référence
