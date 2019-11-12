Juku tietokanta
===============

Tämä projekti sisältää tietokantapäivitykset Oracle Juku-tietokannan skeeman luontiin ja päivitykseen.

Päivitysten hallintaan käytetään [dbmaintain-työkalua][dbmaintain].

Oletukset
---------

Päivitykset olettavat että tietokannassa on valmiina tietokantakäyttäjä: **juku**,
jolla tietokantaobjektit luodaan ja jolla on riittävät oikeudet muutosten tekemiseen.

Kaikki tietoa varaavat kantaobjektit luodaan juku-käyttäjän oletustaulualueeseen: **juku_data**.
Oletustaulualueen lisäksi tarvitaan taulualue indekseille: **juku_indx**.

Indeksitaulualue valitaan siten että sen nimi on `juku%_indx`, johon juku käyttäjällä on varattu tilaa (quota).

Tarvittavien taulualueiden luontiin löytyy esimerkit: [dba/tablespace.sql](dba/tablespace.sql)

Tarvittavien roolien ja käyttäjien luontiin löytyy esimerkit: 
[dba/roles.sql](dba/roles.sql) ja [dba/users.sql](dba/users.sql)

JDBC-ajurit
-----------
Oracle JDBC-ajurit ladataan oraclen [maven-varastosta][maven-repository]. 
Tänne pääsy edellyttää Oracle Technology Network (OTN) tunnukset. 
Ohjeet OTN tunnusten saamiseen löytyy [täältä][maven-repository].

Tunnukset tallennetaan tiedostoon: `~/.lein/profiles.clj` esim.
 
 `{:auth {:repository-auth {#"oracle" {:username "scott" :password "tiger"}}}}`

Kehityskäyttö
-------------

Asenna [java][java], [docker][docker] ja [leiningen][leiningen].

Käynnistä [tietokanta](/juku-db/docker).

Tietokannan päivitys:

    lein with-profiles +test-data run update-db

Tietokannan tyhjentäminen:

    lein run clear-db

Tyhjennys/päivitys:

    lein with-profiles +test-data do run clear-db, run update-db

Tämä kehityskäyttöön tarkoitettu päivitys lisää aina myös testidatan. 
Testidata on tarkoitettu automaattisten testien ajamista varten. 
SQL lähdetiedostot luetaan kansioista:
 - sql - tuotantokäyttöön tarkoitetut skeema-päivitykset
 - test/sql - automaattisia testejä varten tehty vakiodata esim. testikäyttäjät

Oletustietokanta-asetukset ovat:
- url = `jdbc:oracle:thin:@localhost:1521/orclpdb1.localdomain`
- user = `juku`
- password = `juku`

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

Asennusohjelma tuotetaan komennolla:

    lein uberjar

Kehitysympäristöt
-----------------
Kehitysympäristöt asennetaan samalla tavalla kuin tuotantoympäristö. 
Kehitysympäristössä kanta voidaan tyhjentää komennolla:

    java -jar juku-db.jar clear-db

Asennusohjelmasta voi tuottaa testidatan sisältävän version komennolla:

    lein with-profiles +test-data uberjar

[maven-repository]: https://maven.oracle.com
[dbmaintain]: http://www.dbmaintain.org
[leiningen]: http://leiningen.org
[java]: https://openjdk.java.net/
[docker]: https://www.docker.com/