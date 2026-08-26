# Plan — FKPOC-936: Komplettering för rimfrost-regel-rtf-manuell

## Bakgrund

`rimfrost-framework-regel-manuell` har kompletteringsstöd inbyggt i regelförfrågningshanteraren.
`RegelManuellServiceInterface` extendar `KompletteringKontrollInterface`, vilket ger alla
regelimplementationer en default-implementation av `checkKomplettering()` som returnerar en
tom lista (ingen komplettering).

Denna jira implementerar kompletteringsstödet i `rimfrost-regel-rtf-manuell`:

- Override av `checkKomplettering()` i `RtfService` med RTF-specifika villkor
- Komplettering REST-yta (GET data, PATCH svar, POST done)

Två repon berörs:

| Repo | Förändring |
|---|---|
| `rimfrost-regel-rtf-manuell-openapi` | Ny OpenAPI-typ: `RtfKompletteringData` |
| `rimfrost-regel-rtf-manuell` | Implementation, beroendebump, tester |

---

## Verifierade antaganden

| Antagande | Status |
|---|---|
| `KompletteringController<T,Y>` finns i `rimfrost-framework-regel` | ✅ Verifierat |
| `KompletteringSvarServiceInterface<T,Y>` finns i `rimfrost-framework-regel` | ✅ Verifierat |
| `individYrkandeRoller` finns på `Yrkande` med `Idtyp(typId, varde)` | ✅ Verifierat |
| `avsikt` finns på `Yrkande` som en `String` | ✅ Verifierat |
| `rimfrost-framework-regel` behöver explicit compile-beroende och `quarkus.index-dependency` | ✅ Tillagt i Step 2 |

---

## Steg

### Step 1 — Uppdatera OpenAPI-spec (`rimfrost-regel-rtf-manuell-openapi`)

Lägg till två nya scheman i specen. Releasea ny patch-version.

#### Design

**`RtfKompletteringData`** — returneras av GET, visar nuvarande yrkande-data:

```yaml
RtfKompletteringData:
  type: object
  properties:
    personnummer:
      type: string
      nullable: true
    avsikt:
      type: string
      nullable: true
```

**`RtfKompletteringData`** — body för PATCH, sökanden skickar korrigeringar:

```yaml
RtfKompletteringData:
  type: object
  properties:
    personnummer:
      type: string
      nullable: true
    avsikt:
      type: string
      nullable: true
```

---

### Step 2 — Beroendebump i `rimfrost-regel-rtf-manuell`

Uppdatera `pom.xml`:

- `rimfrost-framework-regel-manuell` → senaste version
- `rimfrost-regel-rtf-manuell-openapi-jaxrs-spec` → ny version från Step 1

Verifiera i `application.properties` att `rimfrost-framework-regel` finns med i
`quarkus.index-dependency` (för CDI-scanning av `KompletteringController`). Lägg till om
det saknas.

---

### Step 3 — Override `checkKomplettering()` i `RtfService` + tester

Lägg till override i `RtfService`. Villkor:

1. **Personnummer saknas** — ingen `IndividYrkandeRoll` har `individ().typId() == "personnummer"` med ett icke-tomt värde
2. **Avsikt saknas** — `avsikt` är null eller tom sträng

#### Design

```java
@Override
public List<KompletteringUnderlag> checkKomplettering(Handlaggning handlaggning) {
    var underlag = new ArrayList<KompletteringUnderlag>();
    var yrkande = handlaggning.yrkande();

    boolean harPersonnummer = yrkande.individYrkandeRoller().stream()
        .anyMatch(r -> "personnummer".equals(r.individ().typId())
                    && r.individ().varde() != null
                    && !r.individ().varde().isBlank());
    if (!harPersonnummer) {
        underlag.add(ImmutableKompletteringUnderlag.builder()
            .underlagTyp("personnummer")
            .beskrivning("Personnummer saknas på yrkandet")
            .build());
    }

    if (yrkande.avsikt() == null || yrkande.avsikt().isBlank()) {
        underlag.add(ImmutableKompletteringUnderlag.builder()
            .underlagTyp("avsikt")
            .beskrivning("Avsikt saknas på yrkandet")
            .build());
    }

    return underlag;
}
```

