
-- uusi avustuskohdeluokka
insert into avustuskohdeluokka (tunnus, nimi, jarjestys)
values ('PK', 'Julkisen palvelun velvoitteesta maksettava korvaus', 2);

-- aikaisemman avustuskohdeluokan nimen muutos
update avustuskohdeluokka set nimi = 'Liikenteen palvelujen kehittäminen'
where tunnus = 'K';

update avustuskohdelaji set lakkaamisvuosi = 2020
where avustuskohdeluokkatunnus = 'HK';

insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjestys, voimaantulovuosi)
values ('PK', 'LT', 'Lipputuki', 1, 2020);

update avustuskohdelaji set lakkaamisvuosi = 2020
where avustuskohdeluokkatunnus = 'K';

insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjestys, voimaantulovuosi)
values ('K', 'LMK', 'Lippu- ja maksujärjestelmien kehittäminen', 1, 2020);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjestys, voimaantulovuosi)
values ('K', 'PSK', 'Liikenteen palveluiden suunnittelu tai kehittäminen', 2, 2020);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjestys, voimaantulovuosi)
values ('K', 'PKH', 'Liikenteen palveluiden kokeiluhankkeet', 3, 2020);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjestys, voimaantulovuosi)
values ('K', 'M2', 'Muu hanke', 4, 2020);
