# rimfrost-regel-rtf-manuell changelog

Changelog of rimfrost-regel-rtf-manuell.

## 0.4.0 (2026-02-24)

### Features

-  use framework.regel.manuell (#43) ([9e36e](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/9e36e8753f64330) NilsElveros)  
-  send reply to header in kafkamessage to OUL (#41) ([d4f2c](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/d4f2c11d36f12ad) NilsElveros)  
-  add more data to kundebehovsflode PUT (#33) ([71489](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/71489c0e953b02c) NilsElveros)  

### Bug Fixes

-  Bump rimfrost-framework-regel-manuell version ([1f364](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/1f36432a13fb35f) Lars Persson)  
-  Bump rimfrost-framework-regel-manuell version ([ec891](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/ec8911b39c16dff) Lars Persson)  
-  **deps**  update dependency se.fk.rimfrost.framework.arbetsgivare:rimfrost-framework-arbetsgivare-adapter to v0.1.0 ([46bbb](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/46bbbad4bf3bcb4) renovate[bot])  
-  Use getRegelData method from CommonRegelData ([fbf01](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/fbf0170873ad481) Lars Persson)  
-  Use rimfrost-framework-storage ([680b7](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/680b78e3984b1f0) Lars Persson)  
-  **deps**  update dependency se.fk.rimfrost.framework.regel.manuell:rimfrost-framework-regel-manuell to v0.1.10 (#48) ([1a455](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/1a455a3f1728b18) renovate[bot])  
-  Use correct kubernetes urls for services ([524cb](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/524cb715c15b607) Lars Persson)  
-  Bump rimfrost-framework-regel-manuell version ([be3ee](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/be3ee1743e59339) Lars Persson)  
-  Use adapters from framework ([4cfe7](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/4cfe73a1f6d9dbb) Lars Persson)  
-  Re-add missing folkbokford and arbetsgivare api base-url ([395d3](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/395d3aff59d483e) Lars Persson)  
-  lägger till decideUtfall och anpassning mot nytt framework ([84a11](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/84a1162336bc0d4) Ulf Slunga)  
-  **deps**  update dependency se.fk.rimfrost.framework.regel:rimfrost-framework-regel to v0.1.3 ([1c644](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/1c6444690bd234d) renovate[bot])  
-  **deps**  update dependency se.fk.rimfrost.framework.regel:rimfrost-framework-regel to v0.1.2 ([79405](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/794050b2b3645ea) renovate[bot])  
-  Refactor test to use smallrye in-memory and test resources ([35d17](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/35d179eebb5737a) Lars Persson)  
-  Use OulController from framework-oul ([01b9a](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/01b9a9bb0645228) Lars Persson)  
-  Remove dependencies on common ([c8292](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/c829231a29927a2) Lars Persson)  
-  Use kundbehovsflode api from framework ([08f74](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/08f743187386b9c) Lars Persson)  
-  RegelResponse type and source is now correct (#36) ([ca1ae](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/ca1ae1ad16d0c9b) NilsElveros)  
-  add type to RegelResponse (#35) ([94f78](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/94f78324b2e986b) NilsElveros)  
-  Refactor PATCH endpoint into two separate endpoints ([93611](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/9361134ba22a041) Lars Persson)  
-  **deps**  update dependency se.fk.rimfrost:rimfrost-service-oul-asyncapi to v0.4.1 ([80478](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/80478b0b8e36210) renovate[bot])  
-  använder OUL från rimfrost-common ([f782d](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/f782d0c30687256) Ulf Slunga)  
-  använd producer för regel-responses från rimfrost-common ([bb75f](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/bb75fa5bb712d12) Ulf Slunga)  
-  Flytta RegelRequestDeserializer till common ([2cec9](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/2cec991ef4f9336) Ulf Slunga)  
-  använd regel message dto från rimfrost-common ([f22c2](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/f22c20d4aa1c9a3) Ulf Slunga)  

### Other changes

**Feat/use regel and oul framework (#37)**

* feat: use oul and regel framework instead of common 
* spotless 
* fix: smoketest 

[c4591](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/c4591c8be22cb4c) NilsElveros *2026-02-03 07:40:18*

**bumpar version av rimfrost-common**


[0acaa](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/0acaaa2d32d56f9) Ulf Slunga *2026-01-21 13:12:19*


## 0.3.0 (2026-01-15)

### Features

-  add healthcheck (#28) ([34673](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/346739b4844a3b3) NilsElveros)  
-  use new version of oul async api 0.4.0 (#24) ([d2a27](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/d2a27b07c3d3427) NilsElveros)  

### Bug Fixes

-  **deps**  update dependency org.yaml:snakeyaml to v2.5 ([0c448](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/0c448ad889665fb) renovate[bot])  
-  config loaded from classpath (#27) ([1255b](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/1255b3f88449acf) NilsElveros)  
-  Läser regel-information från config.yaml (#25) ([276d3](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/276d34db3132120) Ulf Slunga)  

## 0.2.3 (2026-01-05)

### Bug Fixes

-  Container test för komplett regel-sekvens happy-path ([69947](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/69947d9eea8109d) Ulf Slunga)  

## 0.2.2 (2025-12-23)

### Bug Fixes

-  spotless apply ([8fadd](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/8fadd8ab4814f2d) Ulf Slunga)  
-  regel svarar 404 på regel-custom-url ([3c8de](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/3c8dee29a92e705) Ulf Slunga)  
-  **deps**  update dependency se.fk.rimfrost.regel.rtf.manuell:rimfrost-regel-rtf-manuell-openapi-jaxrs-spec to v0.1.5 ([a3ca2](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/a3ca28143994f52) renovate[bot])  

## 0.2.1 (2025-12-19)

### Bug Fixes

-  spotless apply ([a1fdd](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/a1fdd033ffd4dd1) Ulf Slunga)  
-  Hantera 404 för folkbokford & arbetsgivare ([90438](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/90438ee789a3a62) Ulf Slunga)  

## 0.2.0 (2025-12-17)

### Features

-  Update to use new PUT in kundbehovsflode (#18) ([c3c77](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/c3c774c2786d296) NilsElveros)  
-  call kundbehovs api with a PUT after model is updated (#10) ([da183](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/da183f1ed691b55) NilsElveros)  

### Bug Fixes

-  racecondition when status message received before response (#16) ([b4c8e](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/b4c8e9e79ed6e8b) NilsElveros)  
-  **deps**  update dependency se.fk.rimfrost.api.arbetsgivare:rimfrost-arbetsgivare-api-jaxrs-spec to v1.1.1 (#11) ([97b41](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/97b41f73d2373f4) renovate[bot])  
-  integration between kundeflode and oul (#15) ([d948f](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/d948f48fd81d71f) NilsElveros)  
-  resolved two null issues (#14) ([bb73e](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/bb73edc6f6e3b10) NilsElveros)  
-  incorrect incoming topic (#13) ([38bd0](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/38bd0dd86aec33b) NilsElveros)  

## 0.1.1 (2025-12-04)

### Bug Fixes

-  configure kafka messages and deserialzers (#12) ([b28ab](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/b28ab21b9cdaf64) NilsElveros)  

## 0.1.0 (2025-11-28)

### Features

-  add sign option to patch and send kafka messages once rule is signed (#9) ([9c334](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/9c334bf8cec5cdd) NilsElveros)  
-  implement patch operation (#8) ([9a997](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/9a9974548ee684b) NilsElveros)  
-  add GET endpoint ([41691](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/4169146cd194af3) Nils Elveros)  

### Bug Fixes

-  compilation issue after version upgrade ([ee2ce](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/ee2cec12fc92eb1) Nils Elveros)  
-  spotless apply ([c43dc](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/c43dc2ccfa431fd) Nils Elveros)  
-  spotless apply ([3dd7a](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/3dd7a5e809fcc83) Nils Elveros)  
-  personNummer instead of personnummer ([ad220](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/ad220dbc303c641) Nils Elveros)  
-  integration between rtfmanuell and operativtuppgiftslager ([d0854](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/d08546fa60b3e80) Nils Elveros)  
-  channels/topics according to spec ([7c27a](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/7c27aaf730d0a68) rikrhen)  
-  builds properly. ([591c9](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/591c948f42d8559) rikrhen)  
-  spotless cleaning ([ddadd](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/ddadd4721a2e1b4) rikrhen)  
-  Cleanup, successfully builds, added Deserializer for RTF <- OUL ([8eb13](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/8eb13a26248f943) rikrhen)  

### Other changes

**Update pom.xml**

* Co-authored-by: davidsoderberg-ductus &lt;david.soderberg@ductus.se&gt; 

[65894](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/658943dc855120a) NilsElveros *2025-11-26 09:10:59*

**Added cloudevents**


[6bbc2](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/6bbc2d8e1ac5421) Nils Elveros *2025-11-03 10:28:35*


## 0.0.1 (2025-10-29)

### Features

-  refactor ([ac844](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/ac844337912a10d) Tomas Bjerre)  
-  operationId ([3fa93](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/3fa930358b1910e) Tomas Bjerre)  
-  stegar API och Docker ([e01d6](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/e01d60f1b1dadad) Tomas Bjerre)  
-  use Spotless plugin with code standard from jar ([d9046](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/d9046c2b6e81fb0) Tomas Bjerre)  
-  publish till gemensamt repo ([ffe1b](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/ffe1b02eed2716e) Tomas Bjerre)  
-  publicerar till gemensamt repository ([5ba77](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/5ba77e7ce9b3acb) Tomas Bjerre)  
-  parent ([92b63](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/92b63d6dac54057) Tomas Bjerre)  
-  parent ([6b9ac](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/6b9ac72b380f482) Tomas Bjerre)  

### Bug Fixes

-  removing plugin maven-compile-plugin which is inherited from parent ([3a3c0](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/3a3c0b84d926779) Ulf Slunga)  
-  using java 21 ([f2e38](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/f2e38bd4a911960) Ulf Slunga)  
-  removing not required dependencies ([55c7a](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/55c7a869ae28545) Ulf Slunga)  
-  serializer please serialize ([a9b2b](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/a9b2ba9611e5b6f) rikrhen)  
-  spotless ([e6199](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/e61991640e825f6) rikrhen)  
-  deserializer please deserialize ([e6612](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/e6612130718b5a0) rikrhen)  
-  request -> requests ([8e2ba](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/8e2baae60b75e06) rikrhen)  
-  smoother deserialization ([aa080](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/aa0805b942781e8) rikrhen)  
-  pointing to correct deserializer ([4df40](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/4df402d99140da9) rikrhen)  
-  added deserializer ([1b9b0](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/1b9b07fb210f711) rikrhen)  
-  Plural for all topic names in incoming and outgoing annotations ([7d8b0](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/7d8b0b0a746d025) Ulf Slunga)  
-  Plural for all topic names ([fc32f](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/fc32f9a3baffeae) Ulf Slunga)  
-  implementing Swedish naming scheme, sorted ([36cfc](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/36cfc2b32cd75d0) rikrhen)  
-  implementing Swedish naming scheme ([5ee9c](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/5ee9cb8a87a18f6) rikrhen)  
-  **deps**  update dependency se.fk.maven:fk-maven-quarkus-parent to v1.10.1 ([cf4c1](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/cf4c1e89d865a9c) renovate[bot])  
-  **deps**  update dependency se.fk.github.jaxrsclientfactory:jaxrs-client-factory to v1.1.1 ([22cb7](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/22cb7cfdf17fdc9) renovate[bot])  
-  **deps**  update dependency se.fk.gradle.examples:example-jaxrs-spec to v1.10.1 ([8cf6a](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/8cf6a740621f22b) renovate[bot])  
-  small correction for the folkbokford integration to work (#7) ([151a4](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/151a46d44c2600f) NilsElveros)  
-  change pom.xml artifactId ([07f34](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/07f3450af228c58) David Söderberg)  
-  **deps**  update dependency org.immutables:value to v2.11.6 ([e0f73](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/e0f73ebfede4a5b) renovate[bot])  
-  **deps**  update quarkus.platform.version to v3.28.3 ([63399](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/63399b2784c0947) renovate[bot])  

### Dependency updates

- add renovate.json ([0814f](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/0814fb3ccde5d3e) renovate[bot])  
- update dependency org.apache.maven.plugins:maven-compiler-plugin to v3.14.1 ([41ac0](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/41ac0d364cf6123) renovate[bot])  
- fk-maven ([ce078](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/ce078ccdc0d9179) Tomas Bjerre)  
- update dependency org.apache.maven.plugins:maven-compiler-plugin to v3.14.1 ([e1c97](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/e1c9733a48854e2) renovate[bot])  
### Other changes

**final old repo remnant commented out?**


[517d5](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/517d54fa7262390) rikrhen *2025-10-29 13:11:39*

**new response/request interfaces. Spotless cleaning.**


[90bd6](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/90bd692563a8ab1) rikrhen *2025-10-29 12:54:06*

**Code commented out, immutables built**


[3a3c0](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/3a3c0b2f4026e6f) rikrhen *2025-10-29 12:42:41*

**init commit to get new names built**


[adfb6](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/adfb6dcb8fadd5f) rikrhen *2025-10-29 12:32:43*

**Merge branch 'main' into fix/get-full-flow-working**


[93865](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/938653d8db529f7) rikrhen *2025-10-28 10:22:16*

**Merge branch 'main' into fix/get-full-flow-working**


[db707](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/db7072223648a77) rikrhen *2025-10-27 14:37:43*

**Changing from singular to plural forms.**


[0cc9e](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/0cc9ec679f0eb33) rikrhen *2025-10-27 13:35:00*

**application.properties changes to hopefully fix pod.**


[1983c](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/1983ca1404c4b3e) rikrhen *2025-10-27 12:53:57*

**No fancy stuff**


[ae67e](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/ae67e964be39a9e) rikrhen *2025-10-24 08:55:38*

**Adding kubernetes value to application.properties base URL**


[753db](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/753dbc753500f5a) rikrhen *2025-10-24 08:46:20*

**API base url sorted**


[b32df](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/b32df3427f59beb) rikrhen *2025-10-23 10:42:52*

**Delete src/main/java/presentation/FolkbokfordController.java**

* Used for testing, irrelevant for final version of branch 

[9a3ec](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/9a3ec03542f47c5) Riki Rhen *2025-10-23 07:16:12*

**Cleanup**


[487d4](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/487d43e545146e3) rikrhen *2025-10-22 13:48:03*

**controller fix**


[a5255](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/a52555ca73a126b) rikrhen *2025-10-22 13:36:21*

**Post comment stuff**


[c0700](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/c07004da12f6a78) rikrhen *2025-10-22 13:33:22*

**Spotless cleaning**


[c36b5](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/c36b520a931addb) rikrhen *2025-10-22 10:54:07*

**Post comment changes - api call from generated specs**


[94bed](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/94bed0909605381) rikrhen *2025-10-22 10:52:39*

**Update application.properties**


[15788](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/15788933e4773eb) Riki Rhen *2025-10-22 10:19:49*

**Update PresentationVahRtfResponse.java**

* last old comment 

[85e1b](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/85e1b33e71f7460) Riki Rhen *2025-10-22 09:05:25*

**Update PresentationVahRtfRequest.java**

* more old comments 

[23287](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/23287f2cdae6b04) Riki Rhen *2025-10-22 09:05:01*

**Update FolkbokfordService.java**

* Cleanup of old comments 

[f1581](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/f1581bab8491501) Riki Rhen *2025-10-22 09:04:31*

**Delete src/main/java/logic/Folkbokford.java**

* Delete old unused interface 

[cf972](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/cf972d9eb44c0b5) Riki Rhen *2025-10-22 09:03:15*

**Update pom.xml**

* fixed double dependency in POM 

[4874e](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/4874eecd6b208df) Riki Rhen *2025-10-22 08:59:59*

**api chain implementation**


[7077c](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/7077c76f14bb7c4) rikrhen *2025-10-22 08:55:25*

**Spotless cleaning**


[dea02](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/dea0276ef23eb05) rikrhen *2025-10-20 08:36:33*

**REST API call**


[421e2](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/421e2451a16671c) rikrhen *2025-10-20 08:01:19*

**Merge branch 'feature/parent'**


[8c9a8](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/8c9a89e55dcf2fd) Tomas Bjerre *2025-10-09 16:09:20*

**first commit**


[18d4a](https://github.com/Forsakringskassan/rimfrost-regel-rtf-manuell/commit/18d4ab1d6d92ad5) Tomas Bjerre *2025-10-09 10:47:31*


