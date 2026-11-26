# Conclusion générale

## Résumé du projet et de son déroulement

Ce mémoire est parti d'un constat : la mise en relation entre employeurs et prestataires de services
domestiques repose sur des circuits informels qui n'offrent aucune garantie de confiance ni de sécurité.
Nous avons cherché à répondre à la question suivante : **comment une plateforme numérique peut-elle
instaurer cette confiance ?**

La réponse apportée, JAANKHA, combine plusieurs mécanismes : un dossier vérifié par un administrateur avant
toute visibilité publique (photo, pièce d'identité recto et verso, coordonnées d'un tuteur pour les
prestataires), un stockage privé des documents sensibles, un cycle de vie des réservations imposé par le
serveur, des avis réservés aux missions réellement terminées et un contrôle des rôles à chaque requête.

Le projet a suivi l'ensemble du cycle de développement : analyse des besoins (chapitre 2), pilotage itératif
(chapitre 3), conception UML et modélisation des données (chapitre 4), estimation des coûts (chapitre 5),
réalisation d'une API Spring Boot et d'une interface React (chapitre 6), tests automatisés et audit de
sécurité (chapitre 7), puis conteneurisation et chaîne d'intégration et de livraison continues
(chapitre 8). Chaque évolution a été livrée sur une branche dédiée et fusionnée uniquement après le passage
complet du pipeline de tests.

## Apports techniques

Ce projet, mené individuellement, m'a permis d'acquérir ou de consolider les compétences suivantes :

- conception d'une API REST sécurisée avec Spring Boot et organisation du code en couches ;
- développement d'une application monopage en React et TypeScript strict ;
- modélisation UML et modélisation des données ;
- sécurisation d'une application (authentification, contrôle des rôles, protection des données
  personnelles, audit de sécurité) ;
- tests unitaires, tests d'intégration et tests de fumée ;
- conteneurisation avec Docker et mise en place d'un pipeline CI/CD avec GitHub Actions ;
- travail collaboratif avec Git : branches, demandes de fusion, protection de branche.

## Atouts du projet

Au-delà des compétences techniques, JAANKHA constitue un projet complet, de l'analyse au déploiement, qui
répond à un problème réel et peut être présenté en entretien professionnel. Il m'a confronté à des incidents
concrets — une fusion défectueuse, des droits d'accès aux volumes Docker, des failles de sécurité — et à leur
résolution méthodique : reproduire, comprendre la cause, corriger, puis ajouter un test qui empêche le
problème de revenir. Enfin, le modèle économique prévu (chapitre 5) ouvre la voie à une exploitation réelle
de la plateforme.

## Limites

Plusieurs limites, identifiées honnêtement au cours de l'audit, restent à lever :

- la réservation ne comporte pas encore de date, d'adresse ni de durée choisies par l'employeur ;
- les disponibilités des prestataires ne sont pas enregistrées ;
- l'espace prestataire ne permet pas encore d'accepter, démarrer ou terminer une mission depuis l'interface ;
- un employeur dont le dossier n'est pas validé peut réserver ;
- la connexion n'est pas protégée contre les tentatives répétées, et la déconnexion n'invalide pas la session
  côté serveur ;
- les clés étrangères ne sont pas déclarées comme contraintes en base ;
- l'application mobile n'en est qu'au stade de squelette.

## Perspectives d'évolution et travaux futurs

**À court terme :**

- formulaire de réservation complet (date, heure, durée, adresse, consignes) et gestion des disponibilités ;
- actions du prestataire dans son espace (accepter, démarrer, terminer) et notifications associées ;
- réservation réservée aux employeurs dont le dossier est validé ;
- limitation des tentatives de connexion et des envois d'emails, déconnexion côté serveur ;
- contraintes de clés étrangères et migrations de schéma versionnées (Flyway est déjà présent dans le
  projet).

**À moyen terme :**

- application mobile Expo connectée à la même API, pour les prestataires qui utilisent surtout un téléphone ;
- paiement en ligne de l'abonnement via les solutions de paiement mobile utilisées au Sénégal, et gestion de
  la période gratuite de 6 mois, prérequis au modèle économique prévu (chapitre 5) ;
- contrat de prestation généré automatiquement à partir de la réservation ;
- vérification automatisée des pièces d'identité ;
- mise en production sur le VPS LWS, avec HTTPS, supervision et sauvegardes automatisées.

JAANKHA constitue ainsi une base solide et sécurisée, sur laquelle ces évolutions peuvent être construites
sans remettre en cause l'architecture.
