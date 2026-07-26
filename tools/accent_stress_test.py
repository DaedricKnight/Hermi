#!/usr/bin/env python3
"""
Hermi accent & intonation stress test.

Synthesizes every test phrase with multiple macOS TTS voices (US/GB/AU/IE/IN/ZA
accents), speech rates (slow/normal/fast as an intonation proxy) and an added
white-noise condition, then runs each clip through the SAME pipeline the app
uses: Vosk (vosk-model-small-en-us-0.15) -> the WordPiece tokenizer ported from
ONNXAIProcessor -> automotive_intent_quant.onnx -> the 0.7 confidence gate from
CommandMapper. Produces a per-accent accuracy report and a failure list.

Run from the Hermi repo root:  python3 tools/accent_stress_test.py
Quick smoke run (3 accents, 2 conditions): python3 tools/accent_stress_test.py --quick

Requires: macOS `say`, `pip3 install vosk onnxruntime numpy`.
Synthesis writes straight to WAV files, so nothing is played out loud.
"""
import argparse
import csv
import json
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np
import onnxruntime as ort

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "shared/src/main/assets")
VOSK_MODEL = os.environ.get("HERMI_VOSK_MODEL",
                            os.path.join(ASSETS, "models/vosk-model-small-en-us-0.15"))
GRAMMAR_FILE = os.path.join(ASSETS, "grammar_phrases.txt")
INTENT_MODEL = os.path.join(ASSETS, "automotive_intent_quant.onnx")
VOCAB_FILE = os.path.join(ASSETS, "vocab.txt")
LABELS_FILE = os.path.join(ASSETS, "labels.txt")
CONFIG = os.path.join(ASSETS, "model_config.json")
OUT_DIR = os.environ.get("HERMI_OUT_DIR", os.path.join(ROOT, "tools/accent_test_out"))

# CommandMapper rejects anything below this, or labelled `none` (the model's
# reject class, added in v2).
CONFIDENCE_THRESHOLD = 0.95
NONE_LABEL = "none"

# Labels the model splits but the app treats as one command (CommandCatalog).
SYNONYMS = {
    "navigation_mute_guidance": "navi_mute_guidance",
    "navigation_unmute_guidance": "navi_unmute_guidance",
    "navigation_zoom_in": "navi_zoom_in",
    "navigation_zoom_out": "navi_zoom_out",
    "navigation_stop_route": "navi_cancel_route",
    "media_play_music": "media_play",
    "media_pause_music": "media_pause",
    "media_previous_track": "media_prev_track",
}


def canonical(label):
    return SYNONYMS.get(label, label)
SAMPLE_RATE = 16000

PHRASES = [
    ("lock the doors", "body_door_lock"),
    ("open the sunroof", "body_sunroof_open"),
    ("open the trunk", "body_trunk_open"),
    ("fold the mirrors", "body_mirrors_fold"),
    ("show the rear camera", "camera_show_rear"),
    ("turn on the air conditioning", "climate_ac_on"),
    ("increase the temperature", "climate_increase_temperature"),
    ("turn on the seat heating", "heating_seats_driver_on"),
    ("turn on the headlights", "light_headlights_on"),
    ("play music", "media_play"),
    ("next track", "media_next_track"),
    ("turn up the volume", "media_volume_up"),
    ("navigate home", "navi_go_home"),
    ("find parking", "navi_find_parking"),
    ("cancel the route", "navi_cancel_route"),
    ("answer the call", "phone_accept_call"),
    ("turn on the wipers", "wipers_on"),
    ("turn on the radio", "radio_turn_on"),
    ("turn on the parking sensors", "parking_sensors_on"),
    # Out-of-domain: the app must not act on any of these.
    ("what time is the meeting tomorrow", "none"),
    ("tell me a joke about penguins", "none"),
    ("remind me to buy milk", "none"),
]

VOICES = [
    ("Samantha", "US"),
    ("Daniel", "GB"),
    ("Karen", "AU"),
    ("Moira", "IE"),
    ("Rishi", "IN"),
    ("Tessa", "ZA"),
    ("Grandma (English (UK))", "GB-aged"),
]

