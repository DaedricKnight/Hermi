#!/usr/bin/env python3
"""
Generates the Kotlin command surface from the intent model's label set.

Sources of truth:
  * shared/src/main/assets/labels.txt        — the model's output order
  * <LocalLLM>/training_data_automotive.py   — the phrase bank behind those labels

Emits (all marked GENERATED, do not hand-edit):
  * shared/src/main/java/.../model/Command.kt        — one object per intent
  * shared/src/main/java/.../model/CommandCatalog.kt — label -> command, title,
    spoken acknowledgement, tier, example phrasings
  * shared/src/androidTest/java/.../ai/TestPhrases.kt — phrase bank for tests
  * shared/src/main/assets/grammar_phrases.txt — decoding grammar for Vosk

Run from the repo root:  python3 tools/generate_command_catalog.py
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOCAL_LLM = os.environ.get("HERMI_INTENT_REPO",
                           os.path.join(os.path.dirname(ROOT), "LocalLLM"))
LABELS_FILE = os.path.join(ROOT, "shared/src/main/assets/labels.txt")
MAIN_KT = os.path.join(ROOT, "shared/src/main/java/com/khateev/hermi/shared/model")
GRAMMAR_ASSET = os.path.join(ROOT, "shared/src/main/assets/grammar_phrases.txt")
TEST_KT = os.path.join(ROOT, "shared/src/androidTest/java/com/khateev/hermi/shared/ai")

# The model ships eight pairs of labels that mean the same user action (the
# phrase banks overlap outright — "Stop navigation" sits in navi_cancel_route
# while "Cancel navigation" sits in navigation_stop_route). They collapse onto
# one Command each; the classifier may emit either.
# The reject class is not a command — it maps to Command.Unknown in the app.
NONE_LABEL = "none"

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

CATEGORIES = {
    "body": ("BODY", "Body & Access"),
    "camera": ("CAMERA", "Cameras"),
    "climate": ("CLIMATE", "Climate"),
    "heating": ("HEATING", "Heating & Cooling"),
    "light": ("LIGHT", "Lights"),
    "media": ("MEDIA", "Media"),
    "navi": ("NAVIGATION", "Navigation"),
    "navigation": ("NAVIGATION", "Navigation"),
    "parking": ("PARKING", "Parking Aids"),
    "phone": ("PHONE", "Phone"),
    "radio": ("RADIO", "Radio"),
    "seat": ("SEAT", "Seats"),
    "system": ("SYSTEM", "System"),
    "wipers": ("WIPERS", "Wipers"),
}

# Everyday commands that stay in the free tier. Product can move the line; the
# rest of the catalog is PRO.
FREE = {
    "body_door_lock", "body_door_unlock", "climate_ac_on", "climate_ac_off",
    "climate_increase_temperature", "climate_decrease_temperature",
    "media_play", "media_pause", "media_volume_up", "media_volume_down",
    "phone_accept_call", "phone_end_call",
}

# Phrasings measured as the most robust for that command
# (tools/candidate_phrases_test.py, 7 accents x speech rates x noise). They are
# listed first in `examples`, which is what the command-list screen teaches.
# "turn up the volume" is deliberately not the one taught: under noise it comes
# back as "turn off the volume" and mutes the car instead. The bare word
# "louder" transcribes perfectly but carries too little signal for a 111-class
# model (0.54 confidence, below the gate) — one-word commands are fragile.
PREFERRED_PHRASING = {
    "media_volume_up": "increase the volume",
    "media_volume_down": "turn down the volume",
    "phone_accept_call": "answer the call",
    "body_trunk_open": "open the trunk",
    "body_mirrors_fold": "fold the side mirrors",
    "heating_seats_driver_on": "turn on the seat heating",
    "climate_ac_on": "turn on the air conditioning",
    "body_sunroof_open": "open the sunroof",
}

# Titles/acknowledgements that the rules below cannot phrase naturally.
OVERRIDES = {
    "body_close_windows_all": ("Close all windows", "Closing all windows."),
    "body_open_window_driver": ("Open driver window", "Opening the driver window."),
    "body_open_window_passenger": ("Open passenger window", "Opening the passenger window."),
    "camera_show_360": ("Show 360 view", "Showing the 360 degree view."),
    "climate_ac_on": ("Air conditioning on", "Turning on the air conditioning."),
    "climate_ac_off": ("Air conditioning off", "Turning off the air conditioning."),
    "climate_air_recirculation_on": ("Recirculation on", "Turning on air recirculation."),
    "climate_air_recirculation_off": ("Recirculation off", "Turning off air recirculation."),
    "climate_auto_mode_on": ("Auto climate on", "Switching climate control to auto."),
    "climate_defrost_on": ("Defrost on", "Turning on the defroster."),
    "climate_defrost_windshield": ("Defrost windshield", "Defrosting the windshield."),
    "climate_direction_face": ("Airflow to face", "Directing airflow to the face."),
    "climate_direction_feet": ("Airflow to feet", "Directing airflow to the feet."),
    "climate_fan_speed_up": ("Fan speed up", "Increasing the fan speed."),
    "climate_fan_speed_down": ("Fan speed down", "Decreasing the fan speed."),
    "climate_set_temperature": ("Set temperature", "Setting the cabin temperature."),
    "climate_steering_heat_on": ("Steering heat on", "Heating the steering wheel."),
    "climate_sync": ("Sync climate zones", "Syncing the climate zones."),
    "heating_cooling_seats_driver_on": ("Cool driver seat", "Cooling the driver seat."),
    "heating_rear_window_on": ("Rear window heating", "Heating the rear window."),
    "heating_seats_driver_on": ("Heat driver seat", "Heating the driver seat."),
    "heating_seats_passenger_on": ("Heat passenger seat", "Heating the passenger seat."),
    "heating_seats_rear_on": ("Heat rear seats", "Heating the rear seats."),
    "heating_seats_off": ("Seat heating off", "Turning off the seat heating."),
    "heating_steering_wheel_off": ("Steering heat off", "Turning off the steering wheel heating."),
    "light_high_beams_on": ("High beams on", "Turning on the high beams."),
    "light_high_beams_off": ("High beams off", "Turning off the high beams."),
    "media_next_track": ("Next track", "Skipping to the next track."),
    "media_prev_track": ("Previous track", "Going back to the previous track."),
    "media_source_bluetooth": ("Bluetooth source", "Switching the media source to Bluetooth."),
    "media_source_radio": ("Radio source", "Switching the media source to radio."),
    "navi_cancel_route": ("Cancel route", "Cancelling the route."),
    "navi_go_home": ("Navigate home", "Starting navigation home."),
    "navi_go_work": ("Navigate to work", "Starting navigation to work."),
    "navi_find_gas_station": ("Find gas station", "Searching for gas stations nearby."),
    "navi_find_parking": ("Find parking", "Searching for parking nearby."),
    "navi_find_restaurant": ("Find restaurant", "Searching for restaurants nearby."),
    "navi_mute_guidance": ("Mute guidance", "Muting the navigation guidance."),
    "navi_unmute_guidance": ("Unmute guidance", "Unmuting the navigation guidance."),
    "navi_show_route": ("Show route", "Showing the full route."),
    "navigation_start_route": ("Start route", "Starting the route."),
    "parking_sensors_on": ("Parking sensors on", "Turning on the parking sensors."),
    "parking_sensors_off": ("Parking sensors off", "Turning off the parking sensors."),
    "phone_accept_call": ("Accept call", "Answering the call."),
    "phone_reject_call": ("Reject call", "Rejecting the call."),
    "phone_call_contact": ("Call contact", "Placing the call."),
    "phone_dial_number": ("Dial number", "Dialling the number."),
    "phone_end_call": ("End call", "Ending the call."),
    "phone_mute_mic": ("Mute microphone", "Muting the microphone."),
    "phone_unmute_mic": ("Unmute microphone", "Unmuting the microphone."),
    "phone_redial": ("Redial", "Redialling the last number."),
    "radio_next_station": ("Next station", "Tuning to the next station."),
    "radio_previous_station": ("Previous station", "Tuning to the previous station."),
    "seat_incline": ("Raise seat back", "Raising the seat back."),
    "seat_recline": ("Recline seat", "Reclining the seat."),
    "seat_lumbar_support_up": ("More lumbar support", "Increasing the lumbar support."),
    "seat_lumbar_support_down": ("Less lumbar support", "Decreasing the lumbar support."),
    "seat_move_forward": ("Seat forward", "Moving the seat forward."),
    "seat_move_backward": ("Seat backward", "Moving the seat back."),
    "system_open_settings": ("Open settings", "Opening the settings."),
    "system_mute": ("Mute system", "Muting the system sound."),
    "wipers_wash_windshield": ("Wash windshield", "Washing the windshield."),
    "wipers_speed_up": ("Wipers faster", "Speeding up the wipers."),
    "wipers_speed_down": ("Wipers slower", "Slowing down the wipers."),
    "wipers_on": ("Wipers on", "Turning on the wipers."),
    "wipers_off": ("Wipers off", "Turning off the wipers."),
    "body_door_lock": ("Lock doors", "Locking the doors."),
    "body_door_unlock": ("Unlock doors", "Unlocking the doors."),
    "body_mirrors_fold": ("Fold mirrors", "Folding the mirrors."),
    "body_mirrors_unfold": ("Unfold mirrors", "Unfolding the mirrors."),
    "body_sunroof_open": ("Open sunroof", "Opening the sunroof."),
    "body_sunroof_close": ("Close sunroof", "Closing the sunroof."),
    "body_trunk_open": ("Open trunk", "Opening the trunk."),
    "body_trunk_close": ("Close trunk", "Closing the trunk."),
    "camera_show_front": ("Front camera", "Showing the front camera."),
    "camera_show_rear": ("Rear camera", "Showing the rear camera."),
    "climate_heat_on": ("Heating on", "Turning on the heating."),
    "climate_heat_off": ("Heating off", "Turning off the heating."),
    "light_fog_on": ("Fog lights on", "Turning on the fog lights."),
    "light_fog_off": ("Fog lights off", "Turning off the fog lights."),
    "light_reading_on": ("Reading light on", "Turning on the reading light."),
    "light_reading_off": ("Reading light off", "Turning off the reading light."),
    "media_play": ("Play", "Resuming playback."),
    "media_pause": ("Pause", "Pausing playback."),
    "media_mute": ("Mute media", "Muting the media."),
    "media_play_podcast": ("Play podcast", "Playing your podcast."),
    "media_volume_up": ("Volume up", "Turning the volume up."),
    "media_volume_down": ("Volume down", "Turning the volume down."),
    "navi_zoom_in": ("Zoom in", "Zooming in on the map."),
    "navi_zoom_out": ("Zoom out", "Zooming out on the map."),
    "radio_turn_on": ("Radio on", "Turning on the radio."),
    "radio_turn_off": ("Radio off", "Turning off the radio."),
    "seat_massage_on": ("Seat massage on", "Turning on the seat massage."),
    "seat_massage_off": ("Seat massage off", "Turning off the seat massage."),
    "seat_ventilation_on": ("Seat ventilation on", "Turning on the seat ventilation."),
    "seat_ventilation_off": ("Seat ventilation off", "Turning off the seat ventilation."),
    "system_volume_up": ("System volume up", "Turning the system volume up."),
    "system_volume_down": ("System volume down", "Turning the system volume down."),
}

GERUND = {
    "open": "Opening", "close": "Closing", "lock": "Locking", "unlock": "Unlocking",
    "fold": "Folding", "unfold": "Unfolding", "show": "Showing", "play": "Playing",
    "pause": "Pausing", "mute": "Muting", "unmute": "Unmuting", "start": "Starting",
    "stop": "Stopping", "cancel": "Cancelling", "end": "Ending", "increase": "Increasing",
    "decrease": "Decreasing", "set": "Setting", "move": "Moving", "zoom": "Zooming",
    "turn": "Turning", "wash": "Washing", "dial": "Dialling", "call": "Calling",
    "accept": "Answering", "reject": "Rejecting", "redial": "Redialling",
}


def pascal(label):
    """navi_mute_guidance -> NavigationMuteGuidance"""
    parts = label.split("_")
    if parts[0] == "navi":
        parts[0] = "navigation"
    return "".join(p.capitalize() for p in parts)


def derive(label):
    """(title, acknowledgement) for labels the override table does not cover."""
    body = label.split("_", 1)[1].replace("_", " ")
    words = body.split()
    if words[-1] in ("on", "off"):
        subject = " ".join(words[:-1])
        return (f"{subject.capitalize()} {words[-1]}",
                f"Turning {words[-1]} the {subject}.")
    verb = GERUND.get(words[0])
    if verb:
        rest = " ".join(words[1:])
        return (body.capitalize(), f"{verb} the {rest}." if rest else f"{verb}.")
    return (body.capitalize(), f"Adjusting the {body}.")


def load_none_phrases():
    """Reject-class phrasings, if the training repo has them."""
    sys.path.insert(0, LOCAL_LLM)
    try:
        from training_data_automotive_none import NONE_DATA
    except ImportError:
        return []
    return [text for text, _ in NONE_DATA]


def load_phrase_bank():
    sys.path.insert(0, LOCAL_LLM)
    try:
        from training_data_automotive import TRAINING_DATA
    except ImportError:
        sys.exit(f"phrase bank not found — set HERMI_INTENT_REPO (looked in {LOCAL_LLM})")
    bank = {}
    for text, label in TRAINING_DATA:
        bank.setdefault(label, []).append(text)
    return bank


HEADER = ("/**\n * GENERATED by tools/generate_command_catalog.py — do not edit by hand.\n"
          " * Regenerate after retraining the intent model or changing labels.txt.\n */\n")


def main():
    labels = [l.strip() for l in open(LABELS_FILE) if l.strip()]
    canonical = [l for l in labels if l not in SYNONYMS and l != NONE_LABEL]
    bank = load_phrase_bank()
    has_none = NONE_LABEL in labels
    print(f"{len(labels)} labels -> {len(canonical)} commands "
          f"({len(SYNONYMS)} synonym labels merged"
          f"{', reject class present' if has_none else ', NO reject class'})")

    specs = []
    for label in canonical:
        prefix = label.split("_", 1)[0]
        enum_name, _ = CATEGORIES[prefix]
        title, ack = OVERRIDES.get(label) or derive(label)
        aliases = sorted(k for k, v in SYNONYMS.items() if v == label)
        phrases = sorted(set(bank.get(label, [])))
        for alias in aliases:
            phrases += sorted(set(bank.get(alias, [])))
        preferred = PREFERRED_PHRASING.get(label)
        if preferred:
            phrases = [preferred] + [p for p in phrases if p.lower() != preferred.lower()]
        specs.append(dict(label=label, name=pascal(label), category=enum_name,
                          title=title, ack=ack, aliases=aliases,
                          version="FREE" if label in FREE else "PRO",
                          phrases=phrases))

    # ---- Command.kt ----
    out = [HEADER, "package com.khateev.hermi.shared.model\n",
           "/**\n * One object per intent the model can emit ("
           f"{len(canonical)} commands from {len(labels)} labels;\n"
           " * synonym labels collapse onto a single command), plus [Unknown] for\n"
           " * everything the classifier is not confident about.\n */",
           "sealed class Command {\n"]
    for prefix, (enum_name, human) in sorted(set(CATEGORIES.items()),
                                             key=lambda kv: kv[1][0]):
        group = [s for s in specs if s["category"] == enum_name]
        if not group or prefix == "navigation":
            continue
        out.append(f"    // ===== {human} ({len(group)}) =====")
        for s in group:
            out.append(f"    object {s['name']} : Command()")
        out.append("")
    out.append("    /** Nothing matched above the confidence gate. */")
    out.append("    data class Unknown(val rawText: String) : Command()")
    out.append("}")
    write(os.path.join(MAIN_KT, "Command.kt"), "\n".join(out) + "\n")

    # ---- CommandCatalog.kt ----
    cat = [HEADER, "package com.khateev.hermi.shared.model\n",
           "/** UI grouping for the command list. */",
           "enum class CommandCategory(val title: String) {"]
    seen = []
    for prefix, (enum_name, human) in CATEGORIES.items():
        if enum_name not in seen:
            seen.append(enum_name)
            cat.append(f"    {enum_name}(\"{human}\"),")
    cat.append("}\n")
    cat.append("""/**
 * Everything the app knows about one intent: the model label(s) that produce it,
 * how it is announced, which tier it belongs to, and how a user might phrase it.
 */
