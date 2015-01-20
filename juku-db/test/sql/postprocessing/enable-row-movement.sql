
begin
for i in (
  select 'alter table ' || table_name || ' enable row movement' statement from user_tables
)
loop
  model.putline_or_execute(i.statement);
end loop;
end;
/