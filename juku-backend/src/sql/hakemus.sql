
-- name: select-osaston-hakemukset
select id, vuosi, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus where osastoid = :osastoid