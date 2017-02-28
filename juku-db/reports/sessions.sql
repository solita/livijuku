
------------------
-- open cursors --
------------------

-- source: https://docs.oracle.com/cd/E40329_01/admin.1112/e27149/cursor.htm#OMADM5352

-- cursor related parameters
select * from v$parameter where name like '%cursor%';

-- opened cursors in sessions 
select a.value, s.username, s.sid, s.serial#, s.module, s.action, s.client_identifier, s.status 
from v$sesstat a, v$statname b, v$session s 
where a.statistic# = b.statistic#  and s.sid=a.sid and b.name = 'opened cursors current' and s.username is not null
order by a.value desc;

-- display sqls which open most
select * from v$open_cursor where sid = :sid; --and user_name = :user_name;

-- highest open cursor and maximum open cursors allowed
select  max(a.value) as highest_open_cur, p.value as max_open_cur from v$sesstat a, v$statname b, v$parameter p 
where  a.statistic# = b.statistic#  and b.name = 'opened cursors current' and p.name= 'open_cursors' group by p.value;