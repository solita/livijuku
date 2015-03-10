
-- name: select-max-vuosi-from-hakemuskausi
select nvl(max(vuosi), 100) next from hakemuskausi