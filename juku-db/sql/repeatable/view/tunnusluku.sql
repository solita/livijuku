
create or replace view fact_lipputulo_unpivot_view as
select vuosi, kuukausi, organisaatioid, sopimustyyppitunnus, lipputuloluokkatunnus, tulo
from fact_lipputulo
unpivot include nulls (tulo for lipputuloluokkatunnus in (
  kertalipputulo as 'KE',
  arvolipputulo as 'AR',
  kausilipputulo as 'KA',
  lipputulo as 'ALL'))
;

create or replace view fact_kustannus_unpivot_view as
select vuosi, organisaatioid, kustannuslajitunnus, kustannus
from fact_alue
unpivot include nulls (kustannus for kustannuslajitunnus in (
  kustannus_asiakaspalvelu as 'AP',
  kustannus_konsulttipalvelu as 'KP',
  kustannus_lipunmyyntipalkkio as 'LP',
  kustannus_jarjestelmat as 'TM',
  kustannus_muutpalvelut as 'MP'))
;

create or replace view fact_lippuhinta_unpivot_view as
select vuosi, organisaatioid, vyohykemaara, lippuhintaluokkatunnus, hinta
from fact_lippuhinta
unpivot include nulls (hinta for lippuhintaluokkatunnus in (
  kertalippuhinta as 'KE',
  kausilippuhinta as 'KA'))
;