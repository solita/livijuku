
-- name: select-osaston-hakemukset
select id, vuosi, nro, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus where osastoid = :osastoid


-- name: insert-hakemus<!
-- Lisää hakemus, palauta id.
insert into hakemus (id, vuosi, nro,
  hakuaika_alkupvm, hakuaika_loppupvm, osastoid)
values (hakemus_seq.nextval, :vuosi, :nro,
  :hakuaika_alkupvm, :hakuaika_loppupvm, :osastoid)