# (rate wpm, noise) — rates approximate slow / normal / fast intonation.
# "road" is the realistic cabin case: engine/tyre energy concentrated low, which
# the front-end high-pass is built for. "white" is the pessimistic case: noise
# spread evenly across the band, including where the speech formants live.
CONDITIONS = [(140, None), (180, None), (220, None), (180, "road"), (180, "white")]

SNR_DB = 10.0


def synthesize(text, voice, rate, out_wav):
    subprocess.run(
        ["say", "-v", voice, "-r", str(rate),
         "--file-format=WAVE", "--data-format=LEI16@16000", "-o", out_wav, text],
        check=True, capture_output=True)


def _road_noise(n, rng):
    """
    Cabin noise: brown-ish rumble (1/f^2) plus engine harmonics around 50-150 Hz.
    Nothing like white noise — almost all the energy sits below 300 Hz.
    """
    rumble = np.cumsum(rng.normal(0, 1, n))          # brown noise
    rumble -= np.linspace(rumble[0], rumble[-1], n)  # de-trend
    rumble /= np.abs(rumble).max() or 1.0

    t = np.arange(n) / SAMPLE_RATE
    engine = sum(np.sin(2 * np.pi * f * t + rng.uniform(0, 2 * np.pi)) / k
                 for k, f in enumerate((55.0, 110.0, 165.0), start=1))
    engine /= np.abs(engine).max() or 1.0

    mix = 0.75 * rumble + 0.25 * engine
    return mix / (np.sqrt(np.mean(mix ** 2)) or 1.0)


def add_noise(in_wav, out_wav, snr_db, kind="white"):
    with wave.open(in_wav) as w:
        params = w.getparams()
        pcm = np.frombuffer(w.readframes(w.getnframes()), np.int16).astype(np.float64)
    signal_power = np.mean(pcm ** 2) or 1.0
    noise_power = signal_power / (10 ** (snr_db / 10))
    rng = np.random.default_rng(42)
    if kind == "road":
        noise = _road_noise(len(pcm), rng) * np.sqrt(noise_power)
    else:
        noise = rng.normal(0, np.sqrt(noise_power), len(pcm))
    noisy = np.clip(pcm + noise, -32768, 32767).astype(np.int16)
    with wave.open(out_wav, "w") as w:
        w.setparams(params)
        w.writeframes(noisy.tobytes())


class VoskTranscriber:
    """
    Mirrors VoskManager: one Recognizer at 16 kHz, result JSON -> "text", with
    the same optional grammar constraint and the same `[unk]` stripping.
    """

    def __init__(self, grammar=True):
        from vosk import KaldiRecognizer, Model, SetLogLevel
        SetLogLevel(-1)
        if not os.path.isdir(VOSK_MODEL):
            sys.exit(f"Vosk model not found: {VOSK_MODEL}")
        self.model = Model(VOSK_MODEL)
        self._recognizer_cls = KaldiRecognizer
        self.grammar = None
        if grammar:
            if not os.path.exists(GRAMMAR_FILE):
                sys.exit(f"grammar not found: {GRAMMAR_FILE} "
                         "(run tools/generate_command_catalog.py)")
            with open(GRAMMAR_FILE) as f:
                phrases = [line.strip() for line in f if line.strip()]
            self.grammar = json.dumps(phrases + ["[unk]"])
            print(f"grammar: {len(phrases)} phrases")
        # One recognizer for the whole run: with a 4800-phrase grammar the
        # decoding graph costs far more to build than the recognition itself,
        # and VoskManager builds it once too.
        self._rec = (self._recognizer_cls(self.model, float(SAMPLE_RATE), self.grammar)
                     if self.grammar else self._recognizer_cls(self.model, float(SAMPLE_RATE)))

    def transcribe(self, path):
        with wave.open(path) as w:
            pcm = w.readframes(w.getnframes())
        self._rec.Reset()
        self._rec.AcceptWaveform(pcm)
        text = json.loads(self._rec.FinalResult()).get("text", "")
        return " ".join(w for w in text.split() if w != "[unk]").strip()


