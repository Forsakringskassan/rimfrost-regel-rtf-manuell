# Krav — rimfrost-regel-rtf-manuell

## 1. Bakgrund och syfte

Tjänsten utför en manuell regelkontroll som avgör om en kund har rätt till VAH-försäkring (Vård av husdjur). När ett ärende kräver manuell handläggning initieras tjänsten av kundbehovsflödet via ett asynkront meddelande. En ansvarig handläggare tar sedan del av kundens uppgifter — folkbokföringsdata och anställningsinformation — och registrerar ett beslut per ersättningspost. När samtliga poster har bedömts och handläggaren markerar ärendet som klart skickas ett regelsvar tillbaka till det anropande systemet.

Lagstöd: Husdjursbalken, kap. 3, § 5, st. 1, p. 4 (gäller fr.o.m. 2010-02-11).

---

## 2. Intressenter och aktörer

| Aktör | Roll |
|---|---|
| Ansvarig handläggare | Utför den manuella kontrollen via handläggarportalen |
| Kundbehovsflödet | Initierar regelkontrollen och tar emot regelsvaret |
| Handläggningstjänsten | Förvaltar ärendets livscykel och lagrar beslut |
| OUL (Operativt Uppgiftslager) | Hanterar uppgiftskön och uppgiftsstatusar |
| Folkbokföringstjänsten | Levererar kundens personuppgifter |
| Arbetsgivartjänsten | Levererar kundens anställningsinformation |
| Sökanden | Registrerar kompletterande uppgifter via portalen |

---

## 3. Funktionella krav

### FR-01 — Ta emot regelförfrågan

- **FR-01.1** Tjänsten ska ta emot en regelförfrågan via ett asynkront meddelande och skapa ett nytt handläggningsärende.
- **FR-01.2** Förfrågan ska innehålla ett `kundbehovsflodeId` som identifierar det aktuella kundbehovsflödet.
- **FR-01.3** Förfrågan ska innehålla ett `correlationId` och ett `replyTo`-fält som används vid svar.

### FR-02 — Hämta handläggningsunderlag

- **FR-02.1** Tjänsten ska tillhandahålla ett REST-gränssnitt där handläggaren kan hämta det samlade underlaget för ett ärende.
- **FR-02.2** Underlaget ska innehålla kundens namn och kön från folkbokföringsregistret.
- **FR-02.3** Underlaget ska innehålla kundens anställningsinformation (arbetsgivare, arbetstid, anställningsperiod och lön).
- **FR-02.4** Underlaget ska innehålla samtliga ersättningsposter kopplade till ärendet.
- **FR-02.5** Om folkbokföringsdata saknas för kunden ska tjänsten returnera underlaget utan personuppgifter — ärendet ska inte avbrytas.

### FR-03 — Registrera beslut per ersättningspost

- **FR-03.1** Handläggaren ska kunna uppdatera beslutsutfallet för en eller flera ersättningsposter i ett och samma anrop.
- **FR-03.2** Tillåtna beslutsutfall är: `JA` (beviljat), `NEJ` (avslaget) och `FU` (förklaring utebehövs).
- **FR-03.3** Vid utfall `NEJ` eller `FU` ska handläggaren kunna ange en avslagsanledning.
- **FR-03.4** Tjänsten ska avvisa uppdateringar som refererar till ett okänt ersättnings-ID med statuskod 400.
- **FR-03.5** Tjänsten ska avvisa uppdateringar där beslutsutfallet saknas med statuskod 400.

### FR-04 — Avsluta manuell kontroll

- **FR-04.1** Handläggaren ska kunna markera en manuell kontroll som slutförd.
- **FR-04.2** När kontrollen avslutas ska ett regelsvar med utfall `JA` skickas tillbaka till det anropande systemet via det angivna `replyTo`-ämnet.
- **FR-04.3** Regelsvaret ska innehålla samma `correlationId` som den ursprungliga förfrågan.
- **FR-04.4** När kontrollen avslutas ska uppgiften i OUL markeras som avslutad.

### FR-06 — Komplettering

