# Chapitre 4 : Conception fonctionnelle et technique du système

*(Phase 2 du cycle de développement : conception)*

Ce chapitre modélise la solution avant sa réalisation. Il présente la modélisation UML, le modèle de
données, l'architecture du système et la justification des choix techniques.

> Les diagrammes sont rédigés en Mermaid ci-dessous et **déjà exportés en images** dans le dossier
> [`figures/`](figures/) (`figure-02-diagramme-contexte.png` à `figure-10-architecture-couches.png`) :
> ce sont ces images qu'il faut insérer dans le document final.

## 4.1 Modélisation UML

### 4.1.1 Diagramme de contexte

Le diagramme de contexte présente le système comme une boîte noire et ses échanges avec les acteurs
externes.

*Figure 2 : Diagramme de contexte*

```mermaid
flowchart LR
    V([Visiteur]) -- "consulte, recherche,<br/>s'inscrit, contacte" --> S
    E([Employeur]) -- "dossier, réservations,<br/>avis" --> S
    P([Prestataire]) -- "dossier, compétences,<br/>missions" --> S
    A([Administrateur]) -- "validation, pilotage,<br/>indicateurs" --> S
    S[["Plateforme JAANKHA"]]
    S -- "notifications, statuts" --> E
    S -- "demandes reçues" --> P
    S -- "emails : bienvenue,<br/>réinitialisation, accusé" --> M[(Serveur SMTP)]
```

### 4.1.2 Diagramme de cas d'utilisation

*Figure 3 : Diagramme de cas d'utilisation*

```mermaid
flowchart LR
    Visiteur([Visiteur])
    Employeur([Employeur])
    Prestataire([Prestataire])
    Admin([Administrateur])

    subgraph JAANKHA["Système JAANKHA"]
        UC1([Rechercher un prestataire])
        UC2([Consulter un profil et ses avis])
        UC3([Créer un compte])
        UC4([Envoyer un message de contact])
        UC5([Se connecter / réinitialiser le mot de passe])
        UC6([Gérer son dossier :<br/>photo, CNI recto/verso])
        UC7([Réserver un prestataire])
        UC8([Annuler une réservation])
        UC9([Noter une mission terminée])
        UC10([Accepter, démarrer,<br/>terminer une mission])
        UC11([Consulter ses notifications])
        UC12([Valider / invalider / archiver un compte])
        UC13([Valider / rejeter un profil prestataire])
        UC14([Piloter les réservations])
        UC15([Créer des comptes / importer un CSV])
        UC16([Consulter les indicateurs par période])
        UC17([Consulter et exporter les messages])
    end

    Visiteur --- UC1 & UC2 & UC3 & UC4
    Employeur --- UC5 & UC6 & UC7 & UC8 & UC9 & UC11 & UC1
    Prestataire --- UC5 & UC6 & UC10 & UC11
    Admin --- UC5 & UC12 & UC13 & UC14 & UC15 & UC16 & UC17
    UC7 -. "«include» être connecté<br/>et compte prestataire validé" .-> UC5
```

### 4.1.3 Diagramme de classes

Le diagramme de classes représente les principales entités du domaine et leurs relations. Il reflète le
schéma réel : une partie du modèle est gérée par JPA (`AccountProfile`, `ProviderProfile`), l'autre par
requêtes SQL via JdbcTemplate (`AppUser`, `Booking`, `Review`, `Notification`…).

*Figure 4 : Diagramme de classes*

