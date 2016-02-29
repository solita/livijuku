
create table fact_joukkoliikennetuki (
  vuosi number(4),
  organisaatioid not null references organisaatio (id),
  avustuskohdeluokkatunnus not null references avustuskohdeluokka (tunnus),

  tuki number(12, 2),

  constraint fact_joukkoliikennetuki_pk primary key (vuosi, organisaatioid, avustuskohdeluokkatunnus)
);

begin
  model.define_mutable(model.new_entity('fact_joukkoliikennetuki', 'Joukkoliikennetuki fakta', 'FCTTUKI'));
end;
/