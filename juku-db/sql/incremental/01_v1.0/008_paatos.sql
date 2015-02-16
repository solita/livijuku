
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

-- Uniikki-indeksi, joka varmistaa että hakemuksella on vain yksi avoin päätös
create unique index paatos_hakemus_u on paatos (case when voimaantuloaika is null then hakemusid else null end);

declare
  e entity%rowtype := model.new_entity('paatos', 'Päätös', 'PA');
begin
  model.define_mutable(e);
  model.rename_fk_constraints(e);
end;
/
