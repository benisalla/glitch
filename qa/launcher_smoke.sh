#!/usr/bin/env bash
# Glitch v1.1 launcher smoke test (adb + uiautomator).
#
# Usage:
#   ./gradlew :app:installDebug          # or: adb install -r app/build/outputs/apk/debug/app-debug.apk
#   qa/launcher_smoke.sh                 # full launcher run
#   qa/launcher_smoke.sh --parse-only    # just open Glitch and print the parsed dock and grid
#   qa/launcher_smoke.sh --parse-file dump.xml   # parse a saved uiautomator dump, no device needed
#
# Environment (all optional):
#   ANDROID_SERIAL   device to use when more than one is connected
#   PKG              application id               (default com.benisalla.glitch)
#   ACTIVITY         activity to start in app mode (default .MainActivity)
#   OTHER_ACTION     intent action for "use another app" (default android.settings.SETTINGS)
#   SHUFFLE_TRIES    unlock attempts before declaring "no reshuffle" (default 3)
#   SNAP_TRIES       max dumps per snapshot while waiting for two identical ones (default 6)
#   DOCK_RID         resource-id (testTag) substring of dock tiles (default dock_tile)
#   GRID_RID         resource-id (testTag) substring of grid tiles (default app_tile)
#                    When no node carries either tag (builds before v1.1), the dock is found by
#                    geometry instead: tiles outside the scrollable grid, or, if the grid does
#                    not scroll, tiles whose top edge is in the bottom DOCK_ZONE_PCT percent of
#                    the Glitch window (default 15).
#   EXPECT_DOCK      expected dock labels joined by " | ", checked once on the home screen,
#                    e.g. "Phone | Messages | Camera | Chrome" for a first run on shuffle_api35
#   EXCLUDE_DESC     extended regex of content descriptions that are not app tiles
#   KEEP_DUMPS=1     keep the raw uiautomator XML files in the work directory
#
# Exit codes: 0 all checks passed, 1 at least one FAIL, 2 not a launcher build
# (launcher checks skipped; app-mode checks still run and are reported), 3 setup error.
# On Android 9 and older (API < 29) there is no `cmd role`, so the home-mode checks are
# SKIPped with a message (the role must be set by hand there) and the run can still exit 0.
#
# Only needs adb, grep, sed and awk on the host.

set -euo pipefail

PKG="${PKG:-com.benisalla.glitch}"
ACTIVITY="${ACTIVITY:-.MainActivity}"
OTHER_ACTION="${OTHER_ACTION:-android.settings.SETTINGS}"
SHUFFLE_TRIES="${SHUFFLE_TRIES:-3}"
SNAP_TRIES="${SNAP_TRIES:-6}"
DOCK_RID="${DOCK_RID:-dock_tile}"
GRID_RID="${GRID_RID:-app_tile}"
DOCK_ZONE_PCT="${DOCK_ZONE_PCT:-15}"
EXCLUDE_DESC="${EXCLUDE_DESC:-^(Loading apps|Glitch|Set as home|Not now|.*[Hh]ome (screen|app).*)$}"
EXPECT_DOCK="${EXPECT_DOCK:-}"
KEEP_DUMPS="${KEEP_DUMPS:-0}"
HOME_ROLE="android.app.role.HOME"

PARSE_ONLY=0
PARSE_FILE=""
case "${1:-}" in
  --parse-only) PARSE_ONLY=1 ;;
  --parse-file)
    PARSE_FILE="${2:-}"
    if [ ! -r "$PARSE_FILE" ]; then echo "usage: $0 --parse-file <uiautomator dump .xml>" >&2; exit 3; fi ;;
  -h|--help) awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"; exit 0 ;;
  "") ;;
  *) echo "unknown argument: $1" >&2; exit 3 ;;
esac

