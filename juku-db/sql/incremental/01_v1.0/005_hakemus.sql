
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

create table hakemustila (
  tunnus varchar2(2 char) constraint hakemustila_pk primary key,
  nimi varchar2(100 char),
  description varchar2(2000 char)
);

insert into hakemustila (tunnus, nimi) values ('K', 'Keskeneräinen');
insert into hakemustila (tunnus, nimi) values ('V', 'Vireillä');
insert into hakemustila (tunnus, nimi) values ('TA', 'Tarkastettu');
insert into hakemustila (tunnus, nimi) values ('T0', 'Täydennettävää');
insert into hakemustila (tunnus, nimi) values ('TV', 'Täydennytty - täydennys valmis tarkastettavaksi');
insert into hakemustila (tunnus, nimi) values ('P', 'Päätetty');
insert into hakemustila (tunnus, nimi) values ('M', 'Maksettu');

create table hakemus (
  id number constraint hakemus_pk primary key,
  vuosi number(4) not null,
  organisaatioid not null references organisaatio (id),
  hakemustyyppitunnus not null references hakemustyyppi (tunnus),
  hakemustilatunnus varchar2(2 char) default 'K' not null references hakemustila (tunnus),
  hakuaika_alkupvm date not null,
  hakuaika_loppupvm date not null,
  suunniteltuavustus number(9,2),
  selite clob
);

begin
  model.define_mutable(model.new_entity('hakemus', 'Hakemus', 'HA'));
end;
/