```mermaid
classDiagram
    class AppUser {
        +UUID id
        +String fullName
        +String email
        +String phone
        -String passwordHash
        +Role role
        +boolean emailVerified
        +boolean active
        +boolean archived
        +Instant createdAt
    }
    class Role {
        <<enumeration>>
        EMPLOYEUR
        PRESTATAIRE
        ADMIN
    }
    class AccountProfile {
        +UUID id
        +String ownerKey
        +String photoUrl
        -String identityPhotoUrl
        -String identityPhotoBackUrl
        +String bio
        +String zone
        +BigDecimal monthlyRate
        +Integer experienceYears
        +String skills
        +String guardianFullName
        +String guardianPhone
        +getIdentityPhoto(side)
        +setIdentityPhoto(side, file)
    }
    class IdentityDocumentSide {
        <<enumeration>>
        FRONT
        BACK
    }
    class ProviderProfile {
        +UUID id
        +UUID userId
        +String fullName
        +String photoUrl
        +String bio
        +String zone
        +BigDecimal monthlyRate
        +int experienceYears
        +Set~String~ skills
        +ProviderStatus status
        +BigDecimal averageRating
        +int reviewCount
    }
    class ProviderStatus {
        <<enumeration>>
        PENDING
        APPROVED
        REJECTED
    }
    class Booking {
        +UUID id
        +BookingStatus status
        +LocalDate serviceDate
        +LocalTime startTime
        +int durationHours
        +String address
        +String instructions
        +Instant createdAt
    }
    class BookingStatus {
        <<enumeration>>
        REQUESTED
        ACCEPTED
        IN_PROGRESS
        DONE
        CANCELLED
    }
    class Review {
        +UUID id
        +int rating
        +String comment
        +Instant createdAt
    }
    class Notification {
        +UUID id
        +String type
        +String title
        +String message
        +boolean read
        +Instant createdAt
    }
    class SessionToken {
        +UUID id
        -String token
        +Instant expiresAt
    }
    class PasswordResetToken {
        +UUID id
        -String tokenHash
        +Instant expiresAt
        +Instant usedAt
    }
    class ContactMessage {
        +UUID id
        +String name
        +String email
        +String phone
        +String company
        +String message
        +Instant createdAt
    }

    AppUser --> Role
    AppUser "1" -- "0..1" AccountProfile : possède
    AppUser "1" -- "0..1" ProviderProfile : publie (si PRESTATAIRE)
    AccountProfile ..> IdentityDocumentSide
    ProviderProfile --> ProviderStatus
    AppUser "1" -- "0..*" Booking : réserve (employeur)
    ProviderProfile "1" -- "0..*" Booking : reçoit
    Booking --> BookingStatus
    Booking "1" -- "0..1" Review : est notée par
    AppUser "1" -- "0..*" Notification : reçoit
    Booking "0..1" -- "0..*" Notification : concerne
    AppUser "1" -- "0..*" SessionToken : s'authentifie avec
    AppUser "1" -- "0..*" PasswordResetToken : réinitialise avec
```

### 4.1.4 Diagrammes de séquence

**Inscription et connexion.** Le serveur ne délivre pas de jeton JWT : il génère un jeton de session opaque,
stocké en base avec une date d'expiration, que le client renvoie dans l'en-tête `Authorization`.

*Figure 5 : Diagramme de séquence — inscription et connexion*

```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant W as Application web (React)
    participant C as AuthController
    participant S as AuthService
    participant DB as PostgreSQL
    participant M as Serveur SMTP

    U->>W: Remplit le formulaire d'inscription
    W->>W: Validation (email, téléphone +221, mot de passe)
    W->>C: POST /api/auth/register
    C->>C: Validation des contraintes du DTO (@Valid)
    C->>S: register(requête)
    S->>DB: INSERT app_user (mot de passe haché BCrypt, active = false)
    alt email ou téléphone déjà utilisé
        DB-->>S: violation d'unicité
        S-->>W: 409 « Un compte existe déjà »
    else succès
        S->>M: Email de bienvenue
        S->>DB: INSERT session_token (jeton aléatoire, expiration)
        S-->>W: 201 { token, user }
        W->>W: Mémorise la session, redirige vers l'espace du rôle
    end

    Note over U,DB: Requêtes suivantes
    W->>C: GET /api/... avec « Authorization: Bearer <jeton> »
    C->>DB: Jeton valide, non expiré, compte non archivé ?
    DB-->>C: Utilisateur et rôle
```

**Dépôt de la pièce d'identité.** Les deux faces peuvent être envoyées ensemble ou séparément. Les deux
fichiers sont validés avant toute écriture, et l'ancien fichier remplacé n'est supprimé qu'après la
validation de la transaction.

*Figure 6 : Diagramme de séquence — dépôt de la pièce d'identité (recto et verso)*

```mermaid
sequenceDiagram
    actor U as Employeur ou prestataire
    participant W as Application web
    participant C as AccountProfileController
    participant St as StorageService
    participant S as AccountProfileService
    participant DB as PostgreSQL
    participant FS as Stockage privé

    U->>W: Sélectionne recto et verso
    W->>W: Aperçu, possibilité d'inverser les faces
    W->>C: POST /api/account/me/identity-photos (front, back)
    C->>C: requireUser (jeton de session)
    C->>St: validateImage(front) puis validateImage(back)
    alt format ou taille refusés
        St-->>W: 400, aucun fichier écrit
    else fichiers valides
        C->>St: storeIdentityPhoto(front), storeIdentityPhoto(back)
        St->>FS: Écrit sous un nom UUID (dossier privé)
        C->>S: updateIdentityPhotos(propriétaire, fichiers)
        S->>DB: UPDATE account_profile (recto, verso)
        S-->>FS: Après validation de la transaction : suppression des anciens fichiers
        C-->>W: 200 { URL protégée du recto, URL protégée du verso }
    end
```

