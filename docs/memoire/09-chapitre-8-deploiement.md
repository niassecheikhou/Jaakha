# Chapitre 8 : Déploiement de la solution

*(Phase 5 du cycle de développement : déploiement)*

Ce chapitre présente la préparation de l'environnement d'exécution, l'installation de la base de données et
des dépendances, la mise en production et les procédures de sauvegarde.

## 8.1 Préparation et configuration du serveur

La solution est entièrement **conteneurisée** : chaque composant s'exécute dans un conteneur Docker, et
Docker Compose orchestre l'ensemble. Le serveur cible n'a donc besoin que de Docker ; aucune installation de
Java, de Node.js ou de PostgreSQL n'est nécessaire sur la machine hôte.

*Figure 18 : Architecture de déploiement conteneurisée*

```mermaid
flowchart LR
    U([Navigateur]) -- "HTTP(S) :8081" --> WEB
    subgraph Hôte["Serveur (Docker Compose)"]
        WEB["jaankha-web<br/>Nginx non-root<br/>fichiers React compilés"]
        API["jaankha-backend<br/>Spring Boot, Java 21<br/>utilisateur non-root"]
        PG[("jaankha-postgres<br/>PostgreSQL 17")]
        V1[/"volume jaankha_postgres_data"/]
        V2[/"volume jaankha_uploads<br/>(photos publiques)"/]
        V3[/"volume jaankha_private_uploads<br/>(pièces d'identité)"/]
        WEB -- "/api, /uploads" --> API
        API --> PG
        PG --- V1
        API --- V2
        API --- V3
    end
    API -- "SMTP" --> MAIL[(Serveur de messagerie)]
```

Trois images sont utilisées :

- **Backend** (`backend/Dockerfile`) : construction en deux étapes. Une image Maven compile l'application (les
  dépendances sont mises en cache dans une couche distincte), puis une image d'exécution Java 21 minimale
  l'exécute sous un utilisateur dédié non privilégié. Un script de démarrage corrige les droits des volumes
  avant de lancer Java sans les droits root. Une sonde de santé interroge `/actuator/health`.
- **Web** (`web/Dockerfile`) : construction de l'application React avec Node.js, puis service des fichiers par
  une image Nginx non privilégiée. Nginx relaie `/api` et `/uploads` vers le backend, ajoute des en-têtes de
  sécurité et renvoie `index.html` pour toute route de l'application monopage.
- **Base de données** : image officielle `postgres:17-alpine`, avec une sonde de santé `pg_isready`.

Le démarrage est ordonné : le backend attend que la base soit saine, et le web attend que le backend le soit.

La configuration passe exclusivement par des **variables d'environnement**, décrites dans le fichier
`.env.example` ; le fichier `.env` réel, qui contient les secrets, est exclu de Git.

*Tableau 19 : Variables d'environnement de la solution*

| Variable | Rôle |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Base de données |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Connexion du backend à la base |
| `BACKEND_PORT`, `WEB_PORT` | Ports exposés (8080 et 8081 par défaut) |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Origines autorisées à appeler l'API directement |
| `AUTH_TOKEN_TTL_HOURS`, `RESET_TOKEN_TTL_MINUTES` | Durée de vie des sessions (720 h) et des liens de réinitialisation (30 min) |
| `APP_FRONTEND_URL` | Adresse du site, utilisée dans les liens envoyés par email |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `MAIL_FROM` | Envoi des emails |
| `ADMIN_EMAIL`, `ADMIN_PHONE`, `ADMIN_PASSWORD` | Compte administrateur initial (aucun mot de passe par défaut) |
| `UPLOAD_DIR`, `PRIVATE_UPLOAD_DIR` | Dossiers des fichiers publics et privés |
| `WEB_API_BASE_URL` | Adresse de l'API compilée dans le frontend (`/api` par défaut) |

## 8.2 Installation des dépendances et de la base de données

Les dépendances sont installées **pendant la construction des images** : Maven télécharge les bibliothèques
Java et `npm ci` installe les paquets JavaScript à partir du fichier de verrouillage `package-lock.json`, ce
qui garantit des versions identiques sur toutes les machines.

La base de données est initialisée automatiquement au premier démarrage du backend :

- les tables gérées par JPA (`account_profile`, `provider_profile`, `provider_skill`) sont créées ou mises à
  jour par Hibernate (`ddl-auto=update`) ;
- les autres tables (`app_user`, `session_token`, `booking`, `review`, `notification`…) sont créées par le
  composant `UserAccountSchemaInitializer`, de façon idempotente (`create table if not exists`) ;
- le compte administrateur initial est créé à partir des variables `ADMIN_*` s'il n'existe pas encore.

Les étapes d'installation sur un serveur sont donc :

```bash
git clone <url-du-depot> jaankha && cd jaankha
cp .env.example .env          # puis renseigner les mots de passe et le serveur SMTP
docker compose up --build -d --wait
bash .github/scripts/smoke-test.sh http://localhost:8081 http://localhost:8080
```

