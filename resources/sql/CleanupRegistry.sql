drop database if exists camunda with (force);


-- drop schema registry (if exists)
-- NB. This command also deletes tables from publication and subscription lists and table's distribution from citus
drop schema if exists registry cascade;

-- clean table ddm_liquibase_metadata
truncate table only ddm_liquibase_metadata;
-- restart sequence ddm_liquibase_metadata_metadata_id_seq
alter sequence ddm_liquibase_metadata_metadata_id_seq restart;

-- clean table ddm_rls_metadata
truncate table only ddm_rls_metadata;
-- restart sequence ddm_rls_metadata_rls_id_seq
alter sequence ddm_rls_metadata_rls_id_seq restart;

-- clean table ddm_role_permission
truncate table only ddm_role_permission;
-- restart sequence ddm_role_permission_permission_id_seq
alter sequence ddm_role_permission_permission_id_seq restart;

-- clean table ddm_source_application
truncate table only ddm_source_application;

-- clean table ddm_source_business_process
truncate table only ddm_source_business_process;

-- clean table ddm_source_system
truncate table only ddm_source_system;

-- clean table ddm_db_changelog from registry changeset's info
delete
from ddm_db_changelog
where author <> 'platform';

-- clean possible locks from previous registry deployments
delete from ddm_db_changelog_lock
  where locked = true and lockedby like 'dataplatform-jenkins-agent%';

-- create schema registry
create schema if not exists registry;
alter schema registry owner to ${OWNER_ROLE};

-- grants
grant usage on schema registry to public;

-- schema archive
-- drop schema archive (if exists)
drop schema if exists archive cascade;

-- create schema archive
create schema if not exists archive;
alter schema archive owner to ${OWNER_ROLE};

-- grants
grant usage on schema archive to public;
