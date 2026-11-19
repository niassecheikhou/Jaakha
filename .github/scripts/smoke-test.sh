#!/usr/bin/env bash
# Test de fumee de la stack docker compose (postgres + backend + web).
# Verifie le parcours reel navigateur -> Nginx -> API Spring -> PostgreSQL.
# Usage : smoke-test.sh [URL_WEB] [URL_BACKEND]
set -euo pipefail

WEB_URL="${1:-http://localhost:8081}"
BACKEND_URL="${2:-http://localhost:8080}"
FAILURES=0

check() {
  local label="$1" expected="$2" actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo "OK   $label ($actual)"
  else
    echo "FAIL $label : attendu $expected, obtenu $actual"
    FAILURES=$((FAILURES + 1))
  fi
}

status() {
  curl -s -o /dev/null -w '%{http_code}' "$@"
}

echo "== Backend"
check "sante backend /actuator/health" 200 "$(status "$BACKEND_URL/actuator/health")"

echo "== Web (Nginx)"
check "sonde /healthz" 200 "$(status "$WEB_URL/healthz")"
check "page d'accueil" 200 "$(status "$WEB_URL/")"
check "route React /providers (fallback SPA)" 200 "$(status "$WEB_URL/providers")"
if ! curl -s "$WEB_URL/" | grep -q '<div id="root">'; then
  echo "FAIL index.html ne contient pas l'application React"
  FAILURES=$((FAILURES + 1))
fi

echo "== API via le proxy Nginx"
check "recherche publique /api/providers" 200 "$(status "$WEB_URL/api/providers")"
check "route protegee sans token -> 401" 401 "$(status "$WEB_URL/api/account/me")"
check "route admin sans token -> 401" 401 "$(status "$WEB_URL/api/admin/users")"

# Parcours complet : inscription (ecriture en base) puis lecture du profil avec le token obtenu.
SUFFIX="$(date +%s)$RANDOM"
PHONE="+22177$(printf '%07d' $((10#${SUFFIX: -7})))"
REGISTER_BODY=$(cat <<JSON
{"fullName":"Test Fumee CI","email":"ci-$SUFFIX@jaankha.test","phone":"$PHONE","password":"Ci-Test1!","role":"EMPLOYEUR"}
JSON
)
REGISTER_RESPONSE=$(curl -s -w '\n%{http_code}' -X POST "$WEB_URL/api/auth/register" \
  -H 'Content-Type: application/json' -d "$REGISTER_BODY")
check "inscription /api/auth/register" 201 "$(tail -n1 <<<"$REGISTER_RESPONSE")"
TOKEN=$(head -n -1 <<<"$REGISTER_RESPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [[ -n "$TOKEN" ]]; then
  check "profil avec token /api/account/me" 200 \
    "$(status -H "Authorization: Bearer $TOKEN" "$WEB_URL/api/account/me")"
  check "employeur refuse sur /api/admin/users -> 403" 403 \
    "$(status -H "Authorization: Bearer $TOKEN" "$WEB_URL/api/admin/users")"
else
  echo "FAIL aucun token renvoye par l'inscription"
  FAILURES=$((FAILURES + 1))
fi

echo "== Securite"
check "dossier prive non expose /uploads/identity-documents/" 404 \
  "$(status "$WEB_URL/uploads/identity-documents/x.jpg")"
if curl -sI "$WEB_URL/" | grep -qi '^x-content-type-options: nosniff'; then
  echo "OK   en-tete X-Content-Type-Options"
else
  echo "FAIL en-tete X-Content-Type-Options absent"
  FAILURES=$((FAILURES + 1))
fi

echo
if [[ "$FAILURES" -gt 0 ]]; then
  echo "$FAILURES verification(s) en echec."
  exit 1
fi
echo "Toutes les verifications sont passees."