**Réservation et changement de statut.**

*Figure 7 : Diagramme de séquence — réservation et changement de statut*

```mermaid
sequenceDiagram
    actor E as Employeur
    actor A as Administrateur
    participant B as BookingController / AdminController
    participant P as BookingStatusPolicy
    participant R as BookingRepository
    participant N as NotificationService

    E->>B: POST /api/bookings { providerId }
    B->>B: Rôle EMPLOYEUR ? Prestataire validé et actif ?
    B->>R: INSERT booking (status = REQUESTED)
    B->>N: Notifie les administrateurs et le prestataire

    A->>B: PATCH /api/admin/bookings/{id}/status { ACCEPTED }
    B->>R: Lecture du statut courant
    B->>P: assertTransition(ADMIN, REQUESTED, ACCEPTED)
    alt transition interdite (ex. REQUESTED → DONE)
        P-->>A: 409 « Transition impossible »
    else transition autorisée
        B->>R: UPDATE ... WHERE id = ? AND status = statut lu
        alt statut modifié entre-temps
            R-->>A: 409 « La réservation a été modifiée entre-temps »
        else
            B->>N: Notifie l'employeur et le prestataire
            B-->>A: 200 réservation mise à jour
        end
    end
```

### 4.1.5 Diagramme d'états d'une réservation

*Figure 8 : Diagramme d'états d'une réservation*

```mermaid
stateDiagram-v2
    [*] --> REQUESTED : l'employeur réserve
    REQUESTED --> ACCEPTED : prestataire ou administrateur
    REQUESTED --> CANCELLED : employeur ou administrateur
    ACCEPTED --> IN_PROGRESS : prestataire ou administrateur
    ACCEPTED --> CANCELLED : employeur ou administrateur
    IN_PROGRESS --> DONE : prestataire ou administrateur
    IN_PROGRESS --> CANCELLED : administrateur
    DONE --> [*] : l'employeur peut noter la mission
    CANCELLED --> [*]
```

## 4.2 Modélisation de la base de données

### 4.2.1 Modèle conceptuel de données (MCD)

*Figure 9 : Modèle conceptuel de données*

```mermaid
erDiagram
    APP_USER ||--o| ACCOUNT_PROFILE : "possède"
    APP_USER ||--o| PROVIDER_PROFILE : "publie"
    PROVIDER_PROFILE ||--o{ PROVIDER_SKILL : "déclare"
    APP_USER ||--o{ BOOKING : "réserve"
    PROVIDER_PROFILE ||--o{ BOOKING : "reçoit"
    BOOKING ||--o| REVIEW : "est évaluée par"
    APP_USER ||--o{ REVIEW : "rédige"
    APP_USER ||--o{ NOTIFICATION : "reçoit"
    BOOKING |o--o{ NOTIFICATION : "concerne"
    APP_USER ||--o{ SESSION_TOKEN : "ouvre"
    APP_USER ||--o{ PASSWORD_RESET_TOKEN : "demande"

    APP_USER {
        uuid id PK
        string full_name
        string email UK
        string phone UK
        string password_hash
        string role
        boolean active
        boolean archived
    }
    ACCOUNT_PROFILE {
        uuid id PK
        string owner_key UK
        string photo_url
        string identity_photo_url
        string identity_photo_back_url
        string zone
    }
    PROVIDER_PROFILE {
        uuid id PK
        uuid user_id UK
        string status
        decimal hourly_rate
        decimal average_rating
    }
    BOOKING {
        uuid id PK
        string status
        date service_date
        uuid employer_id FK
        uuid provider_id FK
    }
    REVIEW {
        uuid id PK
        uuid booking_id UK
        int rating
    }
```

La table `CONTACT_MESSAGE` est indépendante : elle stocke les messages envoyés depuis le formulaire public.

### 4.2.2 Modèle logique de données (MLD)

Le MLD se déduit du MCD : chaque entité devient une table, chaque association « un à plusieurs » est
traduite par une clé étrangère du côté « plusieurs », et la liste des compétences d'un prestataire devient
une table dédiée (`provider_skill`).

*Tableau 8 : Dictionnaire du modèle logique de données*

