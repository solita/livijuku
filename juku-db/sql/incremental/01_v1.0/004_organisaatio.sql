
create sequence organisaatio_seq
    increment by 1
    maxvalue 999999999999999999999999999
    minvalue 1
    cache 20
;

begin
  model.new_classification('organisaatiolaji', 'Organisaatiolaji', 3, 'ORGLAJI');
end;
/

insert into organisaatiolaji (tunnus, nimi) values ('KS1', 'Suuri kaupunkiseutu');
insert into organisaatiolaji (tunnus, nimi) values ('KS2', 'Keskisuuri kaupunkiseutu');
insert into organisaatiolaji (tunnus, nimi) values ('ELY', 'ELY-keskus');
insert into organisaatiolaji (tunnus, nimi) values ('LV', 'Liikennevirasto');

create table organisaatio (
  id number constraint organisaatio_pk primary key,
  lajitunnus not null references organisaatiolaji (tunnus),
  nimi varchar2(200 char),
  pankkitilinumero varchar2(34 char)
);

declare
  e entity%rowtype := model.new_entity('organisaatio', 'Organisaatio', 'ORG');
begin
  model.define_mutable(e);
  model.rename_fk_constraints(e);
end;
/

alter table kayttaja add constraint kayttaja_organisaatio_fk foreign key (organisaatioid) references organisaatio (id);
