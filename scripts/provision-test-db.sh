#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MYSQL_HOST=${MYSQL_HOST:-127.0.0.1}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_ROOT_USER=${MYSQL_ROOT_USER:-root}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-}
MYSQL_DOCKER_CONTAINER=${MYSQL_DOCKER_CONTAINER:-}

if [ -z "$MYSQL_ROOT_PASSWORD" ]; then
  echo "MYSQL_ROOT_PASSWORD is required to provision schemas/users" >&2
  exit 1
fi

services=(
  "identity_service:${IDENTITY_DB_USER:-identity_svc}:${IDENTITY_DB_PASSWORD:-identity_svc_pass}"
  "match_service:${MATCH_DB_USER:-match_svc}:${MATCH_DB_PASSWORD:-match_svc_pass}"
  "chat_service:${CHAT_DB_USER:-chat_svc}:${CHAT_DB_PASSWORD:-chat_svc_pass}"
)

mysql_cmd=(mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_ROOT_USER" -p"$MYSQL_ROOT_PASSWORD")
if [ -n "$MYSQL_DOCKER_CONTAINER" ]; then
  mysql_cmd=(docker exec -i "$MYSQL_DOCKER_CONTAINER" mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_ROOT_USER" -p"$MYSQL_ROOT_PASSWORD")
fi

for svc in "${services[@]}"; do
  IFS=":" read -r db user pass <<<"$svc"
  echo "Provisioning schema $db and user $user" >&2
  "${mysql_cmd[@]}" <<SQL
CREATE DATABASE IF NOT EXISTS ${db};
CREATE USER IF NOT EXISTS '${user}'@'%' IDENTIFIED BY '${pass}';
GRANT ALL PRIVILEGES ON ${db}.* TO '${user}'@'%';
FLUSH PRIVILEGES;
SQL
  echo "- ensured ${db} exists with dedicated user"
done
