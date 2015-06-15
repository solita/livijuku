
create table paatos (
  hakemusid not null references hakemus (id),
  paatosnumero number(3) not null,
  paattaja references kayttaja (tunnus),
  paattajanimi varchar2(200 char),
  myonnettyavustus number(12,2),
  voimaantuloaika date,
  poistoaika date,

  selite clob,
  
  constraint paatos_pk primary key (hakemusid, paatosnumero)
);

-- Uniikki-indeksi, joka varmistaa että hakemuksella on vain yksi avoin päätös
create unique index paatos_hakemus_u on paatos (case when voimaantuloaika is null then hakemusid else null end);

begin
  model.define_mutable(model.new_entity('paatos', 'Päätös', 'PA'));
end;
/
