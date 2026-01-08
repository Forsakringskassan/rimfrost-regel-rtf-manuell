# Rimfrost regel - Rätt till försäkring

En regel som körs med quarkus.

Bygg och testa:  `./mvnw -s settings.xml clean verify`.

## Bygg docker image lokalt 

`./mvnw -s settings.xml clean package`

## Github workflow

A GitHub workflow will also create a Docker image, it is published to [repository](https://github.com/Forsakringskassan/repository). It can be started with:

```sh
docker run -d \
  -p 8080:8080 \
  ghcr.io/forsakringskassan/template-quarkus-app:snapshot
```

See also: [fk-maven](https://github.com/Forsakringskassan/fk-maven).

## Konfiguration av regel via yaml

`src/main/resources/config.yaml` <br>
innehåller konfiguration av regel-attribut som t.ex. namn, uppgiftsbeskrivning.

Environment-variabel `REGEL_CONFIG_PATH` kan användas för att peka ut custom config-fil.
`src/main/resources/config.yaml` kommer att paketeras i docker imagen och användas by default.
