# Lokalisointipalvelu

* Java 21
* spring-boot
* spring-security

Lokalisointipalvelun roolit

* toimii Tolgee Cloudin julkaisemien lokalisointitiedostojen jakelijana Opintopolun "vanhassa" formaatissa
    * Tolgee julkaisee lokalisointitiedostot QA:n S3:een
* mahdollistaa lokalisointitiedostojen kopioinnin eri ympäristöjen välillä
* mahdollistaa ympäristökohtaisten "yliajojen" tallentamisen

## Ajaminen paikallisesti:

### PostgreSQL & localstack

Postgresql ja localstack lähtevät käyntiin docker composella:

```shell
docker compose up
```

Ja itse sovellus komennolla:

```shell
mvn spring-boot:run
```
