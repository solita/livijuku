LiviJuku
========

Liikennevirasto Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä

## Versiointi malli

Versiontikäytännöt perustuvat malliin: [http://nvie.com/posts/a-successful-git-branching-model/](http://nvie.com/posts/a-successful-git-branching-model/)

Kehitysympäristön käynnistäminen
--------------------------------

Asenna clojure, leiningen, vagrant.

Käynnistä [tietokanta](/juku-db/vagrant)

    cd juku-db/vagrant
    less README.md

Luo [skeema](/juku-db) tietokantaan

    cd juku-db
    less README.md

Käynnistä [backend-palvelu](/juku-backend)

    cd juku-backend
    less README.md


