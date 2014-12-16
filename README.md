LiviJuku
========

Liikennevirasto Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä

## Git branching model
Tarkoituksena on käyttää Branching modelia: [http://nvie.com/posts/a-successful-git-branching-model/](http://nvie.com/posts/a-successful-git-branching-model/)

Kehitysympäristön käynnistäminen
--------------------------------

Asenna clojure, leiningen, vagrant.

Käynnistä [tietokanta](/tree/develop/juku-db/vagrant)

    cd juku-db/vagrant
    less README.md

Luo [skeema](/tree/develop/juku-db) tietokantaan

    cd juku-db
    less README.md

Käynnistä [backend-palvelu](/tree/develop/juku-backend)

    cd juku-backend
    less README.md


