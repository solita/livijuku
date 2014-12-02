
-- name: select-organisaation-hakemukset
select id, vuosi, hakemustyyppitunnus, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus where organisaatioid = :organisaatioid