
create sequence hakemus_seq
    increment by 1
    maxvalue 999999999999999999999999999
    minvalue 1
    cache 20
;

create table hakemus (
  id number constraint hakemus_pk primary key,
  vuosi number(4) not null,
  osastoid not null references osasto (id),
  nro number(1),
  hakuaika_alkupvm date not null,
  hakuaika_loppupvm date not null,

  suunniteltuavustus number(9,2)
);

begin
  model.define_mutable(model.new_entity('hakemus', 'Hakemus', 'HA'));
end;
/

begin
  model.new_classification('avustuskohdelaji', 'Avustuskohdelaji', 2, 'AKLAJI');
end;
/

create table avustuskohde (
  hakemusid not null references hakemus (id),
  avustuskohdelajitunnus references avustuskohdelaji (tunnus),
  haettavaavustus number(9,2),
  omarahoitus number(9,2),
  
  constraint avustuskohde_pk primary key (hakemusid, avustuskohdelajitunnus)
);

begin
  model.define_mutable(model.new_entity('avustuskohde', 'Avustuskohde', 'AK'));
end;
/

