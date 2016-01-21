
-- name: select-liikennevuositilasto
select kuukausi, lahdot, nousut, linjakilometrit
from fact_liikenne
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-liikennevuositilasto-if-not-exists!
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

-- name: insert-default-liikenneviikkotilasto-if-not-exists!
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

-- name: select-kalusto
select paastoluokkatunnus, lukumaara
from fact_kalusto
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-kalusto-if-not-exists!
insert into fact_kalusto (vuosi, paastoluokkatunnus, organisaatioid, sopimustyyppitunnus)
  select :vuosi vuosi, tunnus paastoluokkatunnus,
         :organisaatioid organisaatioid,
         :sopimustyyppitunnus sopimustyyppitunnus
  from paastoluokka
  where not exists (
      select 1 from fact_kalusto l
      where l.vuosi = :vuosi and
            l.organisaatioid = :organisaatioid and
            l.sopimustyyppitunnus = :sopimustyyppitunnus
  )
  order by vuosi, paastoluokkatunnus, organisaatioid, sopimustyyppitunnus

-- name: select-lippuhinta
select vyohykemaara, kertalippuhinta, kausilippuhinta
from fact_lippuhinta
where vuosi = :vuosi and organisaatioid = :organisaatioid

-- name: insert-default-lippuhinta-if-not-exists!
insert into fact_lippuhinta (vuosi, vyohykemaara, organisaatioid)
  select :vuosi vuosi,
         column_value vyohykelukumaara,
         :organisaatioid organisaatioid
  from table(numbers(1,6))
  where not exists (
      select 1 from fact_lippuhinta l
      where l.vuosi = :vuosi and
            l.organisaatioid = :organisaatioid
  )
  order by vuosi, vyohykelukumaara, organisaatioid

-- name: select-lipputulo
select kuukausi, kertalipputulo, arvolipputulo, kausilipputulo, lipputulo
from fact_lipputulo
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-lipputulo-if-not-exists!
insert into fact_lipputulo (vuosi, kuukausi, organisaatioid, sopimustyyppitunnus)
  select :vuosi vuosi,
         column_value kuukausi,
         :organisaatioid organisaatioid,
         :sopimustyyppitunnus sopimustyyppitunnus
  from table(numbers(1,12))
  where not exists (
      select 1 from fact_lipputulo l
      where l.vuosi = :vuosi and
            l.organisaatioid = :organisaatioid and
            l.sopimustyyppitunnus = :sopimustyyppitunnus
  )
  order by vuosi, kuukausi, organisaatioid, sopimustyyppitunnus

-- name: select-liikennointikorvaus
select kuukausi, korvaus, nousukorvaus
from fact_liikennointikorvaus
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-liikennointikorvaus-if-not-exists!
insert into fact_liikennointikorvaus (vuosi, kuukausi, organisaatioid, sopimustyyppitunnus)
  select :vuosi vuosi,
         column_value kuukausi,
         :organisaatioid organisaatioid,
         :sopimustyyppitunnus sopimustyyppitunnus
  from table(numbers(1,12))
  where not exists (
      select 1 from fact_liikennointikorvaus l
      where l.vuosi = :vuosi and
            l.organisaatioid = :organisaatioid and
            l.sopimustyyppitunnus = :sopimustyyppitunnus
  )
  order by vuosi, kuukausi, organisaatioid, sopimustyyppitunnus

-- name: select-alue
select
  kuntamaara,
  vyohykemaara,
  pysakkimaara,
  maapintaala,
  asukasmaara,
  tyopaikkamaara,
  henkilosto,
  
  pendeloivienosuus,
  henkiloautoliikennesuorite,
  autoistumisaste,
  asiakastyytyvaisyys,
  
  kustannus_asiakaspalvelu,
  kustannus_konsulttipalvelu,
  kustannus_lipunmyyntipalkkio,
  kustannus_jarjestelmat,
  kustannus_muutpalvelut
from fact_alue where vuosi = :vuosi and organisaatioid = :organisaatioid