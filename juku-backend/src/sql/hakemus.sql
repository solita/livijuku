
-- name: select-osaston-hakemukset
select id, vuosi, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus where osastoid = :osastoid


-- name: insert-hakemus<!
-- Lisää hakemus, palauta id.
insert into hakemus (id, vuosi,
  hakuaika_alkupvm, hakuaika_loppupvm, osastoid, avustushakemusid)
values (hakemus_seq.nextval, :vuosi,
  :hakuaika_alkupvm, :hakuaika_loppupvm, :osastoid, :avustushakemusid)