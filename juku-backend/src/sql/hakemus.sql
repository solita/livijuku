
-- name: select-osaston-hakemukset
select id, vuosi, nro, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus where osastoid = :osastoid