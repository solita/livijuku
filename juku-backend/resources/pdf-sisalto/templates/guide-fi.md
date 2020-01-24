# Asiakirjamallin kirjoitusohjeet

Asiakirjamallien pohjalta täydennetään järjestelmän muodostamat asiakirjat. 

Asiakirjamalli on puhdasta tekstiä, jossa muotoilu ja täydennettävät kohdat määritetään tietyillä erikoismerkeillä.

Tässä dokumentissa esitellään tuetut muotoilut ja käytettävissä olevat täydennettävät kohdat.

## Muotoilusäännöt

Muotoilusäännöt perustuvat markdown-standardiin.

### Otsikot

    # 1. tason otsikko (Pääotsikko)
    ## 2. tason otsikko
    ### 3. tason otsikko

### Kappaleet

Tekstikappale alkaa tyhjällä rivillä ja päättyy tyhjään riviin. Tämä sama sääntö pätee kaikkiin lohkoelementteihin.
Kappaleen teksti kirjoitetaan ilman rivinvaihtoja. Rivinvaihto tuottaa rivinvaihdon kappaleeseen. 
Kappale on kuitenkin samaa kappaletta niin kauan kunnes tulee tyhjä rivi, joka päättää kappaleen.

Jos kappaleen haluaa aloittaa erikoismerkillä tai merkkijonolla, joka aloittaa jonkin muun lohkoelementin 
esim. otsikko, niin ko. merkin eteen pitää laittaa \\-merkki.

### Tekstin korostaminen 

`**`**lihavoitu**`**` 

`*`*kursivoitu*`*`

### Taulukot

    |1|1|
    |-|-|
    |rivi 1: sarake 1|rivi 1: sarake 2|
    |rivi 2: sarake 1|rivi 2: sarake 2|

Taulukko alkaa tyhjällä rivillä ja päättyy tyhjään riviin. Taulukko koostuu otsikko- ja runko-osasta. Taulukon sarakkeet erotetaan |-merkillä. Taulukon rivit erotetaan rivinvaihdolla. Kaikissa riveissä (myös otsikko) pitää olla yhtä paljon sarakkeita. Otsikko-osa erotetaan rungosta rivillä, jonka soluissa on `-`-merkkejä esim. `|-|-|`. Otsikko-osaan merkitään kokonaisluvuilla sarakkeiden suhteelliset leveydet. Sarakkeiden suhteelliset leveydet a ja b tarkoittavat että sarakkeen a leveys on: 

    a / (a+b) * taulukon koko leveys

## Täydennettävät kentät

Asiakirja luodaan täydentämällä merkityt kohdat asiakirjamallista. 
Täydennetävät kohdat merkitään tunnuksella `{tiedon nimi}`. 
Käytettävissä olevat täydennettävät kohdat riippuvat asiakirjan lajista: 

- hakemusasiakirja
- päätösasiakirja

ja hakemustyypistä:

- avustushakemus
- 1\. maksatushakemus
- 2\. maksatushakemus
- ely hakemus

### Hakemusasiakirjat

#### Kaikki hakemukset

Kaikissa hakemuksissa on käytettävissä seuraavat täydennettävät kohdat:

|Nimi|Kuvaus|Esimerkki|
|-|-|-|
|vireillepvm|Hakemuksen lähetyspäivämäärä|18.12.2019|
|organisaatio-nimi|Hakemuksen lähettäjän organisaatio|Helsingin seudun liikenne|
|organisaatiolaji-pl-gen|Hakemuksen lähettäjän organisaatiolaji monikon genetiivi|suurten kaupunkiseutujen|
|lahettaja|Hakijan nimi, joka on lähettänyt hakemuksen|Mikko Esimerkki|
|vuosi|Hakemuskausi|2020|
|liitteet|Hakemuksen liitteiden nimet|esimerkki-liite1.pdf|

#### Avustuskohteet

Avustuskohteiden tiedot on käytettävissä hakemuksissa (ah0, mh1, mh2), joissa on avustuskohteet:

|Nimi|Kuvaus|Esimerkki|
|-|-|-|
| avustuskohteet | Avustuskohdetaulukko | |
| avustuskohteet-summary | Avustuskohteiden yhteenveto |
| haettuavustus | Hakemuksessa haettu avustus yhteensä | 2 480 000 |
| omarahoitus | Hakijan käyttämä oma rahoitus kohteisiin johon haettu avustusta | 2 980 000 |
| omarahoitus-all | Hakijan käyttämä oma rahoitus kaiken kaikkiaan| 2 980 000 |
| omarahoitus-all-selite | Selite omasta rahoituksesta, jos omaa rahoitusta on käytetty kohteisiin johon ei ole haettu avustusta | Yhteensä kaikkiin kohteisiin hakija on osoittanut omaa rahoitusta 3 000 000 euroa. |

#### Ely hakemus

|Nimi|Kuvaus|Esimerkki|
|-|-|-|
| haettuavustus | Hakemuksessa haettu avustus yhteensä | 3 502 230 |
| ostot | Liikenteen ostot | 100 000 |
| kaupunkilipputuki | | 100 000 |
| seutulipputuki | | 100 000 |
| kehittaminen | | 100 000 |
| kehityshankkeet | Taulukko kehityshankkeista | |
| maararahatarpeet | Taulukko määrärahatarpeista - joukkoliikennetuki liikenteen ostoihin | |