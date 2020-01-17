
create sequence asiakirjamalli_seq
  increment by 1
  maxvalue 999999999999999999999999999
  minvalue 1
  cache 20
;

begin
  model.new_classification('asiakirjalaji', 'Asiakirjalaji', 2, 'AKIRJALAJI');
end;
/

insert into asiakirjalaji (tunnus, nimi) values ('H', 'Hakemus');
insert into asiakirjalaji (tunnus, nimi) values ('P', 'Päätös');

create table asiakirjamalli (
  id number(19) constraint asiakirjamalli_pk primary key,
  voimaantulovuosi number(4) default 0 not null,
  asiakirjalajitunnus not null references asiakirjalaji (tunnus),
  hakemustyyppitunnus references hakemustyyppi (tunnus),
  organisaatiolajitunnus references organisaatiolaji (tunnus),
  sisalto clob,
  poistoaika date
);

begin
  model.define_mutable(model.new_entity('asiakirjamalli', 'Asiakirjamalli', 'AM'));
end;
/

create unique index asiakirjamalli_u on asiakirjamalli
  (voimaantulovuosi, asiakirjalajitunnus, hakemustyyppitunnus, organisaatiolajitunnus,
   case when poistoaika is null then 0 else id end);
