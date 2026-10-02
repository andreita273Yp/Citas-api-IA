#!/usr/bin/env bash
# Revisión de secretos y archivos .env en lo que está staged para el commit (S3 · V8/V9).
# Solo examina las líneas agregadas; nunca imprime su contenido, solo el archivo y la cantidad de coincidencias.
# Las rutas de .githooks/secret-allowlist pueden nombrar el marcador de la demostración S3, pero se revisan
# igual contra todos los demás patrones. Las reglas del propio hook (.githooks/) no se examinan.
set -uo pipefail

hooks="$(git rev-parse --show-toplevel)/.githooks"
marker='FAKE_SECRET_FOR_S3_TEST_ONLY'
status=0

# 1. Ningún archivo .env (salvo .env.example) puede entrar al commit.
envs=$(git diff --cached --name-only --diff-filter=ACMR | grep -E '(^|/)\.env($|\.)' | grep -vE '(^|/)\.env\.example$')
if [ -n "$envs" ]; then
  echo "pre-commit: FAIL — archivo .env en el commit:" >&2
  printf '  %s\n' $envs >&2
  status=1
fi

# 2. Patrones de secretos en las líneas agregadas.
mentions_marker_allowed() {
  local file="$1" pattern
  while IFS= read -r pattern; do
    case "$pattern" in ''|'#'*) continue ;; esac
    # shellcheck disable=SC2053
    [[ "$file" == $pattern ]] && return 0
  done < "$hooks/secret-allowlist"
  return 1
}

all_patterns=$(grep -vE '^(#|$)' "$hooks/secret-patterns")
general_patterns=$(printf '%s\n' "$all_patterns" | grep -vxF "$marker")

while IFS= read -r file; do
  case "$file" in ''|.githooks/*) continue ;; esac
  patterns="$all_patterns"
  mentions_marker_allowed "$file" && patterns="$general_patterns"
  hits=$(git diff --cached -U0 --no-color -- "$file" | grep -E '^\+' | grep -vE '^\+\+\+ ' | grep -ciE -f <(printf '%s\n' "$patterns"))
  if [ "${hits:-0}" -gt 0 ]; then
    echo "pre-commit: FAIL — posible secreto en $file ($hits línea(s); contenido oculto)" >&2
    status=1
  fi
done < <(git diff --cached --name-only --diff-filter=ACMR)

[ "$status" -eq 0 ] && echo "pre-commit: secretos y .env OK"
exit "$status"
