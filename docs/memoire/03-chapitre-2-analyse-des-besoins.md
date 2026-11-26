# Chapitre 2 : Analyse du besoin et spécifications fonctionnelles et non fonctionnelles

*(Phase 1 du cycle de développement : analyse)*

Ce chapitre identifie le problème à résoudre, analyse les besoins de chaque type d'utilisateur, puis en
déduit les objectifs fonctionnels et non fonctionnels de la plateforme.

## 2.1 Identification du problème

Le problème peut être formulé ainsi : **les employeurs et les prestataires de services domestiques n'ont
pas de moyen fiable de se trouver, de se vérifier mutuellement et de suivre leurs engagements.** Ce problème
se décompose en quatre manques :

| Manque | Conséquence | Réponse attendue de la plateforme |
|---|---|---|
| Vérification d'identité | Risque de recruter une personne inconnue | Dossier avec photo et pièce d'identité recto/verso, validé par un administrateur |
| Encadrement des missions | Engagements flous, litiges | Réservations avec un cycle de vie contrôlé et des notifications |
| Réputation | Pas de moyen d'évaluer un prestataire | Avis et notes réservés aux missions terminées |
| Protection des données | Pièces d'identité exposées | Stockage privé et accès restreint au titulaire et à l'administration |

## 2.2 Analyse des besoins des utilisateurs

### 2.2.1 Les acteurs

*Tableau 2 : Acteurs du système et leurs responsabilités*

| Acteur | Description | Responsabilités principales |
|---|---|---|
| Visiteur | Personne non connectée | Consulter les pages publiques, rechercher des prestataires validés, envoyer un message de contact, créer un compte |
| Employeur | Famille ou particulier qui recrute | Compléter son dossier, réserver un prestataire, suivre et annuler ses réservations, noter une mission terminée |
| Prestataire | Personne proposant des services domestiques | Compléter son dossier (compétences, tarif, tuteur, pièce d'identité), consulter les demandes reçues |
| Administrateur | Gestionnaire de la plateforme | Créer et valider les comptes, valider les profils prestataires, piloter les réservations, consulter les messages et les indicateurs |
| Service de messagerie (acteur externe) | Serveur SMTP | Envoyer les emails de bienvenue, de réinitialisation de mot de passe et d'accusé de réception |

Les rôles sont limités à trois dans le système : `EMPLOYEUR`, `PRESTATAIRE` et `ADMIN`. Le visiteur
n'a pas de rôle : il correspond à un utilisateur non authentifié.

### 2.2.2 Besoins par acteur

**L'employeur** a besoin de trouver rapidement un prestataire adapté (par zone, compétence et tarif
maximal), d'avoir l'assurance que ce prestataire a été vérifié, de suivre ses demandes et de pouvoir
laisser un avis une fois la mission terminée.

**Le prestataire** a besoin de présenter son profil (photo, présentation, compétences, expérience, tarif
mensuel), de prouver son identité et d'être informé des demandes qui lui sont adressées.

