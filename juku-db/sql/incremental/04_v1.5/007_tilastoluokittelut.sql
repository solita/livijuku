
begin
  model.new_classification('korvauslaji', 'Korvauslaji', 2, 'KOLAJI');
  model.new_classification('kustannuslaji', 'Kustannuslaji', 2, 'KULAJI');
  model.new_classification('lippuhintaluokka', 'Lippuhintaluokka', 2, 'LHLUOKKA');
  model.new_classification('lipputuloluokka', 'Lipputuloluokka', 3, 'LTLUOKKA');
end;
/

insert into korvauslaji (tunnus, nimi) values ('K', 'Korvaus');
insert into korvauslaji (tunnus, nimi) values ('NK', 'Nousukorvaus');
insert into korvauslaji (tunnus, nimi) values ('KK', 'Kuntakorvaus');

insert into kustannuslaji (tunnus, nimi) values ('AP', 'Asiakaspalvelu');
insert into kustannuslaji (tunnus, nimi) values ('KP', 'Konsulttipalvelu');
insert into kustannuslaji (tunnus, nimi) values ('LP', 'Lipunmyyntipalkkiot');
insert into kustannuslaji (tunnus, nimi) values ('TM', 'Tieto-/maksujärjestelmät');
insert into kustannuslaji (tunnus, nimi) values ('MP', 'Muut palvelut');

insert into lippuhintaluokka (tunnus, nimi) values ('KE', 'Kertalippu');
insert into lippuhintaluokka (tunnus, nimi) values ('KA', 'Kausilippu');

insert into lipputuloluokka (tunnus, nimi) values ('KE', 'Kertalippu');
insert into lipputuloluokka (tunnus, nimi) values ('KA', 'Kausilippu');
insert into lipputuloluokka (tunnus, nimi) values ('AR', 'Arvolippu');
insert into lipputuloluokka (tunnus, nimi) values ('ALL', 'Lipputulo');