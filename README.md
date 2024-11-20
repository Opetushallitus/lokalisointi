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

### Vaadittavat taustapalvelut

Postgresql ja localstack lähtevät käyntiin docker composella:

```shell
docker compose up
```

Käyttöliittymän kirjastot pitää asentaa ennen käynnistämistä:

```shell
cd ui
nvm use
npm install
cd ..
```

Ja itse sovellus komennolla:

```shell
mvn spring-boot:run
```

Avaa sovellus selaimessa: http://localhost:8080/lokalisointi/secured/index.html
