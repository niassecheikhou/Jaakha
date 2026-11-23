# Pages liminaires

---

## Page de garde

> Mise en page : une page seule, centrée, avec les logos en haut.

[Espace réservé : logo de l'Ecole 221] [Logo du projet JAANKHA]

**ECOLE 221**

Mémoire de fin de cycle pour l'obtention de la **Licence en Génie Logiciel**

---

# JAANKHA

### Conception et réalisation d'une plateforme web de mise en relation sécurisée entre employeurs et prestataires de services domestiques

---

**Présenté par :** Cheikhou NIASS

**Sous l'encadrement de :** M. WANE

**Année académique :** 2025-2026

---

## Remerciements

> Maximum une page.

[Espace réservé : remerciements, à rédiger par l'auteur.]

---

## Dédicace

> Maximum une page, texte personnel.

[Espace réservé : dédicace, à rédiger par l'auteur.]

---

## Préambule

> Maximum une page.

[Espace réservé : préambule — présentation de l'Ecole 221 et du cadre du projet de fin de cycle, à rédiger par l'auteur.]

---

## Liste des tableaux

> Les numéros de page seront générés automatiquement à la mise en page.

| N° | Titre | Chapitre |
|---|---|---|
| Tableau 1 | Fiche d'identité du projet JAANKHA | 1 |
| Tableau 2 | Acteurs du système et leurs responsabilités | 2 |
| Tableau 3 | Besoins fonctionnels | 2 |
| Tableau 4 | Besoins non fonctionnels | 2 |
| Tableau 5 | Comparaison des méthodes de gestion de projet | 3 |
| Tableau 6 | Jalons du projet issus de l'historique Git | 3 |
| Tableau 7 | Outils de gestion et de collaboration | 3 |
| Tableau 8 | Dictionnaire du modèle logique de données | 4 |
| Tableau 9 | Points d'accès de l'API REST | 4 |
| Tableau 10 | Choix technologiques et justifications | 4 |
| Tableau 11 | Ressources matérielles et logicielles | 5 |
| Tableau 12 | Ressources humaines | 5 |
| Tableau 13 | Budget prévisionnel global | 5 |
| Tableau 14 | Environnement de développement | 6 |
| Tableau 15 | Mesures de sécurité implémentées | 6 |
| Tableau 16 | Tests automatisés du backend | 7 |
| Tableau 17 | Vérifications des tests de fumée | 7 |
| Tableau 18 | Anomalies détectées et corrections apportées | 7 |
| Tableau 19 | Variables d'environnement de la solution | 8 |
| Tableau 20 | Procédure de sauvegarde et de restauration | 8 |

---

## Liste des figures et illustrations

| N° | Titre | Chapitre |
|---|---|---|
| Figure 1 | Chronologie du projet | 3 |
| Figure 2 | Diagramme de contexte | 4 |
| Figure 3 | Diagramme de cas d'utilisation | 4 |
| Figure 4 | Diagramme de classes | 4 |
| Figure 5 | Diagramme de séquence — inscription et connexion | 4 |
| Figure 6 | Diagramme de séquence — dépôt de la pièce d'identité (recto et verso) | 4 |
| Figure 7 | Diagramme de séquence — réservation et changement de statut | 4 |
| Figure 8 | Diagramme d'états d'une réservation | 4 |
| Figure 9 | Modèle conceptuel de données (MCD) | 4 |
| Figure 10 | Architecture logicielle en couches | 4 |
| Figure 11 | Page d'accueil | 6 |
| Figure 12 | Formulaire d'inscription | 6 |
| Figure 13 | Dossier prestataire (photo de profil et pièce d'identité) | 6 |
| Figure 14 | Recherche de prestataires | 6 |
| Figure 15 | Tableau de bord administrateur avec filtre par période | 6 |
| Figure 16 | Gestion des réservations et menu d'actions | 6 |
| Figure 17 | Pipeline d'intégration continue | 7 |
| Figure 18 | Architecture de déploiement conteneurisée | 8 |
| Figure 19 | Exécution du pipeline sur GitHub Actions | 8 |

---

## Sigles et abréviations

| Sigle | Signification |
|---|---|
| API | Application Programming Interface (interface de programmation) |
| BCrypt | Algorithme de hachage de mots de passe à coût adaptatif |
| CD | Continuous Delivery / Deployment (livraison ou déploiement continu) |
| CI | Continuous Integration (intégration continue) |
| CNI | Carte Nationale d'Identité |
| CORS | Cross-Origin Resource Sharing |
| CSV | Comma-Separated Values |
| DTO | Data Transfer Object (objet de transfert de données) |
| FCFA | Franc de la Communauté Financière Africaine |
| GHCR | GitHub Container Registry |
| HTTP(S) | HyperText Transfer Protocol (Secure) |
| JDBC | Java Database Connectivity |
| JPA | Jakarta Persistence API |
| JSON | JavaScript Object Notation |
| MCD | Modèle Conceptuel de Données |
| MLD | Modèle Logique de Données |
| MVC | Modèle-Vue-Contrôleur |
| PR | Pull Request (demande de fusion) |
| REST | Representational State Transfer |
| SMTP | Simple Mail Transfer Protocol |
| SPA | Single Page Application (application monopage) |
| SQL | Structured Query Language |
| UML | Unified Modeling Language |
| URL | Uniform Resource Locator |
| UUID | Universally Unique Identifier |

---

## Résumé

Au Sénégal, le recrutement de personnel domestique (ménage, garde d'enfants, cuisine) repose
essentiellement sur le bouche-à-oreille et des intermédiaires informels. Ce mode de mise en relation
laisse les deux parties sans garanties : l'employeur ne peut pas vérifier l'identité ni le sérieux de la
personne qu'il fait entrer chez lui, et le prestataire ne dispose d'aucun cadre clair ni d'historique pour
valoriser son travail. Ce mémoire répond à la question suivante : **comment une plateforme numérique
peut-elle instaurer la confiance et la sécurité dans la mise en relation entre employeurs et prestataires de
services domestiques ?**

Pour y répondre, nous avons conçu et réalisé **JAANKHA**, une plateforme web composée d'une API REST
développée avec Spring Boot (Java 21), d'une interface React en TypeScript et d'une base de données
PostgreSQL, le tout conteneurisé avec Docker. La démarche, pilotée selon une méthode agile, a suivi les phases
du cycle de développement : analyse des besoins, conception UML, réalisation, tests et déploiement, avec des
livraisons par branches de fonctionnalités et un pipeline d'intégration continue qui bloque toute fusion
tant que les tests ne sont pas au vert.

La confiance est construite par des mécanismes concrets : vérification des dossiers par un administrateur
(photo, pièce d'identité recto et verso, informations du tuteur pour les prestataires), stockage privé des
pièces d'identité, cycle de vie contrôlé des réservations, avis réservés aux missions terminées, et contrôle
des rôles à chaque requête. La solution est validée par 53 tests automatisés et des tests de fumée exécutés
sur la pile complète à chaque demande de fusion.

**Mots clés :** mise en relation, services domestiques, confiance numérique, vérification d'identité,
application web.

---

## Abstract

In Senegal, hiring domestic workers (cleaning, childcare, cooking) relies mostly on word of mouth and
informal intermediaries. This leaves both parties without guarantees: employers cannot verify the identity
or reliability of the person entering their home, and workers have no clear framework or track record to
showcase their work. This thesis addresses the following question: **how can a digital platform build trust
and security in matching employers with domestic service providers?**

To answer it, we designed and built **JAANKHA**, a web platform made of a REST API developed with Spring
Boot (Java 21), a React user interface written in TypeScript and a PostgreSQL database, all containerised
with Docker. The work, managed with an agile method, followed the phases of the
development life cycle: requirements analysis, UML design, implementation, testing and deployment, with
deliveries through feature branches and a continuous integration pipeline that blocks any merge until the
tests pass.

Trust is built through concrete mechanisms: profile verification by an administrator (photo, front and back
of the national identity card, guardian details for providers), private storage of identity documents, a
controlled booking life cycle, reviews restricted to completed missions, and role checks on every request.
The solution is validated by 53 automated tests and by smoke tests run against the full stack on every pull
request.

**Keywords:** matchmaking platform, domestic services, digital trust, identity verification, web application.