**L'administrateur** a besoin d'une vue d'ensemble de l'activité, d'outils pour contrôler les dossiers
(consulter la photo et la pièce d'identité recto et verso), de valider ou d'invalider des comptes, de faire
avancer les réservations et de traiter les messages reçus.

## 2.3 Objectifs fonctionnels

*Tableau 3 : Besoins fonctionnels*

| Réf. | Module | Fonctionnalité | Acteur(s) |
|---|---|---|---|
| BF01 | Authentification | Créer un compte employeur ou prestataire | Visiteur |
| BF02 | Authentification | Se connecter par email ou numéro de téléphone | Tous |
| BF03 | Authentification | Réinitialiser son mot de passe par un lien envoyé par email | Tous |
| BF04 | Dossier | Déposer une photo de profil | Tous les utilisateurs connectés |
| BF05 | Dossier | Déposer la pièce d'identité recto et verso, ensemble ou séparément | Employeur, prestataire |
| BF06 | Dossier | Renseigner la zone ; pour le prestataire : présentation, tarif mensuel, expérience, compétences et tuteur | Employeur, prestataire |
| BF07 | Recherche | Rechercher des prestataires validés par zone, compétence et tarif maximal | Visiteur, employeur |
| BF08 | Recherche | Consulter le détail d'un prestataire et ses avis | Visiteur, employeur |
| BF09 | Réservation | Réserver un prestataire validé | Employeur |
| BF10 | Réservation | Faire avancer une réservation (accepter, démarrer, terminer) ou l'annuler, selon son rôle | Employeur, prestataire, administrateur |
| BF11 | Avis | Noter (1 à 5) et commenter une mission terminée, une seule fois | Employeur |
| BF12 | Notifications | Être notifié des nouvelles demandes et des changements de statut | Tous les utilisateurs connectés |
| BF13 | Administration | Créer des comptes un par un ou par import d'un fichier CSV | Administrateur |
| BF14 | Administration | Valider ou invalider un compte, l'archiver ou le restaurer | Administrateur |
| BF15 | Administration | Valider ou rejeter un profil prestataire | Administrateur |
| BF16 | Administration | Consulter les indicateurs, filtrés par mois, trimestre, semestre ou année | Administrateur |
| BF17 | Contact | Envoyer un message de contact, que l'administrateur peut consulter et exporter en CSV ou Excel | Visiteur, administrateur |

## 2.4 Objectifs non fonctionnels

*Tableau 4 : Besoins non fonctionnels*

| Réf. | Catégorie | Exigence |
|---|---|---|
| BNF01 | Sécurité | Les mots de passe sont hachés (BCrypt) et doivent contenir au moins 6 caractères, dont une majuscule, une minuscule, un chiffre et un caractère spécial |
| BNF02 | Sécurité | Chaque route protégée vérifie l'authentification et le rôle de l'utilisateur |
| BNF03 | Sécurité / confidentialité | Les pièces d'identité sont stockées hors du dossier public et ne sont lisibles que par leur titulaire et l'administration |
| BNF04 | Sécurité | Les entrées sont validées côté serveur (formats d'email et de téléphone sénégalais, tailles, types de fichiers) |
| BNF05 | Fiabilité | Le cycle de vie d'une réservation est garanti par le serveur : aucun statut ne peut être sauté ni rouvert |
| BNF06 | Fiabilité | Chaque modification est vérifiée par des tests automatisés avant d'être fusionnée |
| BNF07 | Ergonomie | Interface en français, utilisable sur ordinateur et sur mobile, avec des messages d'erreur explicites |
| BNF08 | Performance | Les fichiers déposés sont limités à 5 Mo et aux formats JPG, PNG et WEBP |
| BNF09 | Portabilité | La solution complète se lance avec une seule commande (`docker compose up --build`) |
| BNF10 | Maintenabilité | Le code est organisé en couches (présentation, application, infrastructure, domaine) et typé (TypeScript strict côté web) |

## 2.5 Règles de gestion

Les règles suivantes, issues de l'analyse, sont appliquées par le serveur :

- **RG01** — Un compte nouvellement inscrit n'est pas validé : l'administrateur ne peut le valider que si le
  dossier est complet (nom, téléphone, photo, recto et verso de la pièce d'identité, zone ; et pour un
  prestataire : présentation, tarif strictement positif, compétences, nom et téléphone du tuteur).
- **RG02** — Un prestataire n'apparaît dans la recherche publique que si son profil est validé
  (`APPROVED`) **et** si son compte est actif et non archivé.
- **RG03** — Une réservation suit le cycle *Demandée → Acceptée → En cours → Terminée* ; elle peut être
  annulée tant qu'elle n'est pas terminée. *Terminée* et *Annulée* sont définitives.
- **RG04** — Un employeur ne peut qu'annuler sa réservation, et seulement avant le début de la mission. Un
  prestataire peut accepter, démarrer et terminer une mission, mais pas l'annuler.
- **RG05** — Un avis ne peut être déposé que par l'employeur de la réservation, sur une réservation
  terminée, et une seule fois.
- **RG06** — Un compte administrateur ne peut être ni invalidé ni archivé depuis l'interface.

## Conclusion du chapitre

L'analyse a permis d'identifier quatre acteurs internes, un acteur externe (la messagerie), dix-sept besoins
fonctionnels, dix exigences non fonctionnelles et six règles de gestion. Ces éléments constituent le cahier
des charges à partir duquel la solution est pilotée (chapitre 3) puis conçue (chapitre 4).