class IntentClassifier:
    """Port of ONNXAIProcessor: same vocab, same hand-rolled WordPiece, same gate."""

    def __init__(self):
        self.max_len = json.load(open(CONFIG))["max_length"]
        self.vocab = {}
        with open(VOCAB_FILE, encoding="utf-8") as f:
            for index, line in enumerate(f):
                self.vocab[line.rstrip("\n")] = index
        with open(LABELS_FILE, encoding="utf-8") as f:
            self.labels = [line.strip() for line in f if line.strip()]
        self.session = ort.InferenceSession(INTENT_MODEL, providers=["CPUExecutionProvider"])

    def tokenize(self, text):
        ids = [self.vocab.get("[CLS]", 101)]

        # Split on whitespace, then peel punctuation off into its own tokens.
        tokens = []
        for word in text.lower().strip().split():
            current = ""
            for char in word:
                if char.isalnum():
                    current += char
                else:
                    if current:
                        tokens.append(current)
                    tokens.append(char)
                    current = ""
            if current:
                tokens.append(current)

        for token in tokens:
            if len(ids) >= self.max_len - 1:
                break
            remaining = token
            while remaining:
                subword, end = "", len(remaining)
                while end > 0:
                    candidate = (remaining[:end] if remaining == token
                                 else "##" + remaining[:end])
                    if candidate in self.vocab:
                        subword = candidate
                        break
                    end -= 1
                if not subword:
                    ids.append(self.vocab.get("[UNK]", 100))
                    break
                ids.append(self.vocab[subword])
                remaining = remaining[end:]

        if len(ids) < self.max_len:
            ids.append(self.vocab.get("[SEP]", 102))
        ids = ids[:self.max_len]
        return np.array(ids + [0] * (self.max_len - len(ids)), dtype=np.int64)

    def classify(self, text):
        input_ids = self.tokenize(text)
        attention_mask = (input_ids != 0).astype(np.int64)
        logits = self.session.run(None, {
            "input_ids": input_ids[None, :],
            "attention_mask": attention_mask[None, :]})[0][0]
        exp = np.exp(logits - logits.max())
        probs = exp / exp.sum()
        idx = int(probs.argmax())
        return canonical(self.labels[idx]), float(probs[idx])


