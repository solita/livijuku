
create table hakemuskausi (
  vuosi number(4) constraint hakemuskausi_pk primary key,

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