data class CommandSpec(
    val label: String,
    val command: Command,
    val category: CommandCategory,
    val title: String,
    val acknowledgement: String,
    val version: CommandVersion,
    val aliases: List<String> = emptyList(),
    val examples: List<String> = emptyList()
)

object CommandCatalog {

    val specs: List<CommandSpec> = listOf(""")
    for s in specs:
        ex = ", ".join(f'"{p}"' for p in s["phrases"][:3])
        al = ", ".join(f'"{a}"' for a in s["aliases"])
        cat.append(f'        CommandSpec("{s["label"]}", Command.{s["name"]}, '
                   f'CommandCategory.{s["category"]}, "{s["title"]}",\n'
                   f'            "{s["ack"]}", CommandVersion.{s["version"]}, '
                   f'listOf({al}), listOf({ex})),')
    cat.append("""    )

    /** Every model label, including synonyms, mapped onto its command. */
    private val byLabel: Map<String, CommandSpec> =
        specs.flatMap { spec -> (listOf(spec.label) + spec.aliases).map { it to spec } }.toMap()

    private val byCommand: Map<Command, CommandSpec> = specs.associateBy { it.command }

    fun specForLabel(label: String): CommandSpec? = byLabel[label]

    fun commandForLabel(label: String): Command? = byLabel[label]?.command

