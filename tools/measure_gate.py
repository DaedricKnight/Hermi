#!/usr/bin/env python3
"""
Picks the confidence gate for CommandMapper, from data rather than taste.

For each candidate threshold it reports, using the shipped assets:
  * in-domain — share of the model's own phrase bank still answered correctly
  * off-topic — share of held-out non-car phrases the app would refuse to act on

The off-topic set is `HELD_OUT` from the training repo: phrases the model has
never seen, mirrored by ONNXAIProcessorIntegrationTest on device.

Run from the Hermi repo root:  python3 tools/measure_gate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from accent_stress_test import IntentClassifier, canonical  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOCAL_LLM = os.environ.get("HERMI_INTENT_REPO",
                           os.path.join(os.path.dirname(ROOT), "LocalLLM"))
GATES = (0.70, 0.80, 0.90, 0.95, 0.98, 0.99)


def main():
    sys.path.insert(0, LOCAL_LLM)
    try:
        from training_data_automotive import TRAINING_DATA
        from training_data_automotive_none import HELD_OUT
    except ImportError:
        sys.exit(f"training repo not found — set HERMI_INTENT_REPO (looked in {LOCAL_LLM})")

    clf = IntentClassifier()
    has_none = "none" in clf.labels
    print(f"model: {len(clf.labels)} labels, reject class {'present' if has_none else 'ABSENT'}")

    in_domain = [(canonical(expected),) + clf.classify(text) for text, expected in TRAINING_DATA]
    off_topic = [clf.classify(text) for text in HELD_OUT]

    print(f"\n{'gate':>6} {'in-domain':>22} {'off-topic rejected':>22}")
    for gate in GATES:
        correct = sum(1 for exp, pred, conf in in_domain
                      if pred == exp and conf >= gate and pred != "none")
        rejected = sum(1 for pred, conf in off_topic if pred == "none" or conf < gate)
        print(f"{gate:>6.2f} {correct:>10}/{len(in_domain)} ({correct / len(in_domain):>5.1%})"
              f" {rejected:>12}/{len(off_topic)} ({rejected / len(off_topic):>5.0%})")

    print("\nOff-topic phrases that still fire at 0.95:")
    for text, (pred, conf) in zip(HELD_OUT, off_topic):
        if pred != "none" and conf >= 0.95:
            print(f"  {text!r} -> {pred} {conf:.3f}")


if __name__ == "__main__":
    main()