WORK="$(mktemp -d "${TMPDIR:-/tmp}/glitch-smoke.XXXXXX")"
PASSES=0
FAILS=0
SKIPS=0
FAILED_NAMES=""
LAUNCHER_BUILD=0
SDK=0
ROLE_SUPPORTED=0   # `cmd role` exists from Android 10 (API 29)
HOME_MODE=0        # launcher build and the role can be set from adb: run the home-mode checks
RESTORE_NEEDED=0
PREV_HOME=""
PREV_USER_ROTATION=""
PREV_ACCEL_ROTATION=""

log()  { printf '  %s\n' "$*"; }
info() { printf '\n== %s\n' "$*"; }
pass() { PASSES=$((PASSES + 1)); printf 'PASS  %s\n' "$1"; }
fail() {
  FAILS=$((FAILS + 1)); FAILED_NAMES="${FAILED_NAMES}${FAILED_NAMES:+, }$1"
  printf 'FAIL  %s\n' "$1"
  if [ -n "${2:-}" ]; then printf '      %s\n' "$2"; fi
}
skip() { SKIPS=$((SKIPS + 1)); printf 'SKIP  %s (%s)\n' "$1" "$2"; }

adb_sh() { adb shell "$@" < /dev/null | tr -d '\r'; }

# ---------------------------------------------------------------- restore on exit
restore() {
  set +e
  if [ "$RESTORE_NEEDED" = 1 ]; then
    info "Restoring device state"
    if [ -n "$PREV_ACCEL_ROTATION" ]; then
      set_setting accelerometer_rotation "$PREV_ACCEL_ROTATION"
    fi
    if [ -n "$PREV_USER_ROTATION" ]; then
      set_setting user_rotation "$PREV_USER_ROTATION"
    fi
    log "rotation restored (user_rotation=${PREV_USER_ROTATION:-?}, accelerometer_rotation=${PREV_ACCEL_ROTATION:-?})"
    local now
    now="$(home_holder)"
    if [ "$ROLE_SUPPORTED" = 0 ]; then
      log "home role untouched (no cmd role on API $SDK)"
    elif [ "$now" != "$PREV_HOME" ]; then
      if [ -n "$PREV_HOME" ]; then
        adb_sh "cmd role add-role-holder $HOME_ROLE $PREV_HOME" > /dev/null
      else
        adb_sh "cmd role remove-role-holder $HOME_ROLE $PKG" > /dev/null
      fi
      log "home role restored to '${PREV_HOME:-<none>}' (now '$(home_holder)')"
    else
      log "home role unchanged ('${PREV_HOME:-<none>}')"
    fi
    adb_sh input keyevent KEYCODE_HOME > /dev/null
  fi
  if [ "$KEEP_DUMPS" = 1 ]; then
    echo "dumps kept in $WORK"
  else
    rm -rf "$WORK"
  fi
}
trap restore EXIT
trap 'exit 130' INT TERM

# ---------------------------------------------------------------- device helpers
set_setting() {
  # settings put, falling back to the content provider on images where `settings` misbehaves.
  adb_sh "settings put system $1 $2" > /dev/null \
    || adb_sh "content insert --uri content://settings/system --bind name:s:$1 --bind value:i:$2" > /dev/null
}

home_holder() {
  [ "$ROLE_SUPPORTED" = 1 ] || return 0
  adb_sh "cmd role get-role-holders $HOME_ROLE" | grep -v '^$' | head -n 1 || true
}

top_activity() {
  adb_sh dumpsys activity activities \
    | grep -m 1 -E 'topResumedActivity|mResumedActivity' \
    | sed -E 's/.* u[0-9]+ ([^ ]+).*/\1/' || true
}