- **FR-06.1** Tjänsten ska identifiera om personnummer saknas bland `individYrkandeRoller` på yrkandet och returnera ett kompletteringsbehov.
- **FR-06.2** Tjänsten ska identifiera om `avsikt` saknas eller är tom på yrkandet och returnera ett kompletteringsbehov.
- **FR-06.3** Kompletteringsgränssnittet ska tillåta sökanden att korrigera personnummer och `avsikt`.

---

### FR-05 — Felhantering mot beroende tjänster

- **FR-05.1** Om folkbokföringstjänsten inte hittar kunden ska tjänsten fortsätta utan personuppgifter.
- **FR-05.2** Om folkbokföringstjänsten returnerar ett oväntat fel ska tjänsten returnera statuskod 500 till anroparen.
- **FR-05.3** Om arbetsgivartjänsten är otillgänglig ska tjänsten returnera statuskod 503 till anroparen.
- **FR-05.4** Om arbetsgivartjänsten returnerar ett oväntat fel ska tjänsten returnera statuskod 500 till anroparen.

---

## 4. Statusmodell

Varje handläggningsärende genomgår följande statusar hos OUL:

| Status | Benämning | Beskrivning |
|---|---|---|
| `NY` | Ny | Uppgiften har skapats men handläggning har ännu inte påbörjats |
| `1` | Redo för handläggning | Underlaget har hämtats och ärendet visas i handläggarportalen |
| `3` | Avslutad | Handläggaren har markerat kontrollen som klar |

---

## 5. Icke-funktionella krav

### NFR-01 — Tillförlitlighet

- **NFR-01.1** Tjänsten ska leverera ett korrekt regelsvar för varje mottagen regelförfrågan, även om enstaka anrop mot externa tjänster misslyckas och behöver göras om.
- **NFR-01.2** Optimistisk låsning ska användas för att förhindra att samtidiga uppdateringar av ersättningsposter skriver över varandra.

### NFR-02 — Spårbarhet

- **NFR-02.1** Varje inkommande Kafka-meddelande ska loggas med tillräcklig information för att spåra ett ärende från förfrågan till svar.
- **NFR-02.2** `correlationId` ska följa med i alla meddelanden och loggar som rör ett och samma ärende.

### NFR-03 — Underhållbarhet

- **NFR-03.1** Tjänstens regelspecifika logik ska vara tydligt separerad från ramverkslogiken i `rimfrost-framework-regel-manuell`.
- **NFR-03.2** Databasmigrationer ska hanteras via Flyway.

### NFR-04 — Driftsättbarhet

- **NFR-04.1** Tjänsten ska exponera ett hälsokontrollsgränssnitt som kan användas av plattformens liveness- och readiness-kontroller.
- **NFR-04.2** Känsliga konfigurationsvärden (databasanslutning) ska tillhandahållas via miljövariabler och inte hårdkodas.

---

## 6. API-gränssnitt (översikt)

| API | Målgrupp | Specifikationsartefakt |
|---|---|---|
| REST — manuell kontrolldatahämtning och beslut | Handläggarportalen | `rimfrost-regel-rtf-manuell-openapi` |
| REST — kompletteringsdata och svar | Sökanden / handläggarportalen | `rimfrost-regel-rtf-manuell-openapi` |
| Kafka — inkommande regelförfrågan | Kundbehovsflödet | `rimfrost-regel-rtf-manuell-asyncapi` |
| Kafka — utgående regelsvar | Kundbehovsflödet | `rimfrost-regel-rtf-manuell-asyncapi` |
| Kafka — uppgiftsstatus-notifikationer (konsument) | OUL | `rimfrost-operativt-uppgiftslager-asyncapi` |

---

## 7. Integration med kundbehovsflödet

Tjänsten är en del av kundbehovsflödets regelmotor och aktiveras när flödet avgör att ett manuellt beslut krävs. Kundbehovsflödet skickar en regelförfrågan och inväntar ett asynkront svar. Tjänsten är inte avsedd att anropas direkt av slutanvändare — all interaktion sker antingen via handläggarportalen (REST) eller via Kafka-meddelanden från det omgivande systemet.
