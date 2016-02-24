


create table liite (
  hakemusid references hakemus (id),
  liitenumero number(6),
  nimi varchar2 (200 char),
  contenttype varchar2 (200 char),
  poistoaika date,
  sisalto blob,

  constraint liite_pk primary key (hakemusid, liitenumero)
);

begin
  model.define_mutable(model.new_entity('liite'));
end;
/