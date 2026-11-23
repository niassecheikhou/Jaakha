# Annexes

## Liste des annexes

| N° | Titre |
|---|---|
| Annexe 1 | Captures d'écran de l'application |
| Annexe 2 | Extrait de la configuration Docker Compose |
| Annexe 3 | Pipeline d'intégration continue (`ci.yml`) |
| Annexe 4 | Configuration du serveur web Nginx |
| Annexe 5 | Modèle de fichier d'import CSV des comptes |

---

## Annexe 1 : Captures d'écran de l'application

[Espace réservé : captures complémentaires (connexion, mot de passe oublié, détail d'un prestataire, espace employeur, notifications, fiche d'un compte avec la CNI, import CSV, messages de contact, affichage mobile)]

---

## Annexe 2 : Extrait de la configuration Docker Compose

Extrait du fichier `docker-compose.yml` (service web) :

```yaml
  web:
    build:
      context: ./web
      args:
        # "/api" : le navigateur appelle Nginx, qui relaie vers le backend (meme origine).
        VITE_API_BASE_URL: ${WEB_API_BASE_URL:-/api}
    container_name: jaankha-web
    depends_on:
      backend:
        condition: service_healthy
    environment:
      BACKEND_URL: http://backend:8080
    ports:
      - "${WEB_PORT:-8081}:8080"
    restart: unless-stopped
```

---

## Annexe 3 : Pipeline d'intégration continue

Extrait du fichier `.github/workflows/ci.yml` (job final, contrôle obligatoire avant fusion) :

```yaml
  ci-ok:
    name: CI OK
    runs-on: ubuntu-latest
    needs: [backend, web, docker]
    if: always()
    steps:
      - name: Verifier que tous les jobs ont reussi
        run: |
          if [[ "${{ needs.backend.result }}" != "success" \
             || "${{ needs.web.result }}" != "success" \
             || "${{ needs.docker.result }}" != "success" ]]; then
            echo "::error::Le pipeline n'est pas entierement vert : fusion interdite."
            exit 1
          fi
```

---

## Annexe 4 : Configuration du serveur web Nginx

Extrait du fichier `web/nginx.conf.template` :

```nginx
    # API et photos publiques relayees vers le backend (meme origine : pas de CORS).
    # Les pieces d'identite ne passent jamais par /uploads : elles sont servies par des routes /api protegees.
    location /api/ {
        proxy_pass ${BACKEND_URL};
        proxy_set_header Host $http_host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Application monopage : toute route inconnue renvoie index.html (React Router).
    location / {
        try_files $uri $uri/ /index.html;
    }
```

---

## Annexe 5 : Modèle de fichier d'import CSV des comptes

Le fichier modèle est téléchargeable depuis l'espace administrateur (`web/public/modele-import-comptes.csv`).
Colonnes attendues : nom complet, email, téléphone, mot de passe par défaut, rôle (`employeur` ou
`prestataire`). Chaque ligne est validée avant l'envoi (format de l'email, téléphone sénégalais, complexité du
mot de passe, rôle) et les erreurs sont signalées ligne par ligne. Les comptes créés reçoivent un email
contenant un lien pour choisir leur mot de passe.

[Espace réservé : capture du fichier modèle ouvert dans un tableur.]
