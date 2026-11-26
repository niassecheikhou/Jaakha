# Chapitre 5 : Coût du projet

Ce chapitre estime les ressources financières nécessaires à la conception, au développement et à la mise
en œuvre de JAANKHA, puis présente le modèle économique prévu pour financer son exploitation. Les montants
sont exprimés en francs CFA (FCFA).

## 5.1 Ressources matérielles et logicielles

*Tableau 11 : Ressources matérielles et logicielles*

| Poste | Détail | Coût (FCFA) |
|---|---|---|
| Ordinateur de développement | Matériel personnel déjà disponible, sans achat spécifique | 0 |
| Connexion Internet | Abonnement personnel déjà existant | 0 |
| Java 21, Spring Boot, Maven | Logiciels libres | 0 |
| React, Vite, TypeScript, Node.js | Logiciels libres | 0 |
| PostgreSQL, H2 | Logiciels libres | 0 |
| Docker Desktop | Gratuit pour un usage personnel et éducatif | 0 |
| Visual Studio Code | Gratuit | 0 |
| Git et GitHub (dépôt, Actions, registre d'images) | Offre gratuite | 0 |
| **Sous-total** | | **0** |

L'ensemble de la pile technique repose sur des logiciels libres ou gratuits : la réalisation n'a entraîné
aucun coût de licence.

## 5.2 Ressources humaines

*Tableau 12 : Ressources humaines*

| Profil | Activités | Coût engagé (FCFA) |
|---|---|---|
| Cheikhou Niass, porteur du projet | Analyse, conception, développement backend et frontend, tests, déploiement, documentation | 0 |

Le projet ayant été réalisé individuellement dans un cadre académique, la main-d'œuvre n'a pas donné lieu
à rémunération ; elle n'est donc pas comptée dans les dépenses engagées. Le projet s'étend du 19 août 2026
(premier commit) au 20 novembre 2026, soit environ trois mois de travail.

## 5.3 Frais de mise en production

La mise en ligne nécessite un serveur capable d'exécuter les conteneurs Docker de la solution (base
PostgreSQL, API Java, serveur web Nginx). Le choix s'est porté sur l'offre **VPS M de l'hébergeur LWS** :

| Caractéristique | Offre VPS M (LWS) |
|---|---|
| Processeur | Intel Xeon, 4 vCore à 3,0 GHz |
| Mémoire vive | 8 Go |
| Stockage | 150 Go SSD NVMe |
| Bande passante | Illimitée (débit de 250 Mbit/s) |
| Administration | Accès root (ou infogérance) |
| Prix promotionnel | 4,99 € HT par mois, soit 5,99 € TTC par mois |

Cette configuration couvre largement les besoins de JAANKHA : les trois conteneurs (PostgreSQL, API Java et
Nginx) tiennent dans 8 Go de mémoire, et les 150 Go de stockage accueillent la base de données ainsi que les
photos et pièces d'identité déposées.

Au taux fixe de 1 € = 655,957 FCFA, le coût annuel au prix promotionnel est de 5,99 € × 12 = 71,88 €, soit
environ **47 150 FCFA**. Le budget d'hébergement est arrêté à **60 000 FCFA**, ce qui couvre l'abonnement
annuel ainsi que les frais de change et de paiement par carte.

| Poste | Détail | Coût (FCFA) |
|---|---|---|
| Hébergement | Offre VPS M LWS, 12 mois (environ 47 150 FCFA, frais de change et de paiement inclus) | 60 000 |
| Certificat HTTPS | Let's Encrypt, gratuit | 0 |
| **Sous-total** | | **60 000** |

## 5.4 Budget global

*Tableau 13 : Budget prévisionnel global*

| Poste | Montant (FCFA) |
|---|---|
| Ressources matérielles et logicielles | 0 |
| Ressources humaines | 0 |
| Mise en production (hébergement VPS LWS) | 60 000 |
| Imprévus (10 % des dépenses) | 6 000 |
| **Total** | **66 000** |

Le choix de technologies libres et la conteneurisation réduisent le budget à l'essentiel : le seul poste de
dépense effectif est l'hébergement.

## 5.5 Modèle économique et rentabilité

Pour financer l'exploitation de la plateforme, le modèle économique suivant est prévu :

- **Phase de lancement (6 premiers mois) :** l'inscription et l'utilisation sont **gratuites** pour tous, afin
  d'attirer les premiers employeurs et prestataires et de constituer une base de profils vérifiés.
- **Après 6 mois :** l'accès devient **payant, par un abonnement annuel de 2 000 FCFA par utilisateur**
  (employeurs et prestataires). Les comptes administrateurs ne sont pas concernés.

**Seuil de rentabilité.** Le budget de la première année, 66 000 FCFA, est couvert par :

> 66 000 FCFA ÷ 2 000 FCFA = **33 abonnements annuels**.

Autrement dit, dès que 33 utilisateurs ont souscrit leur abonnement après la période gratuite, le coût de
mise en production est amorti ; chaque abonnement supplémentaire constitue une marge disponible pour la
maintenance et les évolutions de la plateforme.

**Hypothèse de revenus (illustration).** Avec 100 abonnés, les revenus annuels atteignent
100 × 2 000 = 200 000 FCFA, soit environ trois fois le budget de la première année.

**Analyse de sensibilité : le renouvellement de l'hébergement.** Le prix de 4,99 € HT par mois est un tarif
promotionnel (−75 % sur un prix de référence de 19,99 € HT par mois). Si le renouvellement se faisait au tarif
plein, soit 23,99 € TTC par mois, le coût annuel atteindrait 287,88 €, soit environ **188 840 FCFA**, et le
seuil de rentabilité passerait à :

> 188 840 FCFA ÷ 2 000 FCFA ≈ **95 abonnements annuels**.

Ce seuil reste atteignable pour une plateforme de mise en relation à l'échelle de Dakar, mais il doit être
anticipé : constituer une base d'utilisateurs suffisante pendant la première année, ou migrer vers une
offre d'hébergement moins coûteuse au moment du renouvellement, ce que la conteneurisation rend simple.

**Prérequis technique.** La version actuelle de la plateforme ne gère pas encore le paiement en ligne ni la
période d'essai. Leur mise en œuvre (paiement mobile, blocage de l'accès à l'expiration de la période
gratuite, exemption des administrateurs) figure parmi les perspectives d'évolution (conclusion générale).

## Conclusion du chapitre

Avec un budget de 66 000 FCFA, limité à l'hébergement grâce aux logiciels libres, et un seuil de rentabilité
de 33 abonnements annuels la première année (environ 95 si l'hébergement est renouvelé au tarif plein),
JAANKHA présente un coût de lancement très faible au regard de son potentiel.
La partie suivante présente la mise en œuvre de la solution.
