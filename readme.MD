# Spelpojken a Game Boy emulator

![Alt Text](https://github.com/adajoh/Spelpojken/blob/main/spelpojken.gif)

Detta är ett hobbyprojekt för att lära sig om Game Boy-hårdvara.

## Status - Blargg's Test ROMs
| Test | Status | Kommentar |
| :--- | :--- | :--- |
| `cpu_instrs.gb` | **PASS** | Alla instruktioner utför rätt logik. Verifierat med automatiserat test. |
| `instr_timing.gb` | **PASS?** | Timer-implementeringen har förbättrats med "falling edge"-detektering och cykel-synkronisering. |
| `mem_timing.gb` | **PASS?** | Minnesåtkomst är nu cykel-synkroniserad genom ett tick-system vid varje läsning/skrivning. |

## Planerade Förbättringar

### 1. Komplett ljudstöd (APU)
* [x] Implementera Kanal 3 (Wave) och Kanal 4 (Noise).
* [x] Stöd för stereo-panorering och mastervolym (NR50/51).

### 2. Cykel-exakt timing (Accuracy)
* [x] Refaktorera CPU för att hantera M-cykler (genom synkronisering vid minnesåtkomst).
* [x] Synkronisera Timer och GPU på M-cykel-nivå för att klara timing-tester.
* [x] Implementera korrekt `DIV`-räknare med "Falling Edge"-detektering för `TIMA`.

### 3. Stöd för Game Boy Color (CGB)
* Hantering av färgpaletter och CGB-specifika register.
* Stöd för flera VRAM- och WRAM-banker.
* Dubbel hastighet (Double Speed Mode).

### 4. Förbättrat MBC-stöd
* Real Time Clock (RTC) för MBC3 (krävs för Pokémon Gold/Silver).
* Fullständig implementation av MBC2.

### 5. Användarupplevelse (UX)
* Spara/Ladda SRAM till fil.
* Save States (Serialisering av emulatorns tillstånd).
* Skalning av fönster och enkla grafikfilter.

### 6. Prestanda
* [x] Implementera frame limiter (59.73 FPS) för korrekt hastighet.

## Tekniska Uppdateringar
* [x] Uppgraderat Gradle från 7.6 till 8.12 för bättre byggprestanda och kompatibilitet.
* [x] Rensat upp oanvända variabler, konstanter och metoder för att förbättra kodkvalitén.
* [x] Implementerat ett tick-baserat system för cykel-exakt synkronisering mellan CPU, Timer och GPU.
* [x] Förbättrad Timer-logik med Falling Edge-detektering.

---
*Status och framsteg loggas löpande i detta dokument.*
