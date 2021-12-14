-- search for process_history DB
select coalesce(max(datname), '') as db
  from pg_database where datname = 'process_history'
\gset
-- if we found process_history DB then connect to it, otherwise stay in current DB
\c :db

do '
begin
  -- check if we connected to process_history DB
  if not exists (select where current_database() = ''process_history'') then
    return;
  end if;
  -- clean table bpm_history_process
  truncate table only bpm_history_process;
  -- clean table bpm_history_task
  truncate table only bpm_history_task;
end
';