wait_for_glitch() {
  local i
  for i in $(seq 1 25); do
    case "$(top_activity)" in "$PKG"/*) return 0 ;; esac
    sleep 0.4
  done
  return 1
}

wait_awake_state() { # $1 = Awake|Asleep|Dozing
  local i
  for i in $(seq 1 25); do
    if adb_sh dumpsys power | grep -q "mWakefulness=$1"; then return 0; fi
    sleep 0.4
  done
  return 1
}

# Taps "Wait" on an ANR dialog ("System UI isn't responding"), which slow emulators show.
dismiss_anr() { # $1 = xml file
  local b
  b="$(sed 's/<node/\n<node/g' "$1" | grep 'resource-id="android:id/aerr_wait"' \
        | sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' | head -n 1)"
  [ -n "$b" ] || return 1
  # shellcheck disable=SC2086
  set -- $b
  log "ANR dialog on screen; tapping Wait"
  adb_sh input tap "$(( ($1 + $3) / 2 ))" "$(( ($2 + $4) / 2 ))" > /dev/null
  sleep 2
  return 0
}

# Dumps the hierarchy into $WORK/$1.xml. Retries: uiautomator fails while the UI is animating.
dump_ui() {
  local name="$1" out="$WORK/$1.xml" i
  for i in 1 2 3 4 5; do
    sleep 1
    rm -f "$out"
    adb_sh "uiautomator dump /data/local/tmp/glitch_smoke.xml" > /dev/null 2>&1 || true
    adb exec-out cat /data/local/tmp/glitch_smoke.xml > "$out" 2> /dev/null || true
    if grep -q '</hierarchy>' "$out" 2> /dev/null; then
      if dismiss_anr "$out"; then continue; fi
      if grep -q "package=\"$PKG\"" "$out"; then return 0; fi
    fi
  done
  echo "could not dump a hierarchy containing $PKG (see $out)" >&2
  return 1
}

# Parses a dump into three files:
#   $WORK/$1.grid   one app label per line, in reading order (top-to-bottom, left-to-right)
#   $WORK/$1.dock   same for the dock
#   $WORK/$1.meta   key=value: rotation, scrollable, window size, dock rule used
# A tile is a node of $PKG with a non-empty content-desc (Compose merges icon + label into
# one semantics node whose content-desc is the label). Nodes with identical bounds and label
# are deduplicated; EXCLUDE_DESC drops non-tile descriptions.
parse_dump() {
  local name="$1" xml="$WORK/$1.xml"
  sed 's/<node /\n<node /g' "$xml" | awk \
    -v pkg="$PKG" -v dockrid="$DOCK_RID" -v gridrid="$GRID_RID" -v zone="$DOCK_ZONE_PCT" \
    -v exclude="$EXCLUDE_DESC" -v gridout="$WORK/$name.grid" -v dockout="$WORK/$name.dock" \
    -v metaout="$WORK/$name.meta" '
    function attr(s, a,   r) {
      if (match(s, " " a "=\"[^\"]*\"")) { r = substr(s, RSTART + length(a) + 3, RLENGTH - length(a) - 4); return r }
      return ""
    }
    function unesc(s) {
      gsub(/&lt;/, "<", s); gsub(/&gt;/, ">", s); gsub(/&quot;/, "\"", s); gsub(/&apos;/, "\047", s); gsub(/&amp;/, "\\&", s)
      return s
    }
    /<hierarchy rotation=/ { if (match($0, /rotation="[0-9]+"/)) rot = substr($0, RSTART + 10, RLENGTH - 11) }
    /^<node / {
      if (attr($0, "package") != pkg) next
      b = attr($0, "bounds"); gsub(/[\[\]]/, " ", b); gsub(/,/, " ", b)
      split(b, c, " "); l = c[1] + 0; t = c[2] + 0; r = c[3] + 0; btm = c[4] + 0
      if (r > winR) winR = r; if (btm > winB) winB = btm
      if (attr($0, "scrollable") == "true" && (r - l) * (btm - t) > sarea) {
        sarea = (r - l) * (btm - t); sL = l; sT = t; sR = r; sB = btm
      }
      bk = l "," t "," r "," btm
      rid = attr($0, "resource-id")
      # On API 35 the tagged (clickable) node has an empty content-desc and the label sits on
      # its first child with the same bounds, so that child inherits the tag of its parent.
      if (rid != "" && ((gridrid != "" && index(rid, gridrid)) || (dockrid != "" && index(rid, dockrid)))) {
        tagged = 1; ridAt[bk] = rid
      }
      if (rid == "" && (bk in ridAt)) rid = ridAt[bk]
      d = unesc(attr($0, "content-desc"))
      if (d == "" || d ~ exclude) next
      key = bk "," d
      if (key in seen) next
      seen[key] = 1
      n++; L[n] = l; T[n] = t; R[n] = r; B[n] = btm; D[n] = d; RID[n] = rid
    }
    END {
      rule = tagged ? "resource-id " gridrid "/" dockrid : (sarea > 0 ? "outside-scrollable" : "bottom-" zone "pct")
      for (i = 1; i <= n; i++) {
        cx = (L[i] + R[i]) / 2; cy = (T[i] + B[i]) / 2
        if (tagged) {
          # Tagged UI: only tagged tiles count; anything else (card, menus) is ignored.
          isdock = (dockrid != "" && index(RID[i], dockrid) > 0)
          if (!isdock && !(gridrid != "" && index(RID[i], gridrid) > 0)) continue
        }
        else if (sarea > 0) isdock = (cx < sL || cx > sR || cy < sT || cy > sB)
        else isdock = (T[i] >= winB * (100 - zone) / 100)
        # sort key: top, then left (zero padded) so sort -n is not needed
        line = sprintf("%06d %06d\t%s", T[i], L[i], D[i])
        if (isdock) { nd++; DK[nd] = line } else { ng++; GK[ng] = line }
      }
      # insertion sort (lists are small)
      for (i = 2; i <= ng; i++) { v = GK[i]; j = i - 1; while (j > 0 && GK[j] > v) { GK[j + 1] = GK[j]; j-- } GK[j + 1] = v }
      for (i = 2; i <= nd; i++) { v = DK[i]; j = i - 1; while (j > 0 && DK[j] > v) { DK[j + 1] = DK[j]; j-- } DK[j + 1] = v }
      printf "" > gridout; printf "" > dockout
      for (i = 1; i <= ng; i++) { sub(/^[^\t]*\t/, "", GK[i]); print GK[i] > gridout }
      for (i = 1; i <= nd; i++) { sub(/^[^\t]*\t/, "", DK[i]); print DK[i] > dockout }
      printf "rotation=%s\nscrollable=%d\nwindow=%dx%d\ndockrule=%s\n", rot, (sarea > 0), winR, winB, rule > metaout
    }'
}

meta() { grep "^$2=" "$WORK/$1.meta" | head -n 1 | cut -d= -f2-; }
joined() { awk 'BEGIN { ORS = "" } { if (NR > 1) print " | "; print }' "$1"; }

# Captures dock + grid as snapshot $1 and prints a one-line summary. Waits until the screen has
# settled: the loading indicator is gone and two consecutive dumps parse to the same dock and grid
# (up to SNAP_TRIES dumps, about 1 s apart), so a frame caught mid-update is never compared.
snapshot() {
  local name="$1" i prev="" cur=""
  for i in $(seq 1 "$SNAP_TRIES"); do
    dump_ui "$name"
    parse_dump "$name"
    if grep -q 'content-desc="Loading apps"' "$WORK/$name.xml"; then
      prev=""
      continue
    fi
    cur="$(cat "$WORK/$name.dock")|$(cat "$WORK/$name.grid")"
    if [ -n "$prev" ] && [ "$cur" = "$prev" ]; then break; fi
    prev="$cur"
    if [ "$i" = "$SNAP_TRIES" ]; then log "warning: [$name] screen did not settle after $SNAP_TRIES dumps"; fi
  done
  log "[$name] rotation=$(meta "$name" rotation) scrollable=$(meta "$name" scrollable) dock-rule=$(meta "$name" dockrule)"
  log "[$name] dock ($(wc -l < "$WORK/$name.dock")): $(joined "$WORK/$name.dock")"
  log "[$name] grid ($(wc -l < "$WORK/$name.grid") visible): $(joined "$WORK/$name.grid")"
}

# True when the shorter visible grid is a prefix of the longer one. Rotation changes how many
# tiles are on screen, so exact equality is too strict; reading order must still match.
same_order() {
  local a="$WORK/$1.grid" b="$WORK/$2.grid" na nb n
  na="$(wc -l < "$a")"; nb="$(wc -l < "$b")"
  n=$(( na < nb ? na : nb ))
  [ "$n" -gt 0 ] || return 1
  [ "$(head -n "$n" "$a")" = "$(head -n "$n" "$b")" ]
}

same_dock() { [ "$(cat "$WORK/$1.dock")" = "$(cat "$WORK/$2.dock")" ]; }

# Probability that an independent uniform shuffle of N apps shows the same first k tiles:
# (N-k)!/N! = 1 / (N * (N-1) * ... * (N-k+1)).
same_prefix_probability() { # $1 = N, $2 = k
  awk -v n="$1" -v k="$2" 'BEGIN {
    if (n < 1) { print "unknown"; exit }
    if (k > n) k = n
    p = 1; for (i = 0; i < k; i++) p /= (n - i)
    printf "%.3g", p }'
}

launchable_count() {
  # Launcher activities on the device minus Glitch itself (the catalog hides its own package).
  adb_sh "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER" \
    | grep -E '^[[:space:]]*[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+[[:space:]]*$' \
    | grep -v -c "$PKG/" || true
}

check_dock_stable() { # $1 = reference snapshot, $2 = snapshot, $3 = label
  if same_dock "$1" "$2"; then pass "dock unchanged: $3"
  else fail "dock unchanged: $3" "before: $(joined "$WORK/$1.dock") / after: $(joined "$WORK/$2.dock")"; fi
}

# Repeats an action that should reshuffle up to SHUFFLE_TRIES times.
# $1 check name, $2 reference snapshot, $3 function performing one attempt, $4 snapshot prefix.
# On success leaves the snapshot name of the new order in LAST_SNAP.
expect_reshuffle() {
  local check="$1" ref="$2" action="$3" prefix="$4" try k n p
  LAST_SNAP="$ref"
  k="$(wc -l < "$WORK/$ref.grid")"
  n="$(launchable_count)"
  n=$(( n - $(wc -l < "$WORK/$ref.dock") ))
  [ "$n" -ge "$k" ] || n="$k"
  p="$(same_prefix_probability "$n" "$k")"
  for try in $(seq 1 "$SHUFFLE_TRIES"); do
    "$action"
    snapshot "$prefix-$try"
    LAST_SNAP="$prefix-$try"
    if ! same_order "$ref" "$prefix-$try"; then
      pass "$check (new order on attempt $try)"
      return 0
    fi
    log "attempt $try: same visible order (chance of that by luck alone: $p per attempt)"
    ref="$prefix-$try"
  done
  fail "$check" "order identical on $SHUFFLE_TRIES attempts; with $n shuffled apps and $k visible, a true reshuffle repeats the visible order with probability $p each time, so this is almost certainly a missing reshuffle"
  return 0
}

# ---------------------------------------------------------------- actions
press_home() { adb_sh input keyevent KEYCODE_HOME > /dev/null; wait_for_glitch || true; }

screen_cycle() {
  adb_sh input keyevent KEYCODE_SLEEP > /dev/null
  wait_awake_state Asleep || wait_awake_state Dozing || log "warning: device did not report Asleep"
  sleep 1
  adb_sh input keyevent KEYCODE_WAKEUP > /dev/null
  wait_awake_state Awake || log "warning: device did not report Awake"
  adb_sh "wm dismiss-keyguard" > /dev/null
  sleep 1
  # Some images land on the keyguard or the previous app; Home shows the launcher in all cases.
  case "$(top_activity)" in "$PKG"/*) ;; *) press_home ;; esac
  wait_for_glitch || log "warning: Glitch is not in front after unlock (top: $(top_activity))"
}

open_app_mode() {
  adb_sh "am start -W -n $PKG/$ACTIVITY" > /dev/null || log "warning: am start $PKG/$ACTIVITY failed"
  wait_for_glitch || log "warning: Glitch is not in front (top: $(top_activity))"
}

leave_and_reopen_app_mode() {
  # Leave Glitch for another app (stops the activity), then open it from scratch again.
  adb_sh "am start -W -a $OTHER_ACTION" > /dev/null || log "warning: could not start $OTHER_ACTION"
  sleep 1
  open_app_mode
}

rotate_to() { # $1 = 0..3
  set_setting accelerometer_rotation 0
  set_setting user_rotation "$1"
  sleep 3
}

# ---------------------------------------------------------------- offline parse
if [ -n "$PARSE_FILE" ]; then
  cp "$PARSE_FILE" "$WORK/file.xml"
  parse_dump file
  log "[file] rotation=$(meta file rotation) scrollable=$(meta file scrollable) dock-rule=$(meta file dockrule)"
  log "[file] dock ($(wc -l < "$WORK/file.dock")): $(joined "$WORK/file.dock")"
  log "[file] grid ($(wc -l < "$WORK/file.grid")): $(joined "$WORK/file.grid")"
  exit 0
fi

# ---------------------------------------------------------------- preflight
info "Preflight"
command -v adb > /dev/null || { echo "adb not found on PATH" >&2; exit 3; }
devices="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
if [ -z "$devices" ]; then echo "no device connected (adb devices is empty)" >&2; exit 3; fi
if [ -z "${ANDROID_SERIAL:-}" ] && [ "$(printf '%s\n' "$devices" | wc -l)" -gt 1 ]; then
  echo "several devices connected; set ANDROID_SERIAL to one of: $(printf '%s ' $devices)" >&2; exit 3
fi
SDK="$(adb_sh getprop ro.build.version.sdk)"
case "$SDK" in ''|*[!0-9]*) SDK=0 ;; esac
if [ "$SDK" -ge 29 ]; then ROLE_SUPPORTED=1; fi
log "device: ${ANDROID_SERIAL:-$devices} (API $SDK)"
if ! adb_sh pm path "$PKG" | grep -q '^package:'; then
  echo "$PKG is not installed; run ./gradlew :app:installDebug first" >&2; exit 3
fi
log "installed: $PKG $(adb_sh dumpsys package "$PKG" | grep -m 1 versionName | sed 's/^ *//')"

adb_sh input keyevent KEYCODE_WAKEUP > /dev/null
adb_sh "wm dismiss-keyguard" > /dev/null

if [ "$PARSE_ONLY" = 0 ]; then
  # Only AndroidX's own signature permission <pkg>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION is allowed.
  extra_perms="$(adb_sh dumpsys package "$PKG" \
    | awk '/^ *requested permissions:/ { on = 1; next } on && /^ *[A-Za-z ]+:/ { on = 0 } on { gsub(/^ +| +$/, ""); print }' \
    | grep -v -x "$PKG.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" | grep -v '^$' || true)"
  if [ -z "$extra_perms" ]; then pass "no user-facing permissions requested"
  else fail "no user-facing permissions requested" "requested: $(printf '%s' "$extra_perms" | tr '\n' ' ')"; fi
fi

if [ "$PARSE_ONLY" = 1 ]; then
  open_app_mode
  snapshot parse
  if [ -s "$WORK/parse.grid" ]; then pass "parsed $(wc -l < "$WORK/parse.grid") grid tiles"; else fail "parsed grid tiles" "no tiles found"; fi
  exit $(( FAILS > 0 ))
fi

PREV_HOME="$(home_holder)"
PREV_USER_ROTATION="$(adb_sh settings get system user_rotation)"
PREV_ACCEL_ROTATION="$(adb_sh settings get system accelerometer_rotation)"
case "$PREV_USER_ROTATION" in [0-3]) ;; *) PREV_USER_ROTATION=0 ;; esac
case "$PREV_ACCEL_ROTATION" in [01]) ;; *) PREV_ACCEL_ROTATION=1 ;; esac
RESTORE_NEEDED=1
log "previous home: '${PREV_HOME:-<none>}', user_rotation=$PREV_USER_ROTATION, accelerometer_rotation=$PREV_ACCEL_ROTATION"
rotate_to 0

