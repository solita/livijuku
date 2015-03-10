
-- name: select-max-vuosi-from-hakemuskausi
select nvl(max(vuosi), 0) next from hakemuskausi