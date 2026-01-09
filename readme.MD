# Spelpojken a Game Boy emulator

![Alt Text](https://github.com/adajoh/Spelpojken/blob/main/spelpojken.gif)

Detta är ett hobbyprojekt för att lära sig om Game Boy-hårdvara.

## Status - Blargg's Test ROMs
| Test | Status | Kommentar |
| :--- | :--- | :--- |
| `cpu_instrs.gb` | **PASS** | Alla instruktioner utför rätt logik. |
| `instr_timing.gb` | **FAIL** | Timer-implementeringen är inte tillräckligt exakt. |
| `mem_timing.gb` | **FAIL** | Minnesåtkomst är inte cykel-synkroniserad. |

## Planerade Förbättringar

### 1. Komplett ljudstöd (APU)
* [x] Implementera Kanal 3 (Wave) och Kanal 4 (Noise).
* [x] Stöd för stereo-panorering och mastervolym (NR50/51).

### 2. Cykel-exakt timing (Accuracy)
* Refaktorera CPU för att hantera M-cykler (4 T-cykler per steg).
* Synkronisera Timer och GPU på M-cykel-nivå för att klara timing-tester.
* Implementera korrekt `DIV`-räknare med "Falling Edge"-detektering för `TIMA`.

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

---
*Status och framsteg loggas löpande i detta dokument.*
