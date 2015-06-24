
-- name: select-hakemus-organisaatiot
select organisaatioid from hakemus where id in (:hakemusids)