
create sequence osasto_seq
    increment by 1
    maxvalue 999999999999999999999999999
    minvalue 1
    cache 20
;

create table osasto (
  id number constraint osasto_pk primary key,
  nimi varchar2(200 char)
);
