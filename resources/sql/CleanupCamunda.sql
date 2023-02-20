\c camunda

--truncate all tables in camunda db except mentioned in citus liquibase scripts
select 'TRUNCATE' ||' '||table_name ||' '||'CASCADE'  from information_schema.tables
    where table_schema = 'public'
        and table_type = 'BASE TABLE'
        and table_name not in
            ('act_ge_property', 'act_ge_schema_log', 'ddm_db_changelog', 'ddm_db_changelog_lock')
        and current_database() = 'camunda'
\gexec