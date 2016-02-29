
insert into hakemuskausi (vuosi) values (2017);

insert into hakuaika (vuosi,hakemustyyppitunnus,alkupvm,loppupvm) values (2017, 'AH0', to_date('01.09.2016','DD.MM.YYYY'), to_date('15.12.2017','DD.MM.YYYY'));
insert into hakuaika (vuosi,hakemustyyppitunnus,alkupvm,loppupvm) values (2017, 'MH1', to_date('01.07.2017','DD.MM.YYYY'), to_date('31.08.2017','DD.MM.YYYY'));
insert into hakuaika (vuosi,hakemustyyppitunnus,alkupvm,loppupvm) values (2017, 'MH2', to_date('01.01.2018','DD.MM.YYYY'), to_date('31.01.2018','DD.MM.YYYY'));
insert into hakuaika (vuosi,hakemustyyppitunnus,alkupvm,loppupvm) values (2017, 'ELY', to_date('01.09.2016','DD.MM.YYYY'), to_date('31.10.2016','DD.MM.YYYY'));



-- ui test fix - käyttöliittymätestit ei mene läpi jos 2016 kauden avustushakemuksen hakuaika on käynnisssä
-- siirretään hakuaikaa testidatassa eteenpäin tulevaisuuteen
update hakuaika set alkupvm = to_date('01.07.2016','DD.MM.YYYY'), loppupvm = to_date('31.08.2016','DD.MM.YYYY')
where vuosi = 2016 and hakemustyyppitunnus = 'AH0';
