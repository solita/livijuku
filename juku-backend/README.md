Liikennevirasto - Juku palvelut
===============================

Tämä projekti sisältää Liikenneviraston Juku -järjestelmän palveluiden toteutuksen.

Kehityskäyttö
-------------

Leiningen build-työkalun asennus: http://leiningen.org/#install

Palvelimen käynnistys lokaalisti: **lein ring server-headless**

Palvelimen käynnistäminen edellyttää tietokannan
* oletus: jdbc:oracle:thin:@localhost:1521:orcl

Palvelujen rajapintadokumentaatio: http://localhost:3000/api/ui/index.html

Lokaalin tietokannan käyttäminen vagrantilla ks. ../juku-db/vagrant/README.md

### Palveluiden käyttäminen curl-työkalulla

Pyynnön otsikkotiedot (pakolliset kaikissa pyynnöissä):
* -H oam-remote-user:käyttäjätunnus
* -H oam-groups:käyttäjäroolit

Esimerkki: **curl** -v -H oam-remote-user:harri -H oam-groups:test http://localhost:8082/user

Liitteiden lähettäminen multipart/form-data-muodossa: --form liite="@tiedostonimi;type=mime-type"

Esimerkki: **curl** -v -H oam-remote-user:harri -H oam-groups:t --form liite="@README.md;type=text/plain" http://localhost:8082/hakemus/1/liite

Hakuohjeen päivittäminen: -X PUT --form hakuohje="@tiedostonimi;type=text/plain"

Esimerkki: **curl** -v -X PUT -include -H oam-remote-user:harri -H oam-groups:t --form hakuohje="@ohje.pdf;type=application/pdf" http://localhost:8082/hakemuskausi/2015/hakuohje

Tuotantokäyttö
--------------

Tuotantokäyttö edellyttää Java 1.8 JRE:n.

Käynnistäminen: **java -jar juku.jar**

Asetukset: **juku.properties** tiedosto, joka pitää olla ohjelman käynnistyshakemistossa.

Testaus
-------

Midje testit ajetaan komennolla **lein midje**
