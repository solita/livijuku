
create table paatos (
  hakemusid not null references hakemus (id),
  paatosnumero number(3) not null,
  paattaja references kayttaja (tunnus),
  myonnettyavustus number(9,2),
  voimaantuloaika date,
  poistoaika date,

  selite clob,
  
  constraint paatos_pk primary key (hakemusid, paatosnumero)
);

declare
  e entity%rowtype := model.new_entity('paatos', 'Päätös', 'PA');
begin
  model.define_mutable(e);
  model.rename_fk_constraints(e);
end;
/
