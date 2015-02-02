
create table hakemuskausi (
  vuosi number(4) primary key,
  hakuohje blob
);

declare
  e entity%rowtype := model.new_entity('hakemuskausi', 'Hakemuskausi', 'HK');
begin
  model.define_mutable(e);
  model.rename_fk_constraints(e);
end;
/