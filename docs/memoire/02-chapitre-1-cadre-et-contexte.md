# Partie 1 — Cadres organisationnel et méthodologique

# Chapitre 1 : Cadre du projet et contexte d'étude

Ce chapitre situe le projet JAANKHA dans son environnement : le cadre académique dans lequel il a été
mené, puis le contexte métier des services domestiques et les enjeux qu'il soulève.

## Section 1 : Cadre du projet

### 1.1 Cadre académique

JAANKHA est le projet de fin de cycle de la Licence en Génie Logiciel. Il a été conduit au sein de
l'Ecole 221, au titre de l'année académique 2025-2026, sous l'encadrement de M. Wane. Le projet n'a pas été
réalisé pour le compte d'une entreprise : il répond à un besoin identifié par l'auteur dans son
environnement quotidien.

### 1.2 Mission du projet

La mission que nous nous sommes fixée est de **concevoir, développer, tester et déployer une plateforme web
qui sécurise la mise en relation entre employeurs et prestataires de services domestiques**. Concrètement,
cette mission se décline en quatre engagements :

- offrir aux employeurs un moyen de trouver des prestataires dont le dossier a été vérifié ;
- offrir aux prestataires une vitrine professionnelle (profil, compétences, tarif, avis) ;
- donner à l'administration de la plateforme les outils pour contrôler les dossiers et suivre l'activité ;
- protéger les données personnelles manipulées, en particulier les pièces d'identité.

Le projet a été réalisé individuellement par **Cheikhou Niass**, qui en a assuré l'ensemble des rôles :
analyse, conception, développement du backend et du frontend, tests, déploiement et rédaction.

### 1.3 Fiche d'identité du projet

*Tableau 1 : Fiche d'identité du projet JAANKHA*

| Élément | Description |
|---|---|
| Nom du projet | JAANKHA |
| Type de solution | Plateforme web de mise en relation (application monopage et API REST) |
| Domaine | Services à la personne — services domestiques |
| Zone ciblée | Dakar et sa région (numéros de téléphone sénégalais imposés : +221 70, 71, 76, 77, 78) |
| Auteur | Cheikhou Niass, sous l'encadrement de M. Wane (Ecole 221, 2025-2026) |
| Utilisateurs | Employeurs (familles), prestataires de services domestiques, administrateurs de la plateforme |
| Technologies principales | Spring Boot 4.1 (Java 21), React 19 avec TypeScript, PostgreSQL 17, Docker |
| Période de réalisation | Du 19 août 2026 (premier commit) au 20 novembre 2026 (dernière fusion sur la branche principale) |
| Dépôt de code | GitHub, avec intégration continue sur chaque demande de fusion |

## Section 2 : Contexte d'étude

### 2.1 Le domaine des services domestiques

Les services domestiques regroupent les tâches réalisées au domicile d'un particulier : entretien du
logement, garde d'enfants, préparation des repas, repassage. La plateforme JAANKHA les présente sous forme
de compétences libres saisies par les prestataires (par exemple « ménage », « cuisine », « garde enfant »),
ce qui permet de couvrir la diversité des profils sans figer une liste fermée.

Ce secteur présente trois particularités qui orientent la conception de la solution :

1. **La relation se déroule au domicile de l'employeur.** L'employeur confie l'accès à son logement et parfois
   ses enfants : la vérification de l'identité du prestataire est donc primordiale.
2. **Les prestataires sont souvent jeunes et parfois mineurs ou primo-employés.** C'est pourquoi la
   plateforme demande, pour chaque prestataire, les coordonnées d'un **tuteur** (nom complet et téléphone),
   qui servent de personne de référence.
3. **La rémunération est généralement mensuelle.** Le tarif des prestataires est donc exprimé en francs
   CFA par mois.

### 2.2 Situation actuelle et limites

Aujourd'hui, la mise en relation repose principalement sur des circuits informels : recommandation d'un
proche ou d'un voisin, intermédiaire de quartier, annonce diffusée de bouche-à-oreille. La famille recrute
souvent sur la seule base de la confiance accordée à la personne qui recommande, sans document vérifié ni
trace écrite de l'engagement.

Les limites de ce fonctionnement peuvent être résumées ainsi :

- **Absence de vérification :** l'identité du prestataire n'est pas contrôlée de façon systématique.
- **Absence de traçabilité :** les engagements (date, mission, statut) ne sont consignés nulle part.
- **Absence de réputation partagée :** l'expérience d'un employeur avec un prestataire n'est pas transmise
  aux suivants, sauf par le bouche-à-oreille.
- **Asymétrie d'information :** le prestataire, lui non plus, ne dispose d'aucune information fiable sur
  l'employeur.

### 2.3 Solutions existantes

Trois types de solutions coexistent :

| Solution | Points forts | Limites au regard de la confiance |
|---|---|---|
| Groupes et pages sur les réseaux sociaux et messageries | Gratuits, très utilisés, diffusion rapide | Aucune vérification d'identité, aucun suivi des engagements, avis non vérifiables |
| Agences et bureaux de placement | Accompagnement humain, présélection | Coût élevé pour les familles, couverture géographique limitée, peu de traçabilité numérique |
| Plateformes généralistes de services à la personne | Interface structurée, profils | Rarement adaptées au contexte local (téléphone sénégalais, tuteur, rémunération mensuelle en FCFA) |

JAANKHA se positionne entre ces solutions : une plateforme numérique adaptée au contexte local, qui place
la vérification des dossiers et la protection des données au centre du service.

### 2.4 Enjeux du projet

**Pour les employeurs**, l'enjeu est de recruter en confiance : n'accéder qu'à des prestataires dont le
dossier a été vérifié, consulter leurs compétences, leur tarif et les avis laissés par d'autres employeurs,
et suivre l'état de chaque réservation.

**Pour les prestataires**, l'enjeu est de valoriser leur travail : disposer d'un profil public, être visibles
d'un plus grand nombre d'employeurs une fois leur dossier validé, et bâtir une réputation à partir des avis
reçus sur des missions réellement terminées.

**Pour l'administration de la plateforme**, l'enjeu est de garantir la qualité du service : contrôler chaque
dossier avant sa publication, superviser les réservations et disposer d'indicateurs de suivi de l'activité.

**Pour le domaine des services domestiques**, le projet propose une première réponse numérique à un secteur
peu outillé, en plaçant la confiance et la protection des données au centre de la conception.

## Conclusion du chapitre

Ce chapitre a présenté le cadre académique du projet, la mission du projet et le contexte des services
domestiques. Il en ressort que le besoin principal n'est pas seulement de mettre en relation, mais de mettre
en relation **en confiance**. Le chapitre suivant traduit ce besoin en spécifications fonctionnelles et non
fonctionnelles.
