DELETE
FROM access_permissions
WHERE grantee_id > 1
  OR grantor_id > 1;

DELETE
FROM favorites
WHERE user_id > 1;

DELETE
FROM query_snippets
WHERE user_id > 1;

DELETE
FROM widgets
WHERE dashboard_id in
    (SELECT d.id
     FROM dashboards d
     WHERE d.user_id > 1);

DELETE
FROM dashboards WHERE user_id > 1;

DELETE
FROM alert_subscriptions
WHERE user_id > 1
  OR destination_id in
    (SELECT d.id
     FROM notification_destinations d
     WHERE d.user_id > 1)
  OR alert_id in
    (SELECT a.id
     FROM alerts a
     WHERE a.user_id > 1);

DELETE
FROM notification_destinations
WHERE user_id > 1;

DELETE
FROM api_keys
WHERE created_by_id > 1;

DELETE
FROM changes
WHERE user_id > 1;

DELETE
FROM events
WHERE user_id > 1;

DELETE
FROM alerts
WHERE user_id > 1
  OR query_id in
    (SELECT q.id
     FROM queries q
     WHERE q.user_id > 1
       OR q.last_modified_by_id > 1);

DELETE
FROM visualizations
WHERE query_id in
    (SELECT q.id
     FROM queries q
     WHERE q.user_id > 1
       OR q.last_modified_by_id > 1);

DELETE
FROM queries
WHERE user_id > 1
  OR last_modified_by_id > 1;

delete
from users
where id >1;
