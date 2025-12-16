#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DB_SPEC="$ROOT_DIR/contracts/db/db.v2.md"
MIGRATIONS_ROOT="$ROOT_DIR/services"
CONTAINER_NAME="codexpong-maria-test"
MYSQL_IMG="mariadb:10.6"
ROOT_PW="changeme"
MYSQL_PORT=3307

IDENTITY_DB_NAME=${IDENTITY_DB_NAME:-identity_service}
MATCH_DB_NAME=${MATCH_DB_NAME:-match_service}
CHAT_DB_NAME=${CHAT_DB_NAME:-chat_service}

IDENTITY_DB_USER=${IDENTITY_DB_USER:-identity_svc}
MATCH_DB_USER=${MATCH_DB_USER:-match_svc}
CHAT_DB_USER=${CHAT_DB_USER:-chat_svc}

IDENTITY_DB_PASSWORD=${IDENTITY_DB_PASSWORD:-identity_svc_pass}
MATCH_DB_PASSWORD=${MATCH_DB_PASSWORD:-match_svc_pass}
CHAT_DB_PASSWORD=${CHAT_DB_PASSWORD:-chat_svc_pass}

if [ ! -f "$DB_SPEC" ]; then
  echo "Database ownership spec missing: $DB_SPEC" >&2
  exit 1
fi

if [ ! -s "$DB_SPEC" ]; then
  echo "Database ownership spec is empty: $DB_SPEC" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is not available in this environment; skipping migration smoke tests." >&2
  echo "Set MIGRATION_TEST_REQUIRE_DOCKER=1 to force failure when docker is missing." >&2
  if [ "${MIGRATION_TEST_REQUIRE_DOCKER:-0}" = "1" ]; then
    exit 1
  else
    exit 0
  fi
fi

echo "Starting ephemeral MariaDB for migration smoke tests..."
docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
docker run -d --name "$CONTAINER_NAME" -e MARIADB_ROOT_PASSWORD="$ROOT_PW" -p ${MYSQL_PORT}:3306 "$MYSQL_IMG" >/dev/null

cleanup() {
  docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Waiting for MariaDB to become ready..."
for _ in {1..30}; do
  if docker exec "$CONTAINER_NAME" mysqladmin ping -h 127.0.0.1 -p"$ROOT_PW" --silent; then
    break
  fi
  sleep 2
done

MYSQL_ENV=(
  PATH="$PATH"
  MYSQL_ROOT_PASSWORD="$ROOT_PW"
  MYSQL_HOST=127.0.0.1
  MYSQL_PORT=3306
  MYSQL_DOCKER_CONTAINER="$CONTAINER_NAME"
  IDENTITY_DB_USER="$IDENTITY_DB_USER"
  IDENTITY_DB_PASSWORD="$IDENTITY_DB_PASSWORD"
  MATCH_DB_USER="$MATCH_DB_USER"
  MATCH_DB_PASSWORD="$MATCH_DB_PASSWORD"
  CHAT_DB_USER="$CHAT_DB_USER"
  CHAT_DB_PASSWORD="$CHAT_DB_PASSWORD"
)

echo "Provisioning schemas and service users..."
env "${MYSQL_ENV[@]}" "$ROOT_DIR/scripts/provision-test-db.sh"

apply_migrations() {
  local service_dir="$1" db_name="$2" db_user="$3" db_pass="$4"
  local migration_path="$MIGRATIONS_ROOT/$service_dir/db/migration"
  if [ ! -d "$migration_path" ]; then
    echo "Skipping $service_dir (no migrations present)"
    return
  fi
  mapfile -t migrations < <(find "$migration_path" -name "*.sql" | sort)
  if [ ${#migrations[@]} -eq 0 ]; then
    echo "Skipping $service_dir (no sql migrations)"
    return
  fi
  echo "Applying ${#migrations[@]} migration(s) for $service_dir as $db_user..."
  for file in "${migrations[@]}"; do
    echo "- $file"
    docker exec -i "$CONTAINER_NAME" mysql -h 127.0.0.1 -P 3306 -u "$db_user" -p"$db_pass" "$db_name" < "$file"
  done
  echo "Validating login for $db_user"
  docker exec "$CONTAINER_NAME" mysql -h 127.0.0.1 -P 3306 -u"$db_user" -p"$db_pass" -D "$db_name" -e "SELECT CURRENT_USER();" >/dev/null
}

apply_migrations identity-service "$IDENTITY_DB_NAME" "$IDENTITY_DB_USER" "$IDENTITY_DB_PASSWORD"
apply_migrations match-service "$MATCH_DB_NAME" "$MATCH_DB_USER" "$MATCH_DB_PASSWORD"
apply_migrations chat-service "$CHAT_DB_NAME" "$CHAT_DB_USER" "$CHAT_DB_PASSWORD"

assert_cross_schema_denied() {
  local db_user="$1" db_pass="$2" target_schema="$3" table_name="$4"
  set +e
  docker exec "$CONTAINER_NAME" mysql -h 127.0.0.1 -P 3306 -u"$db_user" -p"$db_pass" -e "SELECT 1 FROM ${target_schema}.${table_name} LIMIT 1;" >/dev/null 2>&1
  local status=$?
  set -e
  if [ $status -eq 0 ]; then
    echo "ERROR: user $db_user unexpectedly accessed ${target_schema}.${table_name}" >&2
    exit 1
  fi
  echo "Confirmed $db_user cannot query ${target_schema}.${table_name}"
}

echo "Validating cross-schema isolation..."
assert_cross_schema_denied "$IDENTITY_DB_USER" "$IDENTITY_DB_PASSWORD" "$MATCH_DB_NAME" "match_outbox"
assert_cross_schema_denied "$MATCH_DB_USER" "$MATCH_DB_PASSWORD" "$CHAT_DB_NAME" "chat_messages"
assert_cross_schema_denied "$CHAT_DB_USER" "$CHAT_DB_PASSWORD" "$IDENTITY_DB_NAME" "identity_users"

echo "Checking trace_id presence in match_outbox..."
docker exec "$CONTAINER_NAME" mysql -h 127.0.0.1 -P 3306 -u root -p"$ROOT_PW" -e "DESCRIBE ${MATCH_DB_NAME}.match_outbox" | grep -q "trace_id" && echo "trace_id column present in match_outbox"

echo "Migration smoke tests completed successfully."
