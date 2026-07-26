# Accent & Intonation Stress Test — Report (2026-07-26)

Harness: `tools/accent_stress_test.py` — 22 phrases (19 cabin commands + 3
out-of-domain) × 7 macOS TTS accents (US/GB/AU/IE/IN/ZA + aged-GB) × 5 conditions
(140/180/220 wpm clean, plus 180 wpm with road noise and with white noise, both
at SNR 10 dB) = 770 clips through the exact production pipeline: Vosk with the
command grammar → the WordPiece tokenizer ported from `ONNXAIProcessor` → the
111-label `automotive_intent_quant.onnx` (v2.2) → CommandMapper's 0.95 gate.

Verdicts: **PASS** (correct command above the gate, or a refusal for an
out-of-domain phrase), **REJECTED** (below the gate or `none` — safe, the app
says it did not understand), **WRONG_INTENT** (confident wrong command —
dangerous, the app acts on it).

Every row also carries the *text baseline* — the written phrase straight into the
classifier — so a model error is never mistaken for an accent error.

## Where it landed

| | Commands | Rejected (safe) | Wrong (dangerous) | Off-topic kept safe |
|---|---|---|---|---|
| v1 — 110 labels, open vocabulary | 371/532 (70%) | 112 | 49 | 38/84 (45%) |
| v2 — `none` class + grammar | 490/532 (92%) | 29 | 12 | 33/84 (39%) |
| v2.1 — + ASR augmentation, `none` in grammar | 637/665 (96%) | 14 | 13 | 104/105 (99%) |
| **v2.2 — + second augmentation round** | **645/665 (97%)** | **10** | **9** | **105/105 (100%)** |

Overall 750/770 (97.4%).

| Condition | Commands | Wrong |
|---|---|---|
| 140 wpm | 131/133 (98%) | 0 |
| 180 wpm | 132/133 (99%) | 0 |
| 220 wpm | 132/133 (99%) | 0 |
| **180 wpm + road noise** | **132/133 (99%)** | **0** |
| 180 wpm + white noise | 118/133 (89%) | 9 |

| Accent | Voice | Pass rate |
|---|---|---|
| IN / ZA | Rishi / Tessa | 100% |
| US | Samantha | 99% |
| GB / AU / IE | Daniel / Karen / Moira | 98% |
| GB-aged | Grandma (UK) | 88% |

## What each step was worth

**1. The `none` reject class** (LocalLLM `training_data_automotive_none.py`).
497 negatives from three sources — curated everyday utterances, template
expansion, and the medical/industrial phrase banks in the training repo —
filtered so nothing containing car vocabulary can leak into the reject class.
25 phrases are held out of training entirely and used as the evaluation set.
Held-out refusal went 16/25 → 22/25 → **25/25** across v1 → v2 → v2.2.

**2. The confidence gate.** With a reject class the gate stopped being the main
defence: in-domain accuracy is 100% of the phrase bank at every gate from 0.70 to
0.98, and off-topic refusal is flat from 0.95 up. 0.95 stays, now as a second
line rather than the only one (`tools/measure_gate.py` reproduces the sweep).

**3. Constrained decoding.** Vosk decodes against the command phrase bank instead
of the open vocabulary. On the hard subset this took commands from 88% to 93%
and dangerous failures from 5 to 0 — it removes the recognizer's freedom to
invent words: "the on the headlights" → "turn on the headlights", "cancel the
roots" → "cancel the route".

*The trap:* with only command phrases in the grammar, off-topic speech has
nowhere to go and gets forced into a command — off-topic safety collapsed to 39%
even though the model itself refused those phrases as text. Putting the
reject-class phrasings **into the grammar** (4795 phrases now) fixed it: 39% →
100%. Grammar decoding and a reject class only work as a pair.

**4. ASR-confusion augmentation.** 67 pairs of (what the recognizer produced,
what the driver meant), mined from the grid over three rounds. The rule: a
mangling is added only when it cannot plausibly be a different real command.

