
-- Uusi avustuskohdeluokka
insert into avustuskohdeluokka (tunnus, nimi, jarjestys)
values ('PK', 'Julkisen palvelun velvoitteesta maksettava korvaus', 2);

insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjestys, voimaantulovuosi)
values ('PK', 'LT', 'Lipputuki', 1, 2020);

-- Lakkautetaan kaikki lajit luokalta HK
update avustuskohdelaji set lakkaamisvuosi = 2020
where avustuskohdeluokkatunnus = 'HK';

-- Olemassaolevan avustuskohdeluokan nimen muutos
update avustuskohdeluokka set nimi = 'Liikenteen palvelujen kehittäminen'
where tunnus = 'K';

update avustuskohdelaji set nimi = 'Lippu- ja maksujärjestelmien kehittäminen'
where avustuskohdeluokkatunnus = 'K' and tunnus = 'IM';
update avustuskohdelaji set nimi = 'Liikenteen palveluiden suunnittelu tai kehittäminen'
where avustuskohdeluokkatunnus = 'K' and tunnus = 'MPK';
update avustuskohdelaji set nimi = 'Liikenteen palveluiden kokeiluhankkeet'
where avustuskohdeluokkatunnus = 'K' and tunnus = 'MK';

-- Lakkautetaan avustuskohdelaji: Raitiotien suunnittelu (K/RT)
update avustuskohdelaji set lakkaamisvuosi = 2020
where avustuskohdeluokkatunnus = 'K' and tunnus = 'RT';

