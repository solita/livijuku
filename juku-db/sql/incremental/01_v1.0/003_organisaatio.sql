
create sequence organisaatio_seq
    increment by 1
    maxvalue 999999999999999999999999999
    minvalue 1
    cache 20
;

create table organisaatio (
  id number constraint organisaatio_pk primary key,
  nimi varchar2(200 char)
);
