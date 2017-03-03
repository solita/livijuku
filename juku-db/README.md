Juku tietokanta
===============

Tämä projekti sisältää tietokantapäivitykset Oracle Juku-tietokannan skeeman päivitykseen ja luontiin tyhjästä.

Päivitysten hallintaan käytetään dbmaintain työkalua: http://www.dbmaintain.org/overview.html

Tätä työkalua käytetään leiningen asennustyökalulla: http://leiningen.org

Oletukset
---------

Päivitykset olettavat että tietokannassa on valmiina tietokantakäyttäjä: **juku**,
jolla tietokantaobjektit luodaan ja jolla on riittävät oikeudet muutosten tekemiseen.

Kaikki tietoa varaavat kantaobjektit luodaan juku-käyttäjän oletustaulualueeseen: **juku_data**.
Oletustaulualueen lisäksi tarvitaan taulualue indekseille: **juku_indx**.

Indeksitaulualue valitaan siten että sen nimi on juku%_indx, johon juku käyttäjällä on varattu tilaa (quota).

Tarvittavien taulualueiden luontiin löytyy esimerkit: users/tablespace.sql

Tarvittavien käyttäjien luontiin löytyy esimerkit: users/users.sql

JDBC-ajurit
-----------
Oracle JDBC-ajurit ladataan oraclen [maven-varastosta](maven-repository). 
Tänne pääsy edellyttää Oracle Technology Network (OTN) tunnukset. 
Ohjeet OTN tunnusten saamiseen löytyy [täältä](maven-repository).

Tunnukset tallennetaan tiedostoon: `~/.lein/profiles.clj` esim.
 
 `{:auth {:repository-auth {#"oracle" {:username "scott" :password "tiger"}}}}`

Kehityskäyttö
-------------

Leiningen build-työkalun asennus: http://leiningen.org/#install

Tietokannan päivitys:

    lein run update-db

Tietokannan tyhjentäminen:

    lein run clear-db

Tyhjennys/päivitys:

    lein do run clear-db, run update-db

Ohjeet tietokantapalvelimen käyttämiseen vagrant-työkalulla löytyy: vagrant/README.md

Tämä kehityskäyttöön tarkoitettu päivitys lisää aina myös testidatan. SQL lähdetiedostot luetaan kansioista:
 - sql - tuotantokäyttöön tarkoitetut skeema-päivitykset
 - test/sql - testauksia varten luotava vakiodata

Oletustietokanta-asetukset ovat:
- url = jdbc:oracle:thin:@localhost:1521:orcl
- user = juku
- password = juku

Asetuksia voi muuttaa ympäristömuuttujilla:
- DB_URL
- DB_USER
- DB_PASSWORD

Esim. letto-tietokannassa oleva kehitysympäristön päivitys:

    DB_URL=letto.solita.fi:1521/ldev.solita.fi lein do run clear-db, run update-db

Tuotantoasennus
---------------

Tuotantoasennuksessa käytetään lein-työkalun sijasta itsenäistä java-ohjelmaa juku-db.jar.
Tämä paketti sisältää kaiken tarvittavan tietokannan päivittämiseen.

Tuotantoasennuksessa tuotantokannan osoite ja salasana annetaan ympäristömuuttujina esim.

    DB_URL=oracle.livi.fi:1521/juku.livi.fi DB_PASSWORD=trustno1 java -jar juku-db.jar update-db


[maven-repository]: https://maven.oracle.com`