[Espace réservé : capture de la commande `docker compose up` et des conteneurs à l'état « healthy ».]

## 8.3 Mise en production de l'application

La mise en production est automatisée par le workflow GitHub Actions `deploy.yml` :

1. **à chaque fusion sur `main`**, le pipeline d'intégration continue complet est relancé sur le commit
   fusionné ;
2. s'il réussit, les images du backend et du web sont construites et publiées sur le registre de GitHub
   (`ghcr.io/<propriétaire>/jaakha-backend` et `ghcr.io/<propriétaire>/jaakha-web`), étiquetées avec
   l'identifiant du commit et le nom de l'environnement ;
3. le serveur de production récupère ces images et redémarre les conteneurs (étape décrite ci-dessous).

### 8.3.1 Hébergement retenu : un VPS LWS

L'application est hébergée sur l'offre **VPS M de l'hébergeur LWS** : 4 vCore, 8 Go de mémoire, 150 Go de
stockage SSD NVMe et accès root (budget de 60 000 FCFA par an, voir chapitre 5). Le choix d'un VPS plutôt que d'un hébergement mutualisé est imposé par l'architecture : un
hébergement mutualisé classique est prévu pour des sites PHP et ne permet ni d'exécuter une application
Java ni d'utiliser Docker. Le VPS, lui, fournit un système Linux complet sur lequel la solution s'exécute
exactement comme en développement et en intégration continue.

**Préparation du VPS :**

```bash
# 1. Connexion au serveur et mise à jour du système
ssh <utilisateur>@<adresse-du-vps>
sudo apt update && sudo apt upgrade -y

# 2. Installation de Docker et de Docker Compose
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER

# 3. Pare-feu : seuls SSH, HTTP et HTTPS sont ouverts
sudo ufw allow OpenSSH && sudo ufw allow 80 && sudo ufw allow 443 && sudo ufw enable

# 4. Récupération du projet et configuration
git clone https://github.com/niassecheikhou/Jaakha.git jaankha && cd jaankha
cp .env.example .env    # mots de passe forts, serveur SMTP, APP_FRONTEND_URL=https://<domaine>
docker compose up --build -d --wait
```

**Exposition sécurisée :** un reverse proxy (Nginx ou Caddy) placé devant le conteneur web termine le HTTPS
avec un certificat **Let's Encrypt** gratuit et redirige le trafic HTTP vers HTTPS. Les ports 8080 (API) et
5432 (base) ne sont pas exposés publiquement : seul le conteneur web est joignable, et il relaie lui-même les
appels vers l'API.

**Mises à jour :** à chaque nouvelle version validée par le pipeline, le serveur récupère les images publiées
et redémarre les conteneurs, sans perte de données grâce aux volumes persistants :

```bash
cd jaankha && git pull && docker compose up --build -d --wait
```

Cette étape peut être automatisée en ajoutant au workflow `deploy.yml` une connexion SSH vers le VPS ; le
workflow actuel prévoit un déclenchement par *deploy hook*, mécanisme propre aux plateformes de type Render
ou Railway, qu'il faudra remplacer par cette étape SSH pour le VPS LWS.

[Espace réservé : capture de l'espace client LWS et de l'application en ligne.]

[Espace réservé : capture d'écran]

*Figure 19 : Exécution du pipeline sur GitHub Actions*

**Points de vigilance pour la production :**

- servir le site en HTTPS (certificat fourni par l'hébergeur ou par un proxy tel que Caddy ou Traefik) ;
- restreindre `CORS_ALLOWED_ORIGIN_PATTERNS` au seul domaine de production ;
- utiliser des mots de passe forts et uniques pour la base et le compte administrateur ;
- monter les volumes de fichiers sur un stockage persistant ; le dossier des pièces d'identité ne doit
  jamais être exposé publiquement.

## 8.4 Procédures de sauvegarde

Les données à protéger sont la base PostgreSQL et les deux volumes de fichiers. La procédure suivante est
recommandée.

*Tableau 20 : Procédure de sauvegarde et de restauration*

| Élément | Sauvegarde | Restauration | Fréquence conseillée |
|---|---|---|---|
| Base de données | `docker exec jaankha-postgres pg_dump -U <utilisateur> -d <base> > sauvegarde.sql` | `docker exec -i jaankha-postgres psql -U <utilisateur> -d <base> < sauvegarde.sql` | Quotidienne |
| Photos publiques | `docker run --rm -v jaankha_uploads:/data -v "$PWD":/backup alpine tar czf /backup/uploads.tgz -C /data .` | Commande inverse (`tar xzf`) dans le volume | Quotidienne |
| Pièces d'identité | Même commande sur le volume `jaankha_private_uploads`, archive **chiffrée** | Commande inverse | Quotidienne |

Les sauvegardes doivent être conservées hors du serveur et testées régulièrement par une restauration
d'essai. Les archives contenant des pièces d'identité sont des données personnelles : elles doivent être
chiffrées et leur accès restreint.

Sur le VPS, ces commandes sont regroupées dans un script exécuté chaque nuit par une tâche planifiée
`cron`, par exemple à 2 h du matin, avec une conservation de 30 jours :

```bash
# crontab -e
0 2 * * * /home/<utilisateur>/jaankha/sauvegarde.sh >> /var/log/jaankha-sauvegarde.log 2>&1
```

Les archives produites sont ensuite copiées hors du VPS (espace de stockage distant ou poste
d'administration), afin de rester disponibles en cas de perte du serveur.

## Conclusion du chapitre

Grâce à la conteneurisation et à l'automatisation du pipeline, la solution s'installe avec une seule
commande, et chaque mise en production est précédée d'une vérification complète. Le VPS LWS accueille la
solution dans les mêmes conditions qu'en développement, protégée par HTTPS et par des sauvegardes
quotidiennes.
