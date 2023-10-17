\c notifications

--truncate table notification_template in notifications db
truncate table notification_template cascade;
--truncate table inbox_notification in notifications db
truncate table inbox_notification;

\gexec