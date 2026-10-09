#!/bin/zsh
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
PKG=com.deezus.wordy
ACT=$PKG/.ui.MainActivity
a()      { "$ADB" -s emulator-5554 "$@"; }
stop()   { a shell am force-stop $PKG; }
launch() { a shell am start -W -n $ACT >/dev/null; sleep 1.5; }
stage()  { printf '%s' "$1" > sg.json; a push sg.json /sdcard/sg.json >/dev/null; a shell "cat /sdcard/sg.json | run-as $PKG sh -c 'mkdir -p files/datastore; cat > files/datastore/saved_games.json'"; }
show_sg(){ a shell run-as $PKG cat files/datastore/saved_games.json; echo; }
type_()  { a shell input text "$1"; }
enter()  { a shell input keyevent 66; }
tap()    { a shell input tap $1 $2; }
back()   { a shell input keyevent 4; }
shot()   { sleep ${2:-0.6}; a exec-out screencap -p > raw/$1.png; echo "saved raw/$1.png"; }
dump()   { a shell uiautomator dump /sdcard/ui.xml >/dev/null; a shell cat /sdcard/ui.xml | python3 -c "
import sys,re
for m in re.finditer(r'<node[^>]*?text=\"([^\"]*)\"[^>]*?content-desc=\"([^\"]*)\"[^>]*?bounds=\"(\[[^\"]*\])\"', sys.stdin.read()):
    t,d,b=m.groups()
    if t or d: print(repr(t), repr(d), b)
"; }
game_json() {
  local mode=$1 ans=$2 g=${3:-}
  local gl="[]"
  if [ -n "$g" ]; then gl=$(python3 -c "import sys,json;print(json.dumps(sys.argv[1].split(',')))" "$g"); fi
  echo "{\"games\":{\"$mode\":{\"mode\":\"$mode\",\"answer\":\"$ans\",\"guesses\":$gl}}}"
}
# tapText <exact text or content-desc>
tapText() {
  a shell uiautomator dump /sdcard/ui.xml >/dev/null
  local xy=$(a shell cat /sdcard/ui.xml | python3 -c "
import sys,re
want=sys.argv[1]
for m in re.finditer(r'<node[^>]*?text=\"([^\"]*)\"[^>]*?content-desc=\"([^\"]*)\"[^>]*?bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', sys.stdin.read()):
    t,d,x1,y1,x2,y2=m.groups()
    if t==want or d==want:
        print((int(x1)+int(x2))//2,(int(y1)+int(y2))//2); break
" "$1")
  if [ -z "$xy" ]; then echo "tapText: '$1' not found"; return 1; fi
  a shell input tap $xy; sleep 0.8
}
# play <answer> <guesses csv>: stage a 5-letter game, launch, type answer, submit
play5() { stop; stage "$(game_json GUESS_5_ENGLISH $1 "$2")"; launch; type_ $1; enter; sleep 1.2; }