| Table | Colonnes principales | Remarques |
|---|---|---|
| `app_user` | **id**, full_name, email (unique), phone (unique), password_hash, role, email_verified, active, archived, created_at, updated_at | Comptes des trois rôles |
| `session_token` | **id**, token (unique), *user_id*, expires_at, created_at | Sessions ; durée de vie de 720 heures par défaut |
| `password_reset_token` | **id**, *user_id*, token_hash (unique), expires_at, used_at, created_at | Le jeton est stocké haché (SHA-256), durée de vie de 30 minutes, usage unique |
| `account_profile` | **id**, owner_key (unique, = identifiant de l'utilisateur), photo_url, identity_photo_url (recto), identity_photo_back_url (verso), bio, zone, monthly_rate, experience_years, skills, guardian_full_name, guardian_phone, created_at, updated_at | Dossier de l'utilisateur, géré par JPA |
| `provider_profile` | **id**, owner_key (unique), *user_id* (unique), full_name, photo_url, bio, zone, hourly_rate, experience_years, status, average_rating, review_count, created_at, updated_at | Profil public du prestataire ; `hourly_rate` contient le tarif **mensuel** |
| `provider_skill` | *provider_id*, skill | Compétences d'un prestataire |
| `booking` | **id**, status, service_date, start_time, duration_hours, address, instructions, *employer_id*, *provider_id*, created_at, updated_at | Réservations |
| `review` | **id**, *booking_id* (unique), rating, comment, *employer_id*, *provider_id*, created_at | Un seul avis par réservation |
| `notification` | **id**, type, title, message, *booking_id*, *recipient_id*, read, created_at | Notifications internes |
| `contact_message` | **id**, name, email, phone, company, message, created_at | Messages du formulaire de contact |

Légende : **gras** = clé primaire (UUID), *italique* = clé étrangère logique.

> Les clés étrangères sont aujourd'hui logiques : elles ne sont pas déclarées comme contraintes en base.
> L'intégrité est assurée par l'application ; l'ajout de contraintes `FOREIGN KEY` fait partie des
> perspectives (conclusion générale).

## 4.3 Architecture du système

### 4.3.1 Architecture client-serveur en couches

La solution suit une architecture **client-serveur** : une application monopage (SPA) React s'exécute dans
le navigateur et dialogue en JSON avec une API REST Spring Boot, elle-même connectée à PostgreSQL. Le
serveur est organisé en **quatre couches**, inspirées de l'architecture hexagonale et du modèle MVC :

- **Présentation** (`presentation`) : contrôleurs REST, gestion centralisée des erreurs, contrôle d'accès ;
- **Application** (`application`) : services métier et objets de transfert de données (DTO) ;
- **Domaine** (`domain`) : entités, énumérations et interfaces de dépôt ;
- **Infrastructure** (`infrastructure`) : persistance (JdbcTemplate), sécurité, stockage des fichiers.

*Figure 10 : Architecture logicielle en couches*

```mermaid
flowchart TB
    subgraph Client["Navigateur"]
        SPA["Application web React + TypeScript<br/>(React Router, TanStack Query, Axios)"]
    end
    subgraph Serveur["API Spring Boot (Java 21)"]
        direction TB
        PRES["Présentation<br/>AuthController, AccountProfileController, ProviderController,<br/>BookingController, ReviewController, NotificationController,<br/>AdminController, ContactController, ApiExceptionHandler"]
        APP["Application<br/>AuthService, CurrentUserService, AccountProfileService, ProviderService,<br/>BookingService, BookingStatusPolicy, ReviewService, NotificationService,<br/>AdminService, MailService, DTO"]
        DOM["Domaine<br/>AccountProfile, ProviderProfile, ProviderStatus, IdentityDocumentSide,<br/>dépôts JPA"]
        INFRA["Infrastructure<br/>UserAccountRepository, BookingRepository, ReviewRepository…<br/>SecurityConfig, StorageService"]
        PRES --> APP --> DOM
        APP --> INFRA
    end
    DB[(PostgreSQL 17)]
    PUB[/"Fichiers publics<br/>(photos de profil)"/]
    PRIV[/"Fichiers privés<br/>(pièces d'identité)"/]
    SMTP[(Serveur SMTP)]

    SPA -- "HTTPS / JSON<br/>Authorization: Bearer" --> PRES
    INFRA --> DB
    INFRA --> PUB
    INFRA --> PRIV
    APP --> SMTP
```

### 4.3.2 Contrat d'interface : l'API REST

*Tableau 9 : Points d'accès de l'API REST*

| Méthode et chemin | Rôle requis | Description |
|---|---|---|
| `POST /api/auth/register` | Public | Inscription (employeur ou prestataire) |
| `POST /api/auth/login` | Public | Connexion par email ou téléphone |
| `POST /api/auth/forgot-password` | Public | Demande de lien de réinitialisation |
| `POST /api/auth/reset-password` | Public (jeton) | Choix d'un nouveau mot de passe |
| `GET`, `PUT /api/account/me` | Connecté | Lecture et mise à jour du dossier |
| `POST /api/account/me/photo` | Connecté | Dépôt de la photo de profil |
| `POST /api/account/me/identity-photos` | Connecté | Dépôt du recto et/ou du verso de la pièce d'identité |
| `GET /api/account/me/identity-photo[/back]` | Titulaire | Lecture privée du recto ou du verso |
| `GET /api/providers` | Public | Recherche (zone, compétence, tarif maximal) |
| `GET /api/providers/{id}`, `/{id}/reviews` | Public | Détail d'un prestataire validé et ses avis |
| `GET`, `PUT /api/providers/me` | Prestataire | Profil public du prestataire |
| `POST /api/bookings` | Employeur | Création d'une réservation |
| `GET /api/bookings/me` | Employeur, prestataire | Mes réservations |
| `PATCH /api/bookings/{id}/status` | Employeur, prestataire | Changement de statut selon le cycle de vie |
| `POST /api/reviews` | Employeur | Avis sur une réservation terminée |
| `GET /api/notifications`, `PATCH /{id}/read` | Connecté | Notifications |
| `POST /api/contact/messages` | Public | Message de contact |
| `GET`, `POST /api/admin/users` | Administrateur | Liste et création de comptes |
| `PATCH /api/admin/users/{id}/active`, `/archive` | Administrateur | Validation et archivage d'un compte |
| `GET /api/admin/users/{id}/identity-photo[/back]` | Administrateur | Lecture de la pièce d'identité d'un utilisateur |
| `GET /api/admin/providers`, `PATCH /{id}/status` | Administrateur | Validation des profils prestataires |
| `GET /api/admin/bookings`, `PATCH /{id}/status` | Administrateur | Pilotage des réservations |
| `GET /api/admin/contact-messages[/export]` | Administrateur | Messages de contact et export CSV ou Excel |

## 4.4 Justification des choix techniques et technologiques

*Tableau 10 : Choix technologiques et justifications*

| Domaine | Choix | Justification |
|---|---|---|
| Langage serveur | Java 21 (LTS) | Typage fort, version à support long, écosystème mature |
| Framework serveur | Spring Boot 4.1 | Standard industriel pour les API REST ; validation (Jakarta Validation), sécurité, accès aux données et tests intégrés |
| Accès aux données | JdbcTemplate et Spring Data JPA | SQL explicite et maîtrisé pour les requêtes métier ; JPA pour les profils |
| Base de données | PostgreSQL 17 (H2 en développement et en test) | SGBD relationnel libre et robuste ; H2 en mémoire rend les tests rapides et isolés |
| Sécurité | Jeton de session opaque, BCrypt | Révocation simple (suppression du jeton en base), mots de passe jamais stockés en clair |
| Langage client | TypeScript (mode strict) | Détection des erreurs dès la compilation |
| Framework client | React 19 et Vite 8 | Composants réutilisables, compilation et rechargement rapides |
| Navigation et données | React Router 7, TanStack Query 5, Axios | Routage côté client, mise en cache et rafraîchissement automatique des données |
| Conteneurisation | Docker et Docker Compose | Environnement identique en développement, en CI et en production |
| Serveur web | Nginx (non-root) | Sert l'application compilée et relaie l'API sur la même origine, sans CORS |
| Intégration continue | GitHub Actions | Intégré au dépôt, gratuit pour un dépôt public ou un usage modéré |
| Application mobile | Expo (React Native) | Partage du langage TypeScript avec le web ; **au stade de squelette** dans ce projet |

**Pourquoi un jeton de session plutôt qu'un JWT ?** Un JWT est autonome et ne peut pas être révoqué avant
son expiration sans mécanisme supplémentaire. Le jeton opaque stocké en base permet, au contraire, de
fermer toutes les sessions d'un utilisateur instantanément : c'est ce que fait la réinitialisation du mot de
passe, qui supprime toutes les sessions du compte. Pour une plateforme où un compte peut devoir être
suspendu rapidement, ce choix privilégie le contrôle.

**Pourquoi stocker les pièces d'identité séparément ?** Les photos de profil sont publiques et servies
directement. Les pièces d'identité sont des données sensibles : elles sont stockées dans un dossier distinct,
jamais exposé par le serveur web, et ne sont lisibles qu'à travers des routes qui vérifient l'identité du
demandeur.

## Conclusion du chapitre

La conception a défini les acteurs et leurs cas d'utilisation, les entités et leurs relations, les
interactions clés et le cycle de vie des réservations, ainsi qu'une architecture en couches qui sépare
clairement les responsabilités. Le chapitre suivant estime le coût de la réalisation de cette solution.
