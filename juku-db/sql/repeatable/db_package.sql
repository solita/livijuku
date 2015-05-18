
create or replace package db as
  procedure grants;
  procedure revoke_grants;
  procedure grant_selects_user;
  procedure grants_user;
  procedure move_indexes;
  procedure create_idx(idx_statement varchar2);
end db;
/

create or replace package body db as
    
    procedure revoke_grants is
    begin 
        for cur in (
          select 'revoke ' || privilege || ' on ' || user || '.' || table_name || ' from ' || grantee stm from all_tab_privs_made 
          where table_name not in ('DB', 'MODEL') and not(table_name like 'SYSTP%')
        )
        loop
            model.putline_or_execute(cur.stm);
        end loop;
    end;
    
    procedure grant_selects_user is
      application_user constant varchar2(30 char) := user || '_app';
    begin  
        for cur in (select 'grant select on ' || table_name || ' to ' || application_user stm from user_tables where table_name in (select table_name from entity))
        loop
            model.putline_or_execute(cur.stm);
        end loop;
        for cur in (select 'grant select on ' || view_name || ' to ' || application_user stm from user_views where view_name like '%_VIEW')
        loop
            model.putline_or_execute(cur.stm);
        end loop;
        for cur in (select 'grant select on ' || sequence_name || ' to ' || application_user stm from user_sequences)
        loop
            model.putline_or_execute(cur.stm);
        end loop;
        for cur in (select 'grant execute on ' || object_name || ' to ' || application_user stm from user_procedures where object_name not in (' model.putline_or_execute') and object_type not in ('TRIGGER'))
        loop
            model.putline_or_execute(cur.stm);
        end loop;
    end;
    
    procedure grants_user is
      application_user constant varchar2(30 char) := user || '_app';
    begin
        /*mutable entities*/
        for cur in (
            select 'grant insert, update, delete on ' || table_name || ' to ' || application_user comm from (
            select table_name 
              from entitytypeentity 
            where entitytypename = 'mutable'
            minus 
            select table_name 
              from entitytypeentity 
            where
                entitytypename in ('class','state','immutable','localizable')
                or table_name in ('kieli') -- ei kälin kautta päivitettävää tai tuhottavaa
        ))
        loop
            model.putline_or_execute(cur.comm);
        end loop;

        /*immutable entities*/
        for cur in (
          select 'grant insert, delete on ' || table_name || ' to ' || application_user comm from (
            select table_name
            from entitytypeentity
            where entitytypename = 'immutable'
          ))
        loop
          model.putline_or_execute(cur.comm);
        end loop;
    end grants_user;
    
    procedure grants is
    begin
        revoke_grants;
        grant_selects_user;
        grants_user;
    end grants;
    
    --
    -- Siirtää indeksit JUKU%_INDX taulutilaan, johon käyttäjällä on quotaa
    procedure move_indexes is
    begin
        for cur in (
        select 'alter index '|| index_name ||' rebuild tablespace ' || 
            index_tablespace.tablespace_name comm
          from user_indexes ind, (
            select tablespace_name from (
              select tsq.tablespace_name from user_ts_quotas tsq 
              where tsq.tablespace_name like 'JUKU%_INDX'
              order by tsq.tablespace_name asc
            ) where rownum = 1
          ) index_tablespace
         where ind.tablespace_name != index_tablespace.tablespace_name
           and ind.index_type like '%NORMAL'
         order by 1)
        loop
            model.putline_or_execute(cur.comm);
        end loop;
    end move_indexes;
    
    procedure create_idx(idx_statement varchar2) is
      idx_tablespace_name varchar2(30 CHAR);  
    begin
      select tablespace_name into idx_tablespace_name from (
        select tsq.tablespace_name from user_ts_quotas tsq 
        where tsq.tablespace_name like 'JUKU%_INDX'
        order by tsq.tablespace_name asc
      ) where rownum = 1;
      
       model.putline_or_execute(idx_statement || ' tablespace ' || idx_tablespace_name);
    end;
end;
/
