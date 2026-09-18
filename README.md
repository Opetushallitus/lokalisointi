# Lokalisointipalvelu

- Java 21
- spring-boot
- spring-security

Lokalisointipalvelun roolit

- toimii Tolgee Cloudin julkaisemien lokalisointitiedostojen jakelijana
  Opintopolun "vanhassa" formaatissa
  - Tolgee julkaisee lokalisointitiedostot QA:n S3:een
- Julkaisee myös Tolgee-formaatin mukaiset lokalisointitiedostot polussa
  /lokalisointi/tolgee/{slug}[/{namespace}]
  /{locale}.json
- mahdollistaa lokalisointitiedostojen kopioinnin eri ympäristöjen välillä
- mahdollistaa ympäristökohtaisten "yliajojen" tallentamisen

## Ajaminen paikallisesti

### Vaadittavat taustapalvelut

Postgresql ja localstack lähtevät käyntiin docker composella:

```shell
docker compose up
```

Käyttöliittymän kirjastot pitää asentaa ennen käynnistämistä. Tämä vaatii sen, että olet luonut Github Personal Access Tokenin (classic) - eli PAT token -, ja lisänyt sen `.npmrc`-tiedostoon:

1. [Luo PAT token näillä ohjeilla](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic)
   1. HUOM! Token tarvitseen ainoastaan `packages: read` oikeuden. Muista kopioda tokenin luonin jälkeen
2. Avaa `.npmrc`-tiedosto, joka löytyy löytyy sun kotikansiosta (Linux/Mac: `~/.npmrc`, Windows: `C:\Users\<username>\.npmrc`). Lisää nämä rivit tiedostoon:

```shell
@opetushallitus:registry=https://npm.pkg.github.com
//npm.pkg.github.com/:_authToken=<lisää tähän PAT tokenin>
```

Kirjastojen asentaminen:

```shell
cd ui
nvm use
pnpm install
cd ..
```

Ja itse sovellus komennolla:

```shell
mvn spring-boot:run
```

Avaa sovellus selaimessa: <http://localhost:8080/lokalisointi/secured/index.html>