#### Tester

- Personnummer + avsikt komplett → tom lista
- Personnummer saknas → ett underlag med `underlagTyp="personnummer"`
- Avsikt saknas → ett underlag med `underlagTyp="avsikt"`
- Båda saknas → två underlag

---

### Step 4 — Implementera `RtfKompletteringSvarService` + tester

Ny klass i `logic/` som implementerar
`KompletteringSvarServiceInterface<RtfKompletteringData, RtfKompletteringData>`.

#### Design

```
se.fk.github.manuellregelratttillforsakring.logic.RtfKompletteringSvarService
  implements KompletteringSvarServiceInterface<RtfKompletteringData,
                                              RtfKompletteringData>
```

**`readSvarData(Handlaggning handlaggning): RtfKompletteringData`**
- Plocka ut personnummer från `individYrkandeRoller` (första träff med typId="personnummer")
- Hämta `avsikt` från yrkandet
- Bygg och returnera `RtfKompletteringData`

**`registerSvar(Handlaggning handlaggning, RtfKompletteringData request): HandlaggningUpdate`**
- Applicera uppgifterna från `request` på det nuvarande yrkandet:
  - Ersätt (eller lägg till) `IndividYrkandeRoll` med `typId="personnummer"` och
    `varde=request.getPersonnummer()`. Befintlig `yrkandeRollId` behålls om rollen
    redan finns, annars genereras ett nytt UUID.
  - Sätt `avsikt` på yrkandet via `withAvsikt(request.getAvsikt())`
- Bygg `HandlaggningUpdate` med `ImmutableHandlaggningUpdate.builder()` från handlaggningens
  egna fält (`id`, `version`, `skapadTS`, `avslutadTS`, `handlaggningspecifikationId`),
  det uppdaterade yrkandet, och tom underlagslista. `uppgift` lämnas null (`@Nullable`).

> **Design-beslut:** `ImmutableHandlaggningUpdate` och `ImmutableYrkande` har båda fullständigt
> Immutables-stöd. Uppdateringen görs via
> `ImmutableYrkande.copyOf(yrkande).withAvsikt(...).withIndividYrkandeRoller(...)`.
> `uppgift` och `processInstansId` är `@Nullable` i `HandlaggningUpdate` och sätts inte.

#### Tester

- `readSvarData()` — personnummer och avsikt mappas korrekt från handlaggning
- `registerSvar()` — personnummer och avsikt appliceras på yrkandet

---

### Step 5 — Implementera `RtfKompletteringController` + integrationstester

Ny klass i `presentation/rest/`. Alla tre endpoint-implementationer (GET, PATCH,
POST `/done`) tillhandahålls av `KompletteringController` i framework. Klassen
behöver bara ange sökväg och typparametrar — CDI löser upp `RtfKompletteringSvarService`
och `RtfService` automatiskt.

#### Design

```java
@Path("/regel/rtf-manuell")
@ApplicationScoped
public class RtfKompletteringController
      extends KompletteringController<RtfKompletteringData, RtfKompletteringData> {}
```

Exponerar (via framework-basklassen):

| Metod | Sökväg | Ansvarig |
|---|---|---|
| GET | `/regel/rtf-manuell/{handlaggningId}/komplettering` | Framework → `svarService.readSvarData()` |
| PATCH | `/regel/rtf-manuell/{handlaggningId}/komplettering` | Framework → `svarService.registerSvar()` |
| POST | `/regel/rtf-manuell/{handlaggningId}/komplettering/done` | Helt i framework |

#### Tester

Integrationstester som kör mot det kompletteringsflöde som triggas av Kafka:

- GET returnerar personnummer och avsikt för ett ärende i kompletteringstillstånd
- PATCH applicerar korrigeringar och returnerar 204
- POST /done avslutar kompletteringen och returnerar 204

---

## Avvikelser

_Fylls i löpande under implementation._