if adb_sh "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME" \
     | grep -q "$PKG/"; then
  LAUNCHER_BUILD=1
  if [ "$ROLE_SUPPORTED" = 1 ]; then
    HOME_MODE=1
  else
    printf '\nHOME-MODE CHECKS SKIPPED: this device is API %s; `cmd role` needs Android 10 (API 29) or newer,\n' "$SDK"
    printf 'so the default home cannot be set from adb. Set Glitch as home by hand to test home mode.\n'
    printf 'App-mode checks still run.\n'
  fi
else
  printf '\nNOT A LAUNCHER BUILD: %s declares no MAIN/HOME/DEFAULT activity, so it cannot hold %s.\n' "$PKG" "$HOME_ROLE"
  printf 'Launcher checks are skipped; app-mode checks still run so the parsing is exercised.\n'
fi

# ---------------------------------------------------------------- launcher checks
if [ "$HOME_MODE" = 1 ]; then
  info "Launcher mode"
  adb_sh "cmd role add-role-holder $HOME_ROLE $PKG" > /dev/null || true
  if [ "$(home_holder)" = "$PKG" ]; then pass "set as default home ($HOME_ROLE)"
  else fail "set as default home ($HOME_ROLE)" "role holder is '$(home_holder)'"; fi

  press_home
  if wait_for_glitch; then pass "HOME shows Glitch"; else fail "HOME shows Glitch" "top activity: $(top_activity)"; fi
  snapshot home0
  if [ -s "$WORK/home0.grid" ]; then pass "grid tiles parsed ($(wc -l < "$WORK/home0.grid") visible)"
  else fail "grid tiles parsed" "no grid tiles found in dump"; fi
  nd="$(wc -l < "$WORK/home0.dock")"
  if [ "$nd" -ge 1 ] && [ "$nd" -le 4 ]; then pass "dock has 1..4 apps ($nd)"
  else fail "dock has 1..4 apps" "found $nd (dock rule: $(meta home0 dockrule); set DOCK_RID if the heuristic is wrong)"; fi
  if [ -n "$EXPECT_DOCK" ]; then
    if [ "$(joined "$WORK/home0.dock")" = "$EXPECT_DOCK" ]; then pass "dock is: $EXPECT_DOCK"
    else fail "dock is: $EXPECT_DOCK" "found: $(joined "$WORK/home0.dock")"; fi
  fi
  overlap="$(grep -Fxf "$WORK/home0.dock" "$WORK/home0.grid" || true)"
  if [ -z "$overlap" ]; then pass "dock apps are not in the grid"
  else fail "dock apps are not in the grid" "in both: $(printf '%s' "$overlap" | tr '\n' ',')"; fi

  adb_sh input keyevent KEYCODE_BACK > /dev/null
  sleep 1
  snapshot back
  case "$(top_activity)" in
    "$PKG"/*) if same_order home0 back; then pass "Back on home does nothing"
              else fail "Back on home does nothing" "order changed after Back"; fi ;;
    *) fail "Back on home does nothing" "Glitch left the foreground (top: $(top_activity))" ;;
  esac

  adb_sh input keyevent KEYCODE_HOME > /dev/null
  sleep 1
  snapshot home_again
  if same_order home0 home_again; then pass "Home while on Glitch keeps the order"
  else fail "Home while on Glitch keeps the order" "order changed"; fi

  adb_sh "am start -W -a $OTHER_ACTION" > /dev/null || log "warning: could not start $OTHER_ACTION"
  sleep 2
  case "$(top_activity)" in
    "$PKG"/*) fail "other app opens over Glitch" "Glitch still in front" ;;
    *) log "in front: $(top_activity)" ;;
  esac
  press_home
  snapshot after_app
  if same_order home0 after_app; then pass "returning Home after another app keeps the order"
  else fail "returning Home after another app keeps the order" "before: $(joined "$WORK/home0.grid")"; fi
  check_dock_stable home0 after_app "after another app"

  expect_reshuffle "screen off/on reshuffles the grid" after_app screen_cycle unlock
  unlocked="$LAST_SNAP"
  check_dock_stable home0 "$unlocked" "after unlock"

  rotate_to 1
  snapshot land
  if [ "$(meta land rotation)" = 1 ] || [ "$(meta land rotation)" = 3 ]; then
    if same_order "$unlocked" land; then pass "rotation to landscape keeps the order"
    else fail "rotation to landscape keeps the order" "portrait: $(joined "$WORK/$unlocked.grid")"; fi
    check_dock_stable home0 land "landscape"
  else
    skip "rotation to landscape keeps the order" "device stayed at rotation $(meta land rotation)"
  fi
  rotate_to 0
  snapshot portrait
  if same_order "$unlocked" portrait; then pass "rotation back to portrait keeps the order"
  else fail "rotation back to portrait keeps the order"; fi
  check_dock_stable home0 portrait "back in portrait"
else
  for c in "set as default home" "HOME shows Glitch" "dock has 1..4 apps" "Back on home does nothing" \
           "Home while on Glitch keeps the order" "returning Home after another app keeps the order" \
           "screen off/on reshuffles the grid" "rotation keeps the order (home)" "dock unchanged"; do
    if [ "$LAUNCHER_BUILD" = 1 ]; then skip "$c" "no cmd role on API $SDK (needs API 29+)"
    else skip "$c" "not a launcher build"; fi
  done
fi

# ---------------------------------------------------------------- app-mode checks
# Glitch opened like a normal app. When it holds the home role, step away from it first so the
# checks exercise the app path: give the role back to the previous holder for this section.
info "App mode"
if [ "$HOME_MODE" = 1 ] && [ -n "$PREV_HOME" ] && [ "$PREV_HOME" != "$PKG" ]; then
  adb_sh "cmd role add-role-holder $HOME_ROLE $PREV_HOME" > /dev/null || true
  # Without this, a failed hand-back would leave Glitch as home, where keeping the order on
  # return is correct, and the reopen check below would fail for the wrong reason.
  if [ "$(home_holder)" = "$PREV_HOME" ]; then pass "home role handed back to $PREV_HOME for app mode"
  else fail "home role handed back to $PREV_HOME for app mode" "role holder is '$(home_holder)'"; fi
fi
open_app_mode
snapshot app0
if [ -s "$WORK/app0.grid" ]; then pass "app mode: grid tiles parsed ($(wc -l < "$WORK/app0.grid") visible)"
else fail "app mode: grid tiles parsed" "no tiles found"; fi

rotate_to 1
snapshot app_land
if [ "$(meta app_land rotation)" = 1 ] || [ "$(meta app_land rotation)" = 3 ]; then
  if same_order app0 app_land; then pass "app mode: rotation keeps the order"
  else fail "app mode: rotation keeps the order" "portrait: $(joined "$WORK/app0.grid")"; fi
else
  skip "app mode: rotation keeps the order" "device stayed at rotation $(meta app_land rotation)"
fi
rotate_to 0
snapshot app_portrait
if same_order app0 app_portrait; then pass "app mode: rotation back keeps the order"
else fail "app mode: rotation back keeps the order"; fi
if [ "$LAUNCHER_BUILD" = 1 ]; then check_dock_stable app0 app_portrait "app mode rotation"; fi

expect_reshuffle "app mode: reopening gives a new order" app_portrait leave_and_reopen_app_mode reopen

# ---------------------------------------------------------------- summary
info "Summary"
printf '%d passed, %d failed, %d skipped\n' "$PASSES" "$FAILS" "$SKIPS"
if [ "$FAILS" -gt 0 ]; then
  printf 'FAILED: %s\n' "$FAILED_NAMES"
  exit 1
fi
if [ "$LAUNCHER_BUILD" = 0 ]; then
  printf 'RESULT: not a launcher build (exit 2). App-mode checks passed.\n'
  exit 2
fi
if [ "$HOME_MODE" = 0 ]; then
  printf 'RESULT: app-mode checks passed; home-mode checks skipped (API %s has no cmd role)\n' "$SDK"
  exit 0
fi
printf 'RESULT: all checks passed\n'
exit 0
