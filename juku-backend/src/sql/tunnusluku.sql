
-- name: select-liikennevuositilasto
select kuukausi, lahdot, nousut, linjakilometrit
from fact_liikenne
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-liikennevuosi-if-not-exists!
insert into fact_liikenne (vuosi, kuukausi, organisaatioid, sopimustyyppitunnus)
select :vuosi vuosi, column_value kuukausi,
       :organisaatioid organisaatioid,
       :sopimustyyppitunnus sopimustyyppitunnus
from table(numbers(1,12))
where not exists (
    select 1 from fact_liikenne l where l.vuosi = :vuosi and
                                        l.organisaatioid = :organisaatioid and
                                        l.sopimustyyppitunnus = :sopimustyyppitunnus
)
order by vuosi, kuukausi, organisaatioid, sopimustyyppitunnus

-- name: select-liikenneviikkotilasto
select viikonpaivaluokkatunnus, lahdot, nousut, linjakilometrit
from fact_liikenneviikko
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-liikenneviikko-if-not-exists!
insert into fact_liikenneviikko (vuosi, viikonpaivaluokkatunnus, organisaatioid, sopimustyyppitunnus)
select :vuosi vuosi, tunnus viikonpaivaluokka,
       :organisaatioid organisaatioid,
       :sopimustyyppitunnus sopimustyyppitunnus
from viikonpaivaluokka
where not exists (
    select 1 from fact_liikenneviikko l
    where l.vuosi = :vuosi and
          l.organisaatioid = :organisaatioid and
          l.sopimustyyppitunnus = :sopimustyyppitunnus
)
order by vuosi, viikonpaivaluokka, organisaatioid, sopimustyyppitunnus