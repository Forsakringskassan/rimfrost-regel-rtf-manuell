# Teknisk specifikation — rimfrost-regel-rtf-manuell

## 1. Översikt

Quarkus 3.x-tjänst (Java 21) som implementerar en manuell regelkontroll för rätt till VAH-försäkring. Tjänsten exponerar ett REST-API (2 endpoints) för handläggarportalen och kommunicerar asynkront med omgivande system via Kafka (1 inkommande + 1 utgående topic). Tillstånd lagras i PostgreSQL via Panache JPA, med Flyway för schemamigrationer.

---

## 2. Komponentstruktur

```
se.fk.github.manuellregelratttillforsakring
├── RtfManuellController      # JAX-RS-kontroller, delegerar till framework-basklass
├── logic/
│   ├── RtfService            # Affärslogik: readData, updateData, done
│   ├── RtfMapper             # Mappning mellan domänmodell och DTO
│   └── RegelManuellMiddlewareServiceImpl  # Framework-integration (middleware)
└── resources/
    ├── application.properties  # Quarkus- och Kafka-konfiguration
    └── config.yaml             # Regelspecifikation, uppgifts- och lagrumsdefinition
```

Ramverkslogik (meddelanden, persistens, OUL-integration) hanteras av `rimfrost-framework-regel-manuell`.

---

## 3. API-specifikationer

### REST — rimfrost-regel-rtf-manuell-openapi

Baspath: `/regel/rtf-manuell`

Fullständiga scheman definieras i spec-repot `rimfrost-regel-rtf-manuell-openapi`.

| Metod | Sökväg | Beskrivning |
|---|---|---|
| `GET` | `/{handlaggningId}` | Hämtar handläggningsunderlag (kund, anställning, ersättningsposter) |
| `PATCH` | `/{handlaggningId}/ersattning/{ersattningId}` | Uppdaterar beslutsutfall och avslagsanledning per ersättningspost |
| `POST` | `/{handlaggningId}` | Avslutar manuell kontroll och triggar regelsvarsmeddelande |

> POST `/done` hanteras av ramverkets basklass och är inte definierad i tjänstens egna OpenAPI-spec.

---

## 4. Kafka-integration

Meddelandescheman definieras i spec-repot `rimfrost-regel-rtf-manuell-asyncapi`.

| Riktning | Topic | Trigger |
|---|---|---|
| Inkommande | `rtf-manuell-requests` | Nytt ärende initieras av kundbehovsflödet |
| Utgående | `rtf-manuell-responses` | Handläggaren avslutar kontrollen (POST done) |
| Inkommande | `operativt-uppgiftslager-status-notification.rtf-manuell` | Statusuppdatering från OUL |

Svarsrouting sker dynamiskt: `replyTo`-headern i inkommande meddelande anger vilket topic regelsvaret ska publiceras till. `correlationId` kopplar samman förfrågan och svar.

---

## 5. Konfiguration

| Nyckel | Beskrivning | Standardvärde |
|---|---|---|
| `mp.messaging.incoming.regel-requests.topic` | Inkommande Kafka-topic | `rtf-manuell-requests` |
| `mp.messaging.outgoing.regel-responses.topic` | Utgående Kafka-topic | `rtf-manuell-responses` |
| `mp.messaging.incoming.operativt-uppgiftslager-status-notification.topic` | OUL-statustopic | `operativt-uppgiftslager-status-notification.rtf-manuell` |
| `folkbokford.api.base-url` | Bas-URL till folkbokföringstjänsten | `http://rimfrost-k8s-folkbokford:8080` |
| `arbetsgivare.api.base-url` | Bas-URL till arbetsgivartjänsten | `http://rimfrost-k8s-arbetsgivare:8080` |
| `oul.api.base-url` | Bas-URL till OUL | `http://rimfrost-operativt-uppgiftslager:8080` |
| `quarkus.flyway.default-schema` | Databasschema | `regel_rtf_manuell` |
| `regel.persistence.table-prefix` | Prefix för databastabeller | `rtf_manuell` |
| `%prod.quarkus.datasource.username` | Databasanvändare (prod) | `${DB_USERNAME}` |
| `%prod.quarkus.datasource.password` | Databaslösenord (prod) | `${DB_PASSWORD}` |
| `%prod.quarkus.datasource.jdbc.url` | JDBC-URL (prod) | `${DB_URL}` |

---

## 6. Liveness

Quarkus SmallRye Health exponerar hälsokontrollsgränssnitt på standardsökvägar:

- Liveness: `GET /q/health/live`
- Readiness: `GET /q/health/ready`

---

## 7. Kända begränsningar och framtida arbete

| Begränsning | Föreslagen åtgärd |
|---|---|
| Regelsvaret skickas alltid med utfall `JA`, oavsett enskilda ersättningsposter med utfall `NEJ` | Implementera aggregeringslogik som härleder det sammantagna utfallet ur ersättningsposternas beslutsutfall |
| Folkbokföringsdata som saknas resulterar i tomt kundavsnitt utan indikation i svaret | Lägg till ett fält i svaret som signalerar att personuppgifter saknas |
| POST `/done` kan anropas utan att alla ersättningsposter har fått ett beslut | Validera att samtliga poster har ett beslutsutfall innan avslut tillåts |
