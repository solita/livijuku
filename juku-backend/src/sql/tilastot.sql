
-- name: select-nousut
select vuosi, organisaatioid, sopimustyyppitunnus, kuukausi, sum(nousut) nousut
from fact_liikenne
group by vuosi, cube(organisaatioid, sopimustyyppitunnus, kuukausi)