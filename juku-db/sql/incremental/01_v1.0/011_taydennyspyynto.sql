
create table taydennyspyynto (
  hakemusid not null references hakemus (id),
  numero number(3) not null,
  maarapvm date,

  selite clob,

  constraint taydennyspyynto_pk primary key (hakemusid, numero)
);

begin
  model.define_mutable(model.new_entity('taydennyspyynto', 'Täydennyspyyntö', 'TP'));
end;
/