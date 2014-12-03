
create sequence hakemus_seq
    increment by 1
    maxvalue 999999999999999999999999999
    minvalue 1
    cache 20
;

begin
  model.new_classification('hakemustyyppi', 'Hakemustyyppi', 3, 'HATYYPPI');
end;
/

insert into hakemustyyppi (tunnus, nimi) values ('AH0', 'Avustushakemus');
insert into hakemustyyppi (tunnus, nimi) values ('MH1', '1. maksatushakemus');
insert into hakemustyyppi (tunnus, nimi) values ('MH2', '2. maksatushakemus');

create table hakemus (
  id number constraint hakemus_pk primary key,
  vuosi number(4) not null,
  organisaatioid not null references organisaatio (id),
  hakemustyyppitunnus not null references hakemustyyppi (tunnus),
  hakuaika_alkupvm date not null,
  hakuaika_loppupvm date not null,

  suunniteltuavustus number(9,2)
);

begin
  model.define_mutable(model.new_entity('hakemus', 'Hakemus', 'HA'));
end;
/