| Heard | Was | Now |
|---|---|---|
| "cancel the roof" | `body_sunroof_close` — closed the roof instead of ending the route | `navi_cancel_route` 0.9996 |
| "the mirrors" | `body_mirrors_unfold` — the opposite action | `body_mirrors_fold` 0.9997 |
| "so the call" | random commands | `phone_accept_call` 0.9996 |
| "log the doors" | rejected | `body_door_lock` 0.9999 |

**5. Phrasing** (`tools/candidate_phrases_test.py`, 7 accents × rates × noise).
After augmentation the canonical phrasings all reach 100%, so nothing needed
renaming — except the one failure augmentation must not touch:

| Phrase | Pass | Note |
|---|---|---|
| "turn up the volume" | 8/14 | heard as **"turn off the volume"** → mutes the car |
| "volume up" | 11/14 | |
| **"increase the volume"** | **13/14** | now what `CommandCatalog` teaches |
| "louder" | 0/14 | transcribes perfectly, but one word carries too little signal for a 111-class model (0.54 confidence, under the gate) |

That last row is the reusable lesson: **one-word commands are fragile** — not in
the recognizer, in the classifier. The A/B also shows which alternates users
might reach for and would not be understood: "pop the trunk" 11%, "pick up" 39%,
"unlock the trunk" 50% (heard as "lock the trunk" — an antonym flip), "accept the
call" 71% ("zip the call"). Those went into the augmentation rather than into
user education.

**6. Audio front-end — measured and dropped.** A 110 Hz high-pass plus AGC was
built, measured and removed:

| Noise | SNR | With front-end | Without |
|---|---|---|---|
| road | 10 / 5 / 0 dB | 18/18 | 18/18 |
| white | 10 dB | 16/18 | 16/18 |
| white | 5 dB | 14/18 | 13/18 |
| white | 0 dB | 4/18 | 4/18 |

Quiet speech (−20/−30/−36 dB) was unchanged too — Vosk normalizes levels itself.
The hypothesis that a car needs a high-pass was right about the spectrum and
wrong about the consequence: **road noise is harmless even at SNR 0 dB**, because
its energy sits below the formants. White noise is the one that hurts, and no
front-end fixes that. Shipping the filter would have been unmeasured complexity,
so it was deleted; the road-noise condition it motivated stays in the stand.

## What is still open

Nine dangerous failures remain, all under white noise:

- "turn up the volume" → "turn off the volume" → mute (4 clips, 3 accents). The
  phrase is now demoted in the catalog; the residual risk is a driver saying the
  old phrasing anyway. A confirmation step for `media_mute` would close it.
- "open the trunk" → "open the phone" → Bluetooth source, "lock the doors" → "my
  phone" (aged voice). Both are the same shape: a garbled command landing on a
  different real command with high confidence.
- Aged-GB is still the weakest voice at 88%, and contributes 4 of the 9.

The next lever is the recognizer itself, not the classifier: `vosk-model-small`
is 40 MB, and the 128 MB `vosk-model-en-us-0.22-lgraph` would likely take the
aged voice and the white-noise column with it. Worth measuring before adding
another round of augmentation, which is reaching diminishing returns.

## Instrumented counterpart

`shared/src/androidTest/.../VoicePipelineTest.kt` and
`ONNXAIProcessorIntegrationTest` run the same chain on a real device
(`Automotive_1408p_landscape`, API 34), 17 tests: phrase bank 1020/1020, accent
sweep 20/20, off-topic 25/25 refused, grammar 20/20 against open vocabulary
19/20. That is the regression gate; this stand is the exploration.

## Reproduce

```
python3 tools/accent_stress_test.py             # full 770-clip grid (~12 min)
python3 tools/accent_stress_test.py --quick     # 3 accents x 2 conditions
python3 tools/accent_stress_test.py --open-vocabulary   # pre-grammar baseline
python3 tools/candidate_phrases_test.py         # phrasing A/B
python3 tools/measure_gate.py                   # gate sweep
./tools/generate_test_wavs.sh                   # regenerate device fixtures
./gradlew :shared:connectedDebugAndroidTest -Pandroid.injected.build.abi=arm64-v8a
```

Raw data: `results.csv` (one row per clip, with transcript, prediction,
confidence and the text baseline).
