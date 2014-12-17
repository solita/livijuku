
insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
  values ('harri', 'Harri', 'Hakija', 1, 0);
  
insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
  values ('katri', 'Katri', 'Käsittelijä', (select id from organisaatio where lajitunnus = 'LV'), 0);