# Partie 3 — Mise en œuvre

# Chapitre 6 : Réalisation — développement

*(Phase 3 du cycle de développement : réalisation)*

Ce chapitre décrit la mise en œuvre concrète de la solution : l'environnement de développement,
l'implémentation des fonctionnalités, l'organisation du code et la gestion des erreurs et de la sécurité.

## 6.1 Mise en place de l'environnement de développement

*Tableau 14 : Environnement de développement*

| Élément | Outil et version |
|---|---|
| Système | Windows 11, terminal PowerShell et Git Bash |
| Éditeur | Visual Studio Code |
| Backend | Java 21, Spring Boot 4.1.0, Maven (via `mvnw`) |
| Frontend | Node.js 22, React 19, TypeScript 6, Vite 8, ESLint 10 |
| Mobile | Expo 54, React Native 0.81, Expo Router 6 (squelette) |
| Base de données | PostgreSQL 17 (Docker) ; H2 en fichier sans Docker et H2 en mémoire pour les tests |
| Conteneurs | Docker Desktop, Docker Compose |
| Gestion de versions | Git, dépôt GitHub, une branche par fonctionnalité |

Le dépôt est organisé en trois applications :

```text
Jaakha/
├── backend/            API REST Spring Boot (package sn.jaakha.backend)
│   └── src/main/java/sn/jaakha/backend/
│       ├── presentation/      contrôleurs REST et gestion des erreurs
│       ├── application/       services métier et DTO
│       ├── domain/            entités, énumérations, dépôts JPA
│       └── infrastructure/    persistance JDBC, sécurité, stockage
├── web/                Application React + TypeScript (Vite)
│   └── src/ pages/, features/, components/, services/, types/, utils/
├── mobile/             Application Expo (dépôt Git séparé, squelette)
├── .github/            Pipeline CI/CD et tests de fumée
└── docker-compose.yml  Orchestration : PostgreSQL, backend, web
```

Les commandes de travail sont :

```bash
cd backend && ./mvnw spring-boot:run      # API sur le port 8080
cd web && npm install && npm run dev      # interface sur le port 5173
docker compose up --build                 # pile complète, interface sur le port 8081
```

## 6.2 Implémentation des fonctionnalités

### 6.2.1 Authentification

L'inscription crée un compte **non validé** (`active = false`) et ouvre directement une session : l'utilisateur
peut compléter son dossier, mais son compte ne devient valide qu'après le contrôle de l'administrateur. La
connexion accepte indifféremment l'email ou le numéro de téléphone. La réinitialisation du mot de passe
envoie un lien par email ; le jeton n'est conservé en base que sous forme hachée (SHA-256), expire au bout de
30 minutes, n'est utilisable qu'une fois, et sa consommation ferme toutes les sessions ouvertes du compte.

Chaque requête protégée est authentifiée par le service `CurrentUserService`, appelé dans les contrôleurs :

```java
// CurrentUserService.requireUser : le jeton doit exister, ne pas être expiré,
// et le compte ne doit pas être archivé.
return jdbcTemplate.query("""
        select u.id, u.full_name, u.role
        from session_token st
        join app_user u on u.id = st.user_id
        where st.token = ?
          and st.expires_at > current_timestamp
          and u.archived = false
        """, ...);
```

### 6.2.2 Dossier de l'utilisateur : photo et pièce d'identité

Tout utilisateur connecté (employeur, prestataire ou administrateur) peut déposer une **photo de profil**,
affichée dans son espace sous forme d'avatar. Pour un prestataire, la photo est recopiée immédiatement sur
son profil public.

La **pièce d'identité** se dépose en deux faces, **recto et verso**. L'interface propose deux emplacements
avec envoi immédiat, ou une sélection groupée des deux images. Comme l'ordre des fichiers renvoyé par le
navigateur n'est pas fiable, l'utilisateur voit l'attribution recto/verso et peut l'**inverser** avant l'envoi.
Côté serveur, les deux fichiers sont validés avant toute écriture, puis stockés dans un dossier privé sous
un nom aléatoire (UUID) :

