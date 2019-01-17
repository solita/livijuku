Docker database
===============

Instructions how to set up an oracle docker database for development use.

Prerequisites
-------------

Install bash or compatible shell environment.

Install docker CE (see [docker-ce])

You need an account to [Oracle Container Registry][oracle-cr].

Create database
---------------

Login to Oracle Container Registry:

    docker login container-registry.oracle.com

Create database:

    ./create-juku-database.sh
    
Usage
-----

Start database:

    docker start juku-db
    
Stop database:

    docker stop juku-db

Destroy database:

    docker rm juku-db


[docker-ce]: https://docs.docker.com/install/
[oracle-cr]: https://container-registry.oracle.com/