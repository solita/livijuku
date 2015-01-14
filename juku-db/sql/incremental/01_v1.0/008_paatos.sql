
create table paatos (
  hakemusid not null references hakemus (id),
  paatosnumero number(3) not null,
  myonnettyavustus number(9,2),
  voimaantuloaika date,
  poistoaika date,

  selite clob,
  
  constraint paatos_pk primary key (hakemusid, paatosnumero)
);

begin
  model.define_mutable(model.new_entity('paatos', 'Päätös', 'PA'));
end;
/
