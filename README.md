# Lokalisointipalvelu

* Java 21
* spring-boot
* spring-security

## Ajaminen paikallisesti:

### PostgreSQL

Kontin luonti käy komennolla:

```shell
cd scripts/postgres-docker
docker build -t lokalisointi-postgres .
docker volume create lokalisointi_pgdata
```

Kontin ajaminen onnistuu komennolla:

```shell
docker run --rm --name lokalisointi-postgres -p 5432:5432 --volume lokalisointi_pgdata:/var/lib/postgresql/data -e POSTGRES_PASSWORD=lokalisointi lokalisointi-postgres
```

```shell
mvn spring-boot:run
```