def verdict_for(expected, transcript, label, confidence):
    if not transcript:
        return "NO_SPEECH"
    label = canonical(label)
    accepted = label != NONE_LABEL and confidence >= CONFIDENCE_THRESHOLD
    if expected == "none":
        # Out-of-domain: anything the app would act on is a dangerous failure.
        return "PASS" if not accepted else "WRONG_INTENT"
    if label == expected and accepted:
        return "PASS"
    if not accepted:
        return "REJECTED"      # safe failure: the app answers "unknown command"
    return "WRONG_INTENT"      # dangerous failure: a different command fires


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--quick", action="store_true",
                        help="3 accents x 2 conditions instead of the full grid")
    parser.add_argument("--open-vocabulary", action="store_true",
                        help="decode without the command grammar (pre-grammar baseline)")
    args = parser.parse_args()

    voices = VOICES[:3] if args.quick else VOICES
    conditions = [CONDITIONS[1], CONDITIONS[3]] if args.quick else CONDITIONS

    os.makedirs(OUT_DIR, exist_ok=True)
    asr = VoskTranscriber(grammar=not args.open_vocabulary)
    clf = IntentClassifier()

    # Baseline: the written phrase straight into the classifier. Anything that
    # fails here is a model problem, not an accent problem — the audio rows below
    # carry the baseline so the two can never be confused in the report.
    baseline = {}
    print("=== TEXT BASELINE (no audio) ===")
    for phrase, expected in PHRASES:
        label, conf = clf.classify(phrase)
        ok = verdict_for(expected, phrase, label, conf) == "PASS"
        baseline[phrase] = (label, round(conf, 3), ok)
        print(f"{'ok ' if ok else 'BAD'} {expected:26} {label:26} {conf:.3f}  {phrase!r}")
    print()

    rows = []
    total = len(voices) * len(PHRASES) * len(conditions)
    done = 0

    for voice, accent in voices:
        for rate, noise in conditions:
            for phrase, expected in PHRASES:
                done += 1
                cond = f"{rate}wpm{'+' + noise if noise else ''}"
                with tempfile.TemporaryDirectory() as tmp:
                    wav = os.path.join(tmp, "clip.wav")
                    try:
                        synthesize(phrase, voice, rate, wav)
                    except subprocess.CalledProcessError:
                        rows.append([accent, voice, cond, phrase, expected,
                                     "<synthesis failed>", "", 0.0, "SYNTH_FAIL",
                                     baseline[phrase][0], baseline[phrase][1]])
                        continue
                    if noise:
                        noisy_wav = os.path.join(tmp, "noisy.wav")
                        add_noise(wav, noisy_wav, SNR_DB, kind=noise)
                        wav = noisy_wav
                    transcript = asr.transcribe(wav)
                label, conf = ("", 0.0)
                if transcript:
                    label, conf = clf.classify(transcript)
                verdict = verdict_for(expected, transcript, label, conf)
                rows.append([accent, voice, cond, phrase, expected,
                             transcript, label, round(conf, 3), verdict,
                             baseline[phrase][0], baseline[phrase][1]])
                print(f"[{done}/{total}] {accent:8} {cond:12} {verdict:12} {phrase[:34]}",
                      flush=True)

    csv_path = os.path.join(OUT_DIR, "results.csv")
    with open(csv_path, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["accent", "voice", "condition", "phrase", "expected",
                         "transcript", "predicted", "confidence", "verdict",
                         "baseline_label", "baseline_confidence"])
        writer.writerows(rows)

    print("\n=== SUMMARY by accent ===")
    print(f"{'accent':10} {'pass':>5} {'rejected':>9} {'wrong':>6} {'total':>6}  pass-rate")
    for _, accent in voices:
        acc_rows = [r for r in rows if r[0] == accent]
        p = sum(r[8] == "PASS" for r in acc_rows)
        rej = sum(r[8] == "REJECTED" for r in acc_rows)
        w = sum(r[8] == "WRONG_INTENT" for r in acc_rows)
        print(f"{accent:10} {p:5} {rej:9} {w:6} {len(acc_rows):6}  {p / len(acc_rows):.0%}")

    print("\n=== SUMMARY by condition ===")
    for rate, noise in conditions:
        cond = f"{rate}wpm{'+' + noise if noise else ''}"
        c_rows = [r for r in rows if r[2] == cond]
        p = sum(r[8] == "PASS" for r in c_rows)
        w = sum(r[8] == "WRONG_INTENT" for r in c_rows)
        print(f"{cond:14} pass {p}/{len(c_rows)} ({p / len(c_rows):.0%}), wrong-intent {w}")

    print("\n=== SUMMARY by intent ===")
    for phrase, expected in PHRASES:
        p_rows = [r for r in rows if r[3] == phrase]
        p = sum(r[8] == "PASS" for r in p_rows)
        note = "" if baseline[phrase][2] else "  [baseline already fails: model, not accent]"
        print(f"{expected:26} {p}/{len(p_rows)} ({p / len(p_rows):.0%})  said={phrase!r}{note}")

    wrong = [r for r in rows if r[8] == "WRONG_INTENT"]
    asr_induced = [r for r in wrong if baseline[r[3]][2]]
    print(f"\n=== DANGEROUS FAILURES (wrong command above threshold): {len(wrong)} "
          f"({len(asr_induced)} accent/ASR-induced, "
          f"{len(wrong) - len(asr_induced)} already wrong on clean text) ===")
    for r in wrong[:25]:
        tag = "ASR " if baseline[r[3]][2] else "MODEL"
        print(f"  {tag} {r[0]:8} {r[2]:12} said={r[3]!r} heard={r[5]!r} -> {r[6]} {r[7]}")
    print(f"\nFull results: {csv_path}")


if __name__ == "__main__":
    main()
