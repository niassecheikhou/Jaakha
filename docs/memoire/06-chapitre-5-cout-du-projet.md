# Chapitre 5 : Coût du projet

Ce chapitre estime les ressources financières nécessaires à la conception, au développement et à la mise
en œuvre de JAANKHA. Les montants sont exprimés en francs CFA (FCFA).

> Méthode : le coût total est la somme des ressources matérielles, logicielles, humaines et des frais
> d'exploitation (hébergement, nom de domaine, messagerie) sur la durée retenue.
> [À COMPLÉTER : tous les montants marqués ci-dessous. Indiquer la source de chaque prix (devis, site du
> fournisseur, grille salariale) et la date de consultation.]

## 5.1 Ressources matérielles et logicielles

*Tableau 11 : Ressources matérielles et logicielles*

| Poste | Détail | Quantité | Coût unitaire (FCFA) | Coût total (FCFA) |
|---|---|---|---|---|
| Ordinateur de développement | [À COMPLÉTER : modèle] | [À COMPLÉTER] | [À COMPLÉTER] | [À COMPLÉTER] |
| Connexion Internet | Abonnement mensuel | [À COMPLÉTER : nombre de mois] | [À COMPLÉTER] | [À COMPLÉTER] |
| Java 21, Spring Boot, Maven | Logiciels libres | — | 0 | 0 |
| React, Vite, TypeScript, Node.js | Logiciels libres | — | 0 | 0 |
| PostgreSQL, H2 | Logiciels libres | — | 0 | 0 |
| Docker Desktop | Gratuit pour un usage personnel, éducatif ou de petite structure | — | 0 | 0 |
| Visual Studio Code | Gratuit | — | 0 | 0 |
| Git et GitHub (dépôt, Actions, registre d'images) | Offre gratuite | — | 0 | 0 |
| Assistant de développement (Claude Code) | [À COMPLÉTER : abonnement utilisé, le cas échéant] | [À COMPLÉTER] | [À COMPLÉTER] | [À COMPLÉTER] |
| **Sous-total** | | | | **[À COMPLÉTER]** |

L'ensemble de la pile technique repose sur des logiciels libres ou gratuits : le coût logiciel de la
réalisation est donc nul, hors éventuels abonnements d'assistance.

## 5.2 Ressources humaines

Le coût humain est estimé en **jours-homme** : nombre de jours de travail multiplié par un taux journalier
de référence pour chaque profil.

*Tableau 12 : Ressources humaines*

| Profil | Activités | Jours-homme | Taux journalier (FCFA) | Coût (FCFA) |
|---|---|---|---|---|
| Analyste / chef de projet | Analyse des besoins, conception, pilotage | [À COMPLÉTER] | [À COMPLÉTER] | [À COMPLÉTER] |
| Développeur backend | API Spring Boot, base de données, sécurité | [À COMPLÉTER] | [À COMPLÉTER] | [À COMPLÉTER] |
| Développeur frontend | Application React | [À COMPLÉTER] | [À COMPLÉTER] | [À COMPLÉTER] |
| Ingénieur DevOps / testeur | Tests, Docker, CI/CD, déploiement | [À COMPLÉTER] | [À COMPLÉTER] | [À COMPLÉTER] |
| **Sous-total** | | | | **[À COMPLÉTER]** |

Repère de durée : le projet s'étend du 19 août 2026 (premier commit) au 20 novembre 2026, soit environ trois
mois. [À COMPLÉTER : nombre de jours effectivement travaillés par profil.]

## 5.3 Frais de mise en production et d'exploitation

| Poste | Détail | Coût mensuel (FCFA) | Coût annuel (FCFA) |
|---|---|---|---|
| Hébergement du backend et de la base | [À COMPLÉTER : Render, Railway ou VPS] | [À COMPLÉTER] | [À COMPLÉTER] |
| Hébergement du frontend | [À COMPLÉTER : Vercel, Netlify ou même serveur que le backend] | [À COMPLÉTER] | [À COMPLÉTER] |
| Stockage persistant des fichiers | Photos et pièces d'identité | [À COMPLÉTER] | [À COMPLÉTER] |
| Nom de domaine | [À COMPLÉTER : ex. jaankha.sn] | — | [À COMPLÉTER] |
| Service d'envoi d'emails (SMTP) | [À COMPLÉTER : fournisseur] | [À COMPLÉTER] | [À COMPLÉTER] |
| Maintenance corrective et évolutive | [À COMPLÉTER : jours par an] | — | [À COMPLÉTER] |
| **Sous-total exploitation (1 an)** | | | **[À COMPLÉTER]** |

## 5.4 Budget global

*Tableau 13 : Budget prévisionnel global*

| Poste | Montant (FCFA) |
|---|---|
| Ressources matérielles et logicielles | [À COMPLÉTER] |
| Ressources humaines | [À COMPLÉTER] |
| Mise en production et exploitation (première année) | [À COMPLÉTER] |
| Imprévus (10 % du total) | [À COMPLÉTER] |
| **Total** | **[À COMPLÉTER]** |

## Analyse

Le principal poste de dépense est la main-d'œuvre : le choix de technologies libres supprime les coûts de
licence, et la conteneurisation permet d'héberger la solution sur une offre d'entrée de gamme dans un
premier temps, puis de la faire évoluer sans changer l'application. [À COMPLÉTER : commentaire du budget
obtenu et, le cas échéant, piste de financement ou de modèle économique (commission, abonnement des
employeurs, mise en avant des profils).]

## Conclusion du chapitre

L'estimation du coût clôt la partie conceptuelle et analytique. La partie suivante présente la mise en
œuvre : la réalisation, les tests et le déploiement de la solution.
