Juku tietokanta
===============

Tämä projekti sisältää tietokantapäivitykset Oracle Juku-tietokannan skeeman päivitykseen ja luontiin tyhjästä.

Päivitysten hallintaan käytetään dbmaintain työkalua: http://www.dbmaintain.org/overview.html

Tätä työkalua käytetään (leiningen asennustyökalulla): http://leiningen.org

Oletukset
---------

Päivitykset olettavat että tietokannassa on valmiina tietokantakäyttäjä: **juku**,
jolla tietokantaobjektit luodaan ja jolla on riittävät oikeudet muutosten tekemiseen.

Kaikki tietoa varaavat kantaobjektit luodaan juku-käyttäjän oletustaulualueeseen: **juku_data**.
Oletustaulualueen lisäksi tarvitaan taulualue indekseille: **juku_indx**.

Indeksitaulualue valitaan siten että sen nimi on juku%_indx, johon juku käyttäjällä on varattu tilaa (quota).

Tarvittavien taulualueiden luontiin löytyy esimerkit: users/tablespace.sql

Tarvittavien käyttäjien luontiin löytyy esimerkit: users/users.sql

Kehityskäyttö
-------------

Leiningen build-työkalun asennus: http://leiningen.org/#install

Tietokannan päivitys:

    lein update-db

Tietokannan tyhjentäminen:

    lein clear-db

Tyhjennys/päivitys:

    lein do clear-db, update-db

Ohjeet tietokantapalvelimen käyttämiseen vagrant-työkalulla löytyy: vagrant/README.md

Testidata: (test/sql/repeatable.test) on mahdollista lisätä tietokantaan
päivityksen yhteydessä käyttämällä profiilia **test-data**

    lein with-profiles +test-data do clear-db, update-db

Oletustietokanta-asetukset ovat:
- url = jdbc:oracle:thin:@localhost:1521:orcl
- user = juku
- password = juku

Asetuksia voi muuttaa ympäristömuuttujilla:
- DB_URL
- DB_USER
- DB_PASSWORD

Esim. letto-tietokannassa oleva kehitysympäristön päivitys:

    DB_URL=letto.solita.fi:1521/ldev.solita.fi lein with-profiles +test-data do clear-db, update-db

Livin kehitysympäristö
----------------------

Kehitysympäristön tietokannan voi alustaa komennolla:

    livi-kehitys-clear-db-update-db-test-data.sh <DB_USER> <DB_PASSWORD>


Tuotantoasennus
---------------

Tuotantoasennuksessa tuotantokannan osoite ja salasana annetaan ympäristömuuttujina esim.

    DB_URL=oracle.livi.fi:1521/juku.livi.fi DB_PASSWORD=trustno1 lein with-profiles +test-data do clear-db, update-db
