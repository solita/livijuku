Juku palvelut
=============

Tämä projekti sisältää Liikenne- ja viestintäviraston Juku -järjestelmän taustapalveluiden toteutuksen.

Kehityskäyttö
-------------

Asenna [java][java], [docker][docker] ja [leiningen][leiningen].

Kehitystietokannan käynnistäminen ks. [juku-db](../juku-db)

Palvelimen käynnistys paikallisesti: `./start.sh`

Palvelujen rajapintadokumentaatio: `http://localhost:8080/documentation/`

### Palveluiden käyttäminen curl-työkalulla

Pyynnön otsikkotiedot (pakolliset kaikissa pyynnöissä):
* -H iv-user:käyttäjätunnus
* -H iv-groups:käyttäjäroolit
* -H o:käyttäjän organisaation id

Esimerkki: `curl -v -H iv-user:harri -H iv-groups:juku_hakija -H o:069205 http://localhost:8080/user`

Liitteiden lähettäminen multipart/form-data-muodossa: --form liite="@tiedostonimi;type=mime-type"

Esimerkki: `curl -v -H iv-user:harri -H iv-groups:juku_hakija -H o:069205 --form liite="@README.md;type=text/plain" http://localhost:8080/hakemus/1/liite`

Hakuohjeen päivittäminen: -X PUT --form hakuohje="@tiedostonimi;type=text/plain"

Esimerkki: `curl -v -X PUT -include -H iv-user:harri -H iv-groups:juku_hakija -H o:069205 --form hakuohje="@README.md;type=text/plain" http://localhost:8080/hakemuskausi/2015/hakuohje`

Tuotantokäyttö
--------------

Tuotantokäyttö edellyttää Java 1.11 JRE:n.

Käynnistäminen: `java -jar juku.jar`

Asetukset: **juku.properties** tiedosto, joka   
* on ohjelman käynnistyshakemistossa (oletussijainti) tai
* sijainti määritetään järjestelmäparametrilla: properties-file
  * esimerkki: `java -jar -Dproperties-file=/path/juku.properties juku.jar`

Testaus
-------

Midje testit ajetaan komennolla: `lein midje`

[leiningen]: http://leiningen.org
[java]: https://openjdk.java.net/
[docker]: https://www.docker.com/
