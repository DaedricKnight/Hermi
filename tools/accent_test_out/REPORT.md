# Accent & Intonation Stress Test — Report (2026-07-25)

Harness: `tools/accent_stress_test.py` — 22 phrases (19 cabin commands + 3
out-of-domain) × 7 macOS TTS accents (US/GB/AU/IE/IN/ZA + aged-GB) × 4 conditions
(140/180/220 wpm, plus 180 wpm with white noise at SNR 10 dB) = 616 clips through
the exact production pipeline: Vosk (`vosk-model-small-en-us-0.15`) → the
WordPiece tokenizer ported from `ONNXAIProcessor` → the 110-label
`automotive_intent_quant.onnx` → CommandMapper's **0.95** confidence gate.

Verdicts: **PASS** (correct command above the gate, or rejection for an
out-of-domain phrase), **REJECTED** (below the gate — safe, the app answers
"I don't know how to do that"), **WRONG_INTENT** (confident wrong command —
dangerous, the app acts on it).

Every row also carries the *text baseline*: the written phrase straight into the
classifier, so a model error is never mistaken for an accent error.

## Headline results

| | Count | Share |
|---|---|---|
| Commands recognized | 371/532 | **70%** |
| Commands safely rejected | 112 | 21% |
| Commands wrongly executed | 49 | 9% |
| Off-topic phrases kept safe | 38/84 | 45% |

Split by condition, the picture is much sharper:

| Commands | Pass |
|---|---|
| clean speech (140/180/220 wpm) | 320/399 (**80%**) |
| 180 wpm + white noise, SNR 10 dB | 51/133 (**38%**) |

**Noise is what breaks this pipeline.** Speech rate barely matters (77% / 74% /
75% across 140-220 wpm); adding noise halves the pass rate and doubles the
dangerous failures.

## Per-accent pass rate (commands only)

| Accent | Voice | Pass | Rejected | Wrong |
|---|---|---|---|---|
| IE | Moira | **86%** | 7 | 4 |
| US | Samantha | 80% | 10 | 5 |
| AU | Karen | 76% | 11 | 7 |
| GB | Daniel | 75% | 14 | 5 |
| ZA | Tessa | 75% | 16 | 3 |
| IN | Rishi | 66% | 22 | 4 |
| GB-aged | Grandma (UK) | **30%** | 32 | 21 |

The aged voice is the outlier by a wide margin — 21 of the 49 dangerous failures
across the whole grid come from it. Any deployment expecting older drivers needs
a stronger recognizer than `vosk-model-small`.

## Weakest phrases

| Phrase | Pass | What goes wrong |
|---|---|---|
| `fold the mirrors` | 12/28 (43%) | Vosk hears "for/build/though the mirrors" → **`body_mirrors_unfold`** at 0.99 — the opposite action |
| `turn on the seat heating` | 0/28 | classifies correctly (`heating_seats_driver_on`) but at **0.927**, just under the gate → always rejected |
| `answer the call` | 16/28 (57%) | "the sort of the call" → `phone_end_call` |
| `cancel the route` | 18/28 (64%) | "cancel the roots/routes" survives, noise does not |
| `open the trunk` | 18/28 (64%) | "open the term/trying" → `phone_end_call`, `media_source_bluetooth`, `body_door_unlock` |

Two patterns worth designing around:

- **Antonym pairs are one phoneme apart.** fold/unfold, open/close trunk,
  headlights/high beams. A garbled verb does not fail safe — it executes the
  opposite command with high confidence. These pairs deserve either confirmation
  before acting or ASR-confusion augmentation ("for the mirrors" →
  `body_mirrors_fold`).
- **A correct prediction can still sit under the gate.** "turn on the seat
  heating" peaks at 0.927 because the model splits driver/passenger/rear seat
  heating; the probability mass is divided between siblings. Merging or
  re-training those three, or accepting the top-2 sum when the siblings agree,
  recovers this class.

## Out-of-domain: the model has no `unknown` class

| Phrase | Kept safe | What it fires |
|---|---|---|
| `remind me to buy milk` | 28/28 | — |
| `tell me a joke about penguins` | 10/28 | `navi_unmute_guidance` at 0.969 |
| `what time is the meeting tomorrow` | 0/28 | `navi_go_work` at **1.000** |

On the text baseline (no audio at all), 25 everyday non-car phrases go through
the classifier: **16 are rejected by the gate, 9 fire a real command** — "order a
pizza" → find restaurant, "what is your name" → zoom in, "book a table for two" →
dial number, "sing me a song" → play. The gate cannot fix this: the model assigns
1.000 to `navi_go_work` for a calendar question.

The 110-label set has no `none`/`unknown` class, so out-of-domain speech has
nowhere to go. **This is the top item to fix on the model side** — train a `none`
class from generic phone/media/small-talk utterances, the way the medical model
was fixed. `ONNXAIProcessorIntegrationTest.outOfDomainSpeechIsMostlyRejected`
measures the rate on device (currently 16/25) and fails if it regresses.

## Gate choice

| Gate | Phrase bank (4296 trained phrases) | Off-topic rejected (25 phrases) |
|---|---|---|
| 0.70 | 100% | 14/25 |
| 0.90 | 100% | 14/25 |
| **0.95 (shipped)** | **100%** | **16/25** |
| 0.99 | 99.5% | 19/25 |

0.95 is free in-domain and buys two more rejections; 0.99 starts dropping real
commands. The cost shows up on real audio instead: 112 of 532 command clips land
below 0.95 and are rejected — a safe failure (the app re-prompts) but a usability
one.

## Duplicate labels

Eight label pairs mean the same user action and their phrase banks overlap
outright ("Stop navigation" sits in `navi_cancel_route`, "Cancel navigation" in
`navigation_stop_route`). They split probability mass between siblings and can
push a correct prediction below the gate. The app collapses them onto one command
each in `CommandCatalog` (110 labels → 102 commands); the model should merge them
at training time.

## Instrumented counterpart

`shared/src/androidTest/.../VoicePipelineTest.kt` runs 21 bundled clips (7
accents, slow/fast delivery, a synonym probe, an out-of-domain probe) through the
same chain on a real device — accent sweep 20/20 on
`Automotive_1408p_landscape` (API 34), phrase bank 1020/1020. That is the
regression gate; this stand is the exploration.

## Reproduce

```
python3 tools/accent_stress_test.py           # full 616-clip grid (~35 min)
python3 tools/accent_stress_test.py --quick   # 3 accents x 2 conditions
./tools/generate_test_wavs.sh                 # regenerate androidTest fixtures
./gradlew :shared:connectedDebugAndroidTest -Pandroid.injected.build.abi=arm64-v8a
```

Raw data: `results.csv` (one row per clip, with transcript, prediction,
confidence and the text baseline).
