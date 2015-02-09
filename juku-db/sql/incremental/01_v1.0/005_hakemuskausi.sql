
begin
  model.new_state('hakemuskausitila', 'Hakemuskausitila', 2, 'HKTILA');
end;
/

insert into hakemuskausitila (tunnus, nimi) values ('A', 'Avoin');
insert into hakemuskausitila (tunnus, nimi) values ('K', 'Käynnistetty');
insert into hakemuskausitila (tunnus, nimi) values ('S', 'Suljettu');

create table hakemuskausi (
  vuosi number(4) constraint hakemuskausi_pk primary key,
  tilatunnus varchar2(2 char) default 'A' not null references hakemuskausitila (tunnus),

  hakuohje_nimi varchar2 (200 char),
  hakuohje_contenttype varchar2 (200 char),
  hakuohje_sisalto blob
);

declare
  e entity%rowtype := model.new_entity('hakemuskausi', 'Hakemuskausi', 'HK');
begin
  model.define_mutable(e);
  model.rename_fk_constraints(e);
end;
/