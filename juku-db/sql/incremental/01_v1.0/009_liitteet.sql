


create table liite (
  hakemusid not null references hakemus (id),
  liitenumero number,
  nimi varchar2 (200 char),
  kuvaus varchar2(2000 char),
  contenttype varchar2 (200 char),
  sisalto blob,

  constraint liite_pk primary key (hakemusid, liitenumero)
);

begin
  model.define_mutable(model.new_entity('liite'));
end;
/