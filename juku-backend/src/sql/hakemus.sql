
-- name: select-organisaation-hakemukset
select id, vuosi, nro, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus where organisaatioid = :organisaatioid