```java
// StorageService : seul un nom de fichier généré par le stockage (UUID + extension)
// peut être résolu ; aucun chemin arbitraire n'est accepté.
private Optional<Path> identityFile(String storedValue) {
    String filename = storedValue == null ? "" : storedValue.substring(storedValue.lastIndexOf('/') + 1);
    if (!STORED_FILENAME.matcher(filename).matches()) {
        return Optional.empty();
    }
    return Optional.of(privateUploadPath.resolve(IDENTITY_FOLDER).resolve(filename).normalize());
}
```

### 6.2.3 Recherche et profil public des prestataires

La recherche publique filtre les prestataires par zone, compétence et tarif mensuel maximal. Seuls
apparaissent les profils **validés** dont le compte est **actif et non archivé** (règle RG02). Le détail d'un
prestataire affiche sa note moyenne et ses avis, recalculés à chaque nouvel avis.

### 6.2.4 Réservations et cycle de vie

Un employeur réserve un prestataire validé ; la réservation est créée au statut *Demandée* et les
administrateurs ainsi que le prestataire sont notifiés. Le cycle de vie est garanti par une politique unique,
partagée par toutes les routes de modification :

```java
// BookingStatusPolicy : transitions autorisées du cycle de vie (règle RG03).
private static final Map<String, Set<String>> TRANSITIONS = Map.of(
        "REQUESTED", Set.of("ACCEPTED", "CANCELLED"),
        "ACCEPTED", Set.of("IN_PROGRESS", "CANCELLED"),
        "IN_PROGRESS", Set.of("DONE", "CANCELLED"),
        "DONE", Set.of(),
        "CANCELLED", Set.of()
);
private static final Set<String> EMPLOYER_CANCELLABLE = Set.of("REQUESTED", "ACCEPTED");
private static final Set<String> PROVIDER_TARGETS = Set.of("ACCEPTED", "IN_PROGRESS", "DONE");
```

La mise à jour en base est conditionnée au statut lu (`UPDATE … WHERE id = ? AND status = ?`) : si deux
personnes modifient la même réservation au même moment, la seconde reçoit une erreur 409 au lieu d'écraser
la première (verrouillage optimiste).

Dans l'interface d'administration, un menu d'actions « ⋮ » affiche tout le parcours restant d'une
réservation : l'étape suivante est cliquable et indique le nouveau statut, les suivantes sont grisées avec
leur condition (« Après acceptation »), et l'annulation, définitive, demande une confirmation.

### 6.2.5 Avis

Un employeur peut noter de 1 à 5, avec un commentaire facultatif, une réservation **terminée** dont il est
l'auteur, une seule fois (contrainte d'unicité sur `booking_id`). La note moyenne et le nombre d'avis du
prestataire sont alors recalculés.

### 6.2.6 Administration

L'espace administrateur regroupe :

- **un tableau de bord** : nombre d'utilisateurs, de profils en attente, de réservations totales et en attente,
  graphique et camembert des réservations, activités récentes. Une barre « Période d'analyse » filtre tous
  ces indicateurs par mois (par semaine dans le graphique), trimestre, semestre ou année ;
