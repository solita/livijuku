
comment on table fact_liikennointikorvaus is
  'Liikennöinnin järjestämiseen liittyvien korvauksien tunnusluvut eriteltynä organisaation, vuoden, kuukauden ja sopimustypin mukaan.';

comment on column fact_liikennointikorvaus.korvaus is
  'TVV:n maksama liikennöintikorvaus tietylle sopimustyypille ja kuukaudelle.
Korvauksen tarkempi merkitys riippuu sopimustyypistä ja tämä tieto merkitään kaikille sopimustyypeille.';

comment on column fact_liikennointikorvaus.nousukorvaus is
  'Käyttöoikeussopimusliikenteen tapauksessa TVV:n liikennöintikorvauksen lisäksi maksama tietyn kuukauden nousukorvaus.';

comment on column fact_liikennointikorvaus.nousut is
  'Markkinaehtoisen liikenteen tapuksessa liikennöintikorvauksen lisäksi merkitään korvauksen piirissä olevat nousut kuukausittain.';

comment on column fact_liikennointikorvaus.kuntakorvaus is
  'ELY:jen tapauksessa liikennöintikorvaus eritellään ELY:n maksamaan korvaukseen (korvaus-kenttä) ja kuntien maksamaan korvaukseen (kuntakorvaus-kenttä).';