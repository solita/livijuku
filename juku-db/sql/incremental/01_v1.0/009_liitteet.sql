


create table liite (
  hakemusid references hakemus (id),
  liitenumero number,
  nimi varchar2 (200 char),
  contenttype varchar2 (200 char),
  poistoaika date,
  sisalto blob,

  constraint liite_pk primary key (hakemusid, liitenumero)
);

declare
  e entity%rowtype := model.new_entity('liite');
begin
  model.define_mutable(e);
  model.rename_fk_constraints(e);
end;
/