- **la gestion des comptes** : création unitaire ou import CSV, validation, invalidation, archivage et
  restauration, consultation du dossier (photo, recto et verso de la pièce d'identité) ;
- **la validation des profils prestataires** ;
- **la gestion des réservations** ;
- **les messages de contact**, exportables en CSV ou en Excel ;
- **son propre profil** (photo).

La validation d'un compte est refusée tant que le dossier est incomplet, avec la liste des champs manquants
(règle RG01).

[À COMPLÉTER : insérer les captures d'écran des figures 11 à 16.]

*Figure 11 : Page d'accueil* — [À COMPLÉTER : capture]

*Figure 12 : Formulaire d'inscription* — [À COMPLÉTER : capture]

*Figure 13 : Dossier prestataire (photo de profil et pièce d'identité)* — [À COMPLÉTER : capture]

*Figure 14 : Recherche de prestataires* — [À COMPLÉTER : capture]

*Figure 15 : Tableau de bord administrateur avec filtre par période* — [À COMPLÉTER : capture]

*Figure 16 : Gestion des réservations et menu d'actions* — [À COMPLÉTER : capture]

## 6.3 Organisation du code et interactions entre les composants

**Côté serveur**, une requête traverse les couches dans un ordre fixe : le contrôleur authentifie
l'utilisateur et valide le DTO reçu, le service applique les règles métier, puis l'infrastructure lit ou écrit
en base et sur le disque. Les DTO sont des `record` Java immuables, annotés avec les contraintes Jakarta
Validation (`@NotBlank`, `@Pattern`, `@Size`…).

**Côté client**, l'application est découpée par fonctionnalité (`features/auth`, `features/admin`,
`features/employer`, `features/providers`…). Les appels réseau sont centralisés dans `services/api.ts` : un
intercepteur Axios ajoute le jeton à chaque requête et efface la session en cas de réponse 401. TanStack Query
met les données en cache et les rafraîchit après chaque modification. Les routes protégées vérifient le rôle
avant d'afficher un espace (`ProtectedRoute`).

**En production**, le serveur Nginx qui sert l'application relaie `/api` et `/uploads` vers le backend : le
navigateur ne parle qu'à une seule origine, ce qui supprime les problèmes de CORS.

## 6.4 Gestion des erreurs et sécurité

**Gestion des erreurs.** Un gestionnaire global (`ApiExceptionHandler`) transforme les exceptions en réponses
JSON homogènes `{ status, message, fields }` : 400 pour une donnée invalide (avec le détail par champ), 401
pour une session absente ou expirée, 403 pour un rôle insuffisant, 409 pour un conflit (compte existant,
transition interdite, modification concurrente). L'interface affiche ces messages dans les formulaires et
sous forme de notifications.

*Tableau 15 : Mesures de sécurité implémentées*

| Menace | Mesure |
|---|---|
| Vol de mots de passe | Hachage BCrypt ; politique de complexité |
| Accès non autorisé | Jeton de session vérifié à chaque requête ; rôle contrôlé dans chaque contrôleur (`requireUser`, `requireAdmin`) |
| Élévation de privilèges | Rôle limité à `EMPLOYEUR` ou `PRESTATAIRE` à l'inscription ; un compte admin ne peut pas être modifié depuis l'interface |
| Exposition des pièces d'identité | Stockage privé, distinct du dossier public ; routes protégées ; en-têtes `Cache-Control: no-store` et `X-Content-Type-Options: nosniff` |
| Parcours de répertoires | Seuls des noms de fichiers générés (UUID) peuvent être résolus |
| Fichiers malveillants ou trop lourds | Types limités à JPG, PNG, WEBP ; taille limitée à 5 Mo ; validation avant écriture |
| Dépôt de fichiers anonyme | Authentification vérifiée **avant** toute écriture sur le disque |
| Données invalides | Validation côté serveur (email, téléphone sénégalais, tailles) et côté client |
| Manipulation des réservations | Cycle de vie imposé par le serveur ; verrouillage optimiste |
| Avis frauduleux | Avis réservé à l'employeur, sur une mission terminée, une seule fois |
| Injection SQL | Requêtes paramétrées exclusivement |
| Injection HTML dans les emails et les exports | Échappement des contenus saisis |
| Secrets dans le code | Configuration par variables d'environnement ; aucun mot de passe administrateur par défaut |
| Exécution des conteneurs | Utilisateurs non-root dans les images backend et web |

**Audit de sécurité.** Un audit complet du code a été mené pendant le projet. Il a conduit à corriger quatre
failles critiques : dépôt de fichiers possible sans authentification, absence de règles de transition des
réservations, adresse de photo modifiable par le client (qui permettait de contourner le contrôle du dossier)
et mot de passe administrateur par défaut écrit dans le code. Ces corrections sont détaillées au chapitre 7.

## 6.5 Limites de la réalisation

Par honnêteté vis-à-vis du jury, les limites suivantes, identifiées lors de l'audit, sont signalées :

- la réservation ne comporte pas encore de date, d'adresse ni de durée saisies par l'employeur (valeurs par
  défaut) ;
- les disponibilités des prestataires ne sont pas enregistrées ;
- l'interface du prestataire ne propose pas encore de boutons pour accepter, démarrer ou terminer une
  mission : ces actions sont disponibles via l'API et depuis l'espace administrateur ;
- la validation d'un compte n'empêche pas encore un employeur non validé de réserver ;
- il n'y a pas de limitation du nombre de tentatives de connexion ni de déconnexion côté serveur ;
- l'application mobile Expo n'en est qu'au stade de squelette.

Ces points constituent les premières perspectives d'évolution (conclusion générale).
