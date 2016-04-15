
create sequence kilpailutus_seq
  increment by 1
  maxvalue 999999999999999999999999999
  minvalue 1
  cache 20
;

create table kilpailutus (
  id number(19) constraint kilpailutus_pk primary key,
  organisaatioid not null references organisaatio (id),
  kohdenimi varchar2(200 char),
  kohdearvo number(12,2),
  kalusto number(9),
  selite clob,

  julkaisupvm date,
  tarjouspaattymispvm date,
  hankintapaatospvm date,
  liikennointialoituspvm date,
  liikennointipaattymispvm date,
  hankittuoptiopaattymispvm date,
  optiopaattymispvm date,

  optioselite varchar2(1000 char),

  liikennoitsijanimi varchar2(200 char),
  tarjousmaara number(6),
  tarjoushinta1 number(12,2),
  tarjoushinta2 number(12,2)
);

begin
  model.define_mutable(model.new_entity('kilpailutus', 'Kilpailutus', 'KI'));
end;
/