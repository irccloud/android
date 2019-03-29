#!/bin/bash
$(which emulator) -avd Nexus_S_API_O -port 5580 > /dev/null 2>&1 &
$(which emulator) -avd Nexus_7_API_O -port 5582 > /dev/null 2>&1 &
$(which emulator) -avd Nexus_10_API_O -port 5584 > /dev/null 2>&1 &
sleep 30
