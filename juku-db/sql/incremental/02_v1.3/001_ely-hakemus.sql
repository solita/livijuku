
insert into hakemustyyppi (tunnus, nimi) values ('ELY', 'ELY hakemus');

begin
  model.new_classification('maararahatarvetyypi', 'Määrärahatarvetyyppi', 3, 'MRTARVETYYPPI');
end;
/

create table maararahatarve (
  hakemusid not null references hakemus (id),
  maararahatarvetyypitunnus not null references maararahatarvetyypi (tunnus),

  sidotut number,
  uudet number,
  tulot number,
  selite varchar2(2000 char),

  constraint maararahatarve_pk primary key (hakemusid, maararahatarvetyypitunnus)
);

create table kehityshanke (
  hakemusid not null references hakemus (id),
  numero number,

  nimi varchar2(200 char),
  arvo number,

  selite varchar2(200 char),

  constraint kehityshanke_pk primary key (hakemusid, numero)
);

begin
  model.define_immutable(model.new_entity('maararahatarve', 'Määrärahatarve', 'MRTARVE'));
  model.define_immutable(model.new_entity('kehityshanke', 'Kehityshanke', 'KHANKE'));
end;
/