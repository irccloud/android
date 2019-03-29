#!/bin/bash
$(which adb) -s emulator-5580 emu kill
sleep 5
$(which emulator) -avd $1 -ports 5580,5581 -no-snapshot -no-boot-anim > /dev/null 2>&1 &
sleep 20
$(which adb) shell settings put global sysui_demo_allowed 1
$(which adb) shell am broadcast -a com.android.systemui.demo -e command enter
$(which adb) shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900
$(which adb) shell am broadcast -a com.android.systemui.demo -e command network -e nosim hide
$(which adb) shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype 4g -e level 4 -e fully true
$(which adb) shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
$(which adb) shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
$(which adb) shell pm clear com.irccloud.android.mockdata
