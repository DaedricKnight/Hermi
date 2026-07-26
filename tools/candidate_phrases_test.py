#!/usr/bin/env python3
"""
A/B test for command phrasings.

Some commands are phonetically fragile: "fold the mirrors" comes back as "for the
mirrors" in every accent and lands on the *opposite* command. This stand takes
several candidate phrasings per action, runs each through the production pipeline
in every accent (plus road noise), and reports which one survives — the winner is
what the docs, the grammar and the command list should teach users to say.

Run from the Hermi repo root:  python3 tools/candidate_phrases_test.py
Options: --quick (3 accents, clean speech only)
"""
import argparse
import os
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from accent_stress_test import (CONFIDENCE_THRESHOLD, IntentClassifier,  # noqa: E402
                                NONE_LABEL, SNR_DB, VOICES, VoskTranscriber,
                                add_noise, canonical, synthesize)

# action -> candidate phrasings, all of which must produce the same label
CANDIDATES = {
    "body_mirrors_fold": [
        "fold the mirrors", "fold in the mirrors", "fold the side mirrors",
        "close the side mirrors", "retract the mirrors",
    ],
    "heating_seats_driver_on": [
        "turn on the seat heating", "heat my seat", "warm up my seat",
        "turn on the driver seat heater", "heat the driver seat",
    ],
    "phone_accept_call": [
        "answer the call", "accept the call", "pick up", "take the call",
        "answer the phone",
    ],
    "body_trunk_open": [
        "open the trunk", "pop the trunk", "open the boot", "release the trunk",
        "unlock the trunk",
    ],
    "body_sunroof_open": [
        "open the sunroof", "open the roof", "slide the sunroof open",
    ],
    "climate_ac_on": [
        "turn on the air conditioning", "turn on the ac", "start the air conditioning",
    ],
}

CONDITIONS = [(140, None), (180, None), (220, None), (180, "road")]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--quick", action="store_true",
                        help="3 accents, clean speech only")
    args = parser.parse_args()

    voices = VOICES[:3] if args.quick else VOICES
    conditions = CONDITIONS[1:2] if args.quick else CONDITIONS

    asr = VoskTranscriber()
    clf = IntentClassifier()
    trials = len(voices) * len(conditions)

    for expected, candidates in CANDIDATES.items():
        print(f"\n=== {expected} ===")
        scored = []
        for phrase in candidates:
            passed = 0
            heard = {}
            for voice, accent in voices:
                for rate, noise in conditions:
                    with tempfile.TemporaryDirectory() as tmp:
                        wav = os.path.join(tmp, "clip.wav")
                        try:
                            synthesize(phrase, voice, rate, wav)
                        except Exception:
                            continue
                        if noise:
                            noisy = os.path.join(tmp, "noisy.wav")
                            add_noise(wav, noisy, SNR_DB, kind=noise)
                            wav = noisy
                        text = asr.transcribe(wav)
                    label, conf = clf.classify(text) if text else ("", 0.0)
                    label = canonical(label)
                    if label == expected and label != NONE_LABEL and conf >= CONFIDENCE_THRESHOLD:
                        passed += 1
                    elif text and text != phrase:
                        heard[text] = heard.get(text, 0) + 1
            scored.append((passed, phrase, heard))
            misheard = sorted(heard.items(), key=lambda kv: -kv[1])[:2]
            note = "  misheard: " + ", ".join(f"{t!r}x{n}" for t, n in misheard) if misheard else ""
            print(f"  {passed:3}/{trials} ({passed / trials:>4.0%})  {phrase!r}{note}")
        best = max(scored)
        print(f"  -> best: {best[1]!r} at {best[0]}/{trials}")


if __name__ == "__main__":
    main()
