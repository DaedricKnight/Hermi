#!/usr/bin/env bash
# Regenerates the androidTest voice fixtures for VoicePipelineTest.
#
# Every clip is macOS TTS (`say`) written straight to a 16 kHz mono PCM16 WAV —
# the exact format AudioRecorderImpl feeds the pipeline. Nothing is played out
# loud. Run from the repo root:  ./tools/generate_test_wavs.sh
set -euo pipefail

OUT="$(cd "$(dirname "$0")/.." && pwd)/shared/src/androidTest/assets/tts"
rm -rf "$OUT"
mkdir -p "$OUT"

say_wav() { # <file> <voice> <rate> <text>
  say -v "$2" -r "$3" --file-format=WAVE --data-format=LEI16@16000 -o "$OUT/$1" "$4"
  echo "  $1  ($2 @ $3 wpm)  \"$4\""
}

echo "Writing fixtures to $OUT"

# US (Samantha) — one clip per command family the test asserts on
say_wav door_lock_samantha.wav          Samantha 180 "lock the doors"
say_wav sunroof_open_samantha.wav       Samantha 180 "open the sunroof"
say_wav ac_on_samantha.wav              Samantha 180 "turn on the air conditioning"
say_wav temp_up_samantha.wav            Samantha 180 "increase the temperature"
say_wav media_play_samantha.wav         Samantha 180 "play music"
say_wav volume_up_samantha.wav          Samantha 180 "turn up the volume"
say_wav navi_home_samantha.wav          Samantha 180 "navigate home"
say_wav find_parking_samantha.wav       Samantha 180 "find parking"

# GB (Daniel), AU (Karen), IE (Moira), IN (Rishi), ZA (Tessa) — accent coverage
say_wav headlights_on_daniel.wav        Daniel   180 "turn on the headlights"
say_wav next_track_daniel.wav           Daniel   180 "next track"
say_wav wipers_on_karen.wav             Karen    180 "turn on the wipers"
say_wav zoom_in_karen.wav               Karen    180 "zoom in on the map"
say_wav cancel_route_moira.wav          Moira    180 "cancel the route"
say_wav accept_call_rishi.wav           Rishi    180 "accept the call"
say_wav radio_on_rishi.wav              Rishi    180 "turn on the radio"
say_wav rear_camera_tessa.wav           Tessa    180 "show the rear camera"
say_wav parking_sensors_tessa.wav       Tessa    180 "turn on the parking sensors"

# Intonation extremes on one command — slow and fast delivery
say_wav door_lock_slow_daniel.wav       Daniel   130 "lock the doors"
say_wav door_lock_fast_daniel.wav       Daniel   240 "lock the doors"

# Synonym labels that must land on the same command
say_wav media_pause_samantha.wav        Samantha 180 "stop the music"

# Out-of-domain — must not fire a command
say_wav ood_milk_samantha.wav           Samantha 180 "remind me to buy milk"

echo "Done."
