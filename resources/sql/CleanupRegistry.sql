SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE datname = 'camunda'
  AND pid <> pg_backend_pid();
DROP DATABASE camunda with (force);
SELECT run_command_on_workers(
               'SELECT COUNT(pg_terminate_backend(pg_stat_activity.pid)) FROM pg_stat_activity WHERE datname = ''${REGISTRY_NAME}'' AND pid <> pg_backend_pid()'
           );
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE datname = '${REGISTRY_NAME}'
  AND pid <> pg_backend_pid();
DELETE
FROM ddm_db_changelog
WHERE id = 'create-registry-db'
  and author = 'platform';
SELECT run_command_on_workers(
               'DELETE FROM ddm_db_changelog WHERE id=''create-registry-db'' and author = ''platform'';'
           );
DROP DATABASE ${REGISTRY_NAME} with (force);
SELECT run_command_on_workers('DROP DATABASE ${REGISTRY_NAME} with (force);');
