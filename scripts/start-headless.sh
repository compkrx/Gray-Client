#!/usr/bin/env bash
# Linux cloud launcher for an Xorg dummy display (no package installation).
# Usage: scripts/start-headless.sh ./gradlew runClient
#        DISPLAY=:100 scripts/start-headless.sh xset q
# With no command, a new display stays supervised in the foreground until Ctrl+C.
# An already accessible display is reused and never stopped by this script.
set -euo pipefail

export DISPLAY="${DISPLAY:-:99}"
command -v xset >/dev/null 2>&1 || { printf 'Missing xset (X11 utilities).\n' >&2; exit 1; }

print_environment() {
    printf 'export DISPLAY=%q\n' "$DISPLAY"
    if [[ -n ${XAUTHORITY:-} ]]; then printf 'export XAUTHORITY=%q\n' "$XAUTHORITY"; fi
}

if xset -display "$DISPLAY" q >/dev/null 2>&1; then
    printf 'Reusing accessible display %s; it will not be stopped.\n' "$DISPLAY" >&2
    if (( $# )); then exec "$@"; fi
    print_environment
    exit 0
fi

# Never replace an occupied display or try to bypass its authentication.
if [[ ! $DISPLAY =~ ^:([0-9]+)(\.[0-9]+)?$ ]]; then
    printf 'Display %s is inaccessible. A new dummy display requires a local value such as :99.\n' "$DISPLAY" >&2
    exit 1
fi
display_number=${BASH_REMATCH[1]}
if [[ -e /tmp/.X11-unix/X${display_number} || -e /tmp/.X${display_number}-lock ]]; then
    printf 'Display %s already has a socket or lock but is inaccessible. Choose another DISPLAY or supply its existing authorization.\n' "$DISPLAY" >&2
    exit 1
fi
for dependency in Xorg xauth od; do
    command -v "$dependency" >/dev/null 2>&1 || { printf 'Missing prerequisite: %s.\n' "$dependency" >&2; exit 1; }
done

umask 077
runtime_root="${TMPDIR:-/tmp}/gray-client-display"
mkdir -p "$runtime_root"
runtime_dir=$(mktemp -d "$runtime_root/display-${display_number}.XXXXXX")
config="$runtime_dir/xorg-dummy.conf"
log="$runtime_dir/Xorg.log"
export XAUTHORITY="$runtime_dir/Xauthority"
touch "$XAUTHORITY"
cookie=$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')
xauth -f "$XAUTHORITY" add "$DISPLAY" MIT-MAGIC-COOKIE-1 "$cookie"
unset cookie

cat > "$config" <<'XORG'
Section "ServerFlags"
    Option "AutoAddDevices" "false"
    Option "DontVTSwitch" "true"
EndSection
Section "Device"
    Identifier "Dummy"
    Driver "dummy"
    VideoRam 256000
EndSection
Section "Monitor"
    Identifier "Monitor"
    HorizSync 5.0 - 1000.0
    VertRefresh 5.0 - 200.0
    Modeline "1280x720" 74.5 1280 1344 1472 1664 720 723 728 748
EndSection
Section "Screen"
    Identifier "Screen"
    Device "Dummy"
    Monitor "Monitor"
    DefaultDepth 24
    SubSection "Display"
        Depth 24
        Modes "1280x720"
    EndSubSection
EndSection
XORG

xorg_pid=''
command_pid=''
cleanup() {
    # This PID belongs to the server launched below, never a reused display.
    if [[ -n $xorg_pid ]]; then
        if kill -0 "$xorg_pid" 2>/dev/null; then kill "$xorg_pid" 2>/dev/null || true; fi
        wait "$xorg_pid" 2>/dev/null || true
    fi
    rm -f "$XAUTHORITY"
}
interrupted() {
    local result=$1
    if [[ -n $command_pid ]] && kill -0 "$command_pid" 2>/dev/null; then
        kill -TERM "$command_pid" 2>/dev/null || true
    fi
    exit "$result"
}
trap cleanup EXIT
trap 'interrupted 130' INT
trap 'interrupted 143' TERM

Xorg ":${display_number}" -config "$config" -logfile "$log" \
    -auth "$XAUTHORITY" -nolisten tcp -noreset > "$runtime_dir/server-output.log" 2>&1 &
xorg_pid=$!
ready=false
for ((attempt = 0; attempt < 100; attempt++)); do
    if xset -display "$DISPLAY" q >/dev/null 2>&1; then ready=true; break; fi
    if ! kill -0 "$xorg_pid" 2>/dev/null; then break; fi
    sleep 0.1
done
if [[ $ready != true ]]; then
    printf 'Xorg did not become ready. Logs: %s\n' "$runtime_dir" >&2
    if [[ -f $log ]]; then tail -n 30 "$log" >&2; else tail -n 30 "$runtime_dir/server-output.log" >&2; fi
    exit 1
fi
printf 'Dummy display %s is ready. Logs: %s\n' "$DISPLAY" "$runtime_dir" >&2

if (( $# )); then
    # Keep the supervising shell so EXIT can stop only our Xorg afterward.
    "$@" <&0 &
    command_pid=$!
    if wait "$command_pid"; then result=0; else result=$?; fi
    command_pid=''
    exit "$result"
fi

print_environment
printf 'Keep this terminal open while using the display; Ctrl+C stops this owned Xorg.\n' >&2
wait "$xorg_pid"
