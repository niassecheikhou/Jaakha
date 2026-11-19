#!/bin/sh
# Demarrage du backend : remet les dossiers de stockage a l'utilisateur applicatif, puis lance
# Java SANS les droits root.
# Necessaire car des volumes Docker crees par une ancienne image (qui tournait en root) restent
# proprietes de root : le backend ne pourrait plus y ecrire les photos (erreur a l'upload).
set -eu

APP_USER=jaankha
STORAGE_DIRS="${UPLOAD_DIR:-/app/uploads} ${PRIVATE_UPLOAD_DIR:-/app/private-uploads} /app/data"

if [ "$(id -u)" = "0" ]; then
    for dir in $STORAGE_DIRS; do
        mkdir -p "$dir"
        # On ne parcourt le dossier que s'il contient quelque chose qui n'appartient pas a l'utilisateur.
        if [ -n "$(find "$dir" ! -user "$APP_USER" -print -quit)" ]; then
            chown -R "$APP_USER:$APP_USER" "$dir"
        fi
    done
    exec setpriv --reuid="$APP_USER" --regid="$APP_USER" --init-groups -- "$@"
fi

exec "$@"