    fun specFor(command: Command): CommandSpec? = byCommand[command]

    fun byCategory(): Map<CommandCategory, List<CommandSpec>> = specs.groupBy { it.category }
}""")
    write(os.path.join(MAIN_KT, "CommandCatalog.kt"), "\n".join(cat) + "\n")

    # ---- TestPhrases.kt ----
    tp = [HEADER, "package com.khateev.hermi.shared.ai\n",
          "/**\n * Phrase bank behind the shipped intent model, keyed by label.\n"
          " * These phrasings trained the model, so they measure wiring and\n"
          " * regressions — not generalization. Accent/ASR robustness lives in\n"
          " * VoicePipelineTest and tools/accent_stress_test.py.\n */",
          "object TestPhrases {", "    val data: Map<String, List<String>> = mapOf("]
    per_label = 10
    for s in specs:
        picked = s["phrases"][:per_label]
        tp.append(f'        "{s["label"]}" to listOf(')
        for i in range(0, len(picked), 3):
            tp.append("            " + ", ".join(f'"{p}"' for p in picked[i:i + 3]) + ",")
        tp[-1] = tp[-1].rstrip(",")
        tp.append("        ),")
    tp.append("    )")
    tp.append("}")
    write(os.path.join(TEST_KT, "TestPhrases.kt"), "\n".join(tp) + "\n")

    # ---- grammar_phrases.txt ----
    # Vosk decodes against these instead of the open vocabulary, which is what
    # keeps "check while level"-style garbling out of the classifier. One phrase
    # per line, lowercase, letters and digits only (Vosk drops OOV words itself).
    # The reject-class phrasings belong in the grammar too: without them the
    # decoder has nowhere to put off-topic speech and forces it into a command
    # ("remind me to buy milk" -> "we mind me to by melt" -> reading light off).
    grammar = set()
    for phrase in [p for s in specs for p in s["phrases"]] + load_none_phrases():
        words = re.sub(r"[^a-z0-9 ]", " ", phrase.lower()).split()
        if words:
            grammar.add(" ".join(words))
    write(GRAMMAR_ASSET, "\n".join(sorted(grammar)) + "\n")


def write(path, text):
    with open(path, "w") as f:
        f.write(text)
    print(f"  wrote {os.path.relpath(path, ROOT)} ({len(text.splitlines())} lines)")


if __name__ == "__main__":
    main()
