# Generate Android style XML values for a given hue
# Copyright (c) 2015 IRCCloud, Ltd.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import colorsys

if len(sys.argv) != 4:
    print "Usage: " + sys.argv[0] + " name hue saturation"
    sys.exit(1)

script, themename, hue, saturation = sys.argv

def color(name, lightness):
    rgb = colorsys.hls_to_rgb(float(hue)/360.0, float(lightness), float(saturation))
    return "\t<color name=\"" + themename + "_" + name + "\">#%0.2X%0.2X%0.2X</color>\n" % (rgb[0] * 255, rgb[1] * 255, rgb[2] * 255)
    
def style(name, value):
    return "\t\t<item name=\"" + name + "\">@color/" + themename + "_" + value + "</item>\n"
    
def theme(f, t, parent):
    f.write("\t<style name=\"" + t + "\" parent=\"" + parent + "\">\n")
    f.write("\t\t<item name=\"isDarkTheme\">true</item>\n")
    f.write(style("bufferTextColor", "text4"))
    f.write(style("navBarSubheadingColor", "text6"))
    f.write(style("inactiveBufferTextColor", "text9"))
    f.write(style("archivesHeadingTextColor", "text9"))
    f.write(style("chatterBarTextColor", "text5"))
    f.write(style("linkColor", "text1"))
    f.write(style("highlightBackgroundColor", "background5"))
    f.write(style("selfBackgroundColor", "background6"))
    f.write(style("contentBackgroundColor", "background7"))
    f.write(style("dialogBackgroundColor", "background6"))
    f.write(style("messageTextColor", "text4"))
    f.write(style("serverBackgroundColor", "background6"))
    f.write(style("bufferBackgroundColor", "background4"))
    f.write(style("timestampColor", "text10"))
    f.write(style("timestampBackgroundColor", "background6"))
    f.write(style("statusBackgroundColor", "background6a"))
    f.write(style("noticeBackgroundColor", "background5a"))
    f.write(style("collapsedRowTextColor", "text7"))
    f.write(style("collapsedHeadingBackgroundColor", "background6"))
    f.write(style("navBarColor", "background5"))
    f.write(style("navBarBorderColor", "border10"))
    f.write(style("textareaTextColor", "text3"))
    f.write(style("textareaBackgroundColor", "background3"))
    f.write(style("navBarHeadingColor", "text1"))
    f.write(style("lightLinkColor", "text9"))
    f.write(style("unreadBufferTextColor", "text2"))
    f.write(style("selectedBufferTextColor", "border8"))
    f.write(style("selectedBufferBackgroundColor", "text3"))
    f.write(style("bufferBorderColor", "border9"))
    f.write(style("serverBorderColor", "border9"))
    f.write(style("failedServerBorderColor", "border9"))
    f.write(style("selectedBufferBorderColor", "text6"))
    f.write(style("backlogDividerColor", "border1"))
    f.write(style("chatterBarColor", "background3"))
    f.write(style("awayBarTextColor", "text7"))
    f.write(style("awayBarColor", "background6"))
    f.write(style("connectionBarTextColor", "text4"))
    f.write(style("connectionBarColor", "background4"))
    f.write(style("timestampTopBorderColor", "border9"))
    f.write(style("timestampBottomBorderColor", "border6"))
    f.write(style("placeholderColor", "text12"))
    f.write(style("expandCollapseIndicatorColor", "text12"))
    f.write(style("bufferHighlightColor", "background5"))
    f.write(style("selectedBufferHighlightColor", "text4"))
    f.write(style("archivedBufferHighlightColor", "background5"))
    f.write(style("selectedArchivedBufferHighlightColor", "text6"))
    f.write(style("selectedArchivedBufferBackgroundColor", "text6"))
    f.write(style("listItemBackgroundColor", "background5"))
    f.write(style("backlogMarkerColor", "border1"))
    
    f.write(style("colorPrimary", "background5"))
    f.write(style("colorPrimaryDark", "background7"))
    f.write(style("colorAccent", "text8"))
    f.write(style("colorControlNormal", "text6"))
    f.write(style("colorControlActivated", "text4"))
    f.write(style("colorSwitchThumbNormal", "text6"))
    f.write(style("android:colorPrimary", "background5"))
    f.write(style("android:colorPrimaryDark", "background7"))
    f.write(style("android:colorAccent", "text8"))
    f.write(style("android:colorControlNormal", "text6"))
    f.write(style("android:colorControlActivated", "text4"))
    f.write(style("android:colorForeground", "text6"))
    f.write(style("android:textColorPrimary", "text1"))
    f.write(style("android:textColorSecondary", "text6"))
    f.write(style("android:colorEdgeEffect", "text6"))
    
    f.write("\t\t<item name=\"actionbarDrawable\">@drawable/actionbar_" + themename + "</item>\n")
    f.write("\t\t<item name=\"buffersDrawerBackgroundDrawable\">@drawable/buffers_drawer_bg_" + themename + "</item>\n")
    f.write("\t\t<item name=\"bufferBorderDrawable\">@drawable/bufferBorderDrawable_" + themename + "</item>\n")
    f.write("\t\t<item name=\"serverBorderDrawable\">@drawable/serverBorderDrawable_" + themename + "</item>\n")
    f.write("\t\t<item name=\"selectedBorderDrawable\">@drawable/selectedBorderDrawable_" + themename + "</item>\n")
    f.write("\t\t<item name=\"bufferBackgroundDrawable\">@drawable/row_buffer_bg_" + themename + "</item>\n")
    f.write("\t\t<item name=\"serverBackgroundDrawable\">@drawable/row_server_bg_" + themename + "</item>\n")
    f.write("\t\t<item name=\"selectedBackgroundDrawable\">@drawable/selectedBackgroundDrawable_" + themename + "</item>\n")
    f.write("\t\t<item name=\"lastSeenEIDBackgroundDrawable\">@drawable/row_lastseeneid_bg_" + themename + "</item>\n")
    f.write("\t\t<item name=\"timestampBackgroundDrawable\">@drawable/row_timestamp_bg_" + themename + "</item>\n")
    f.write("\t\t<item name=\"socketclosedBackgroundDrawable\">@drawable/row_socketclosed_bg_" + themename + "</item>\n")
    f.write("\t\t<item name=\"android:dialogTheme\">@style/" + themename + "Dialog</item>\n")
    f.write("\t\t<item name=\"dialogTheme\">@style/" + themename + "Dialog</item>\n")
    f.write("\t\t<item name=\"android:alertDialogTheme\">@style/" + themename + "Alert</item>\n")
    f.write("\t\t<item name=\"alertDialogTheme\">@style/" + themename + "Alert</item>\n")
    f.write("\t\t<item name=\"scrollbarDrawable\">@drawable/scrollbar_" + themename + "</item>\n")
    f.write("\t\t<item name=\"windowBackgroundDrawable\">@drawable/windowBackground_" + themename + "</item>\n")
    f.write("\t\t<item name=\"editTextTheme\">@style/" + themename + "EditText</item>\n")
    
    f.write("\t</style>\n\n")
    
LICENSE = """<!--
~ Copyright (c) 2015 IRCCloud, Ltd.
~
~ Licensed under the Apache License, Version 2.0 (the \"License\");
~ you may not use this file except in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an \"AS IS\" BASIS,
~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~ See the License for the specific language governing permissions and
~ limitations under the License.
-->\n"""

params = {
    "license":LICENSE,
    "theme":themename
}

print "Geneating theme: " + themename

f = open("themes/values/theme_" + themename + ".xml", "w")
f.truncate()
f.write(LICENSE)
f.write("\n<resources>\n")

theme(f, themename, "DarkAppTheme")
theme(f, themename + "NoActionBar", "DarkAppThemeNoActionBar")
theme(f, themename + "Dialog", "DarkAppDialogTheme")
theme(f, themename + "Alert", "DarkAppAlertTheme")
theme(f, themename + "DialogWhenLarge", "DarkAppDialogWhenLargeTheme")

f.write(color("border1", 0.55))
f.write(color("border2", 0.50))
f.write(color("border3", 0.45))
f.write(color("border4", 0.40))
f.write(color("border5", 0.35))
f.write(color("border6", 0.30))
f.write(color("border7", 0.25))
f.write(color("border8", 0.20))
f.write(color("border9", 0.15))
f.write(color("border10", 0.10))
f.write(color("border11", 0.05))

f.write(color("text1", 1.0))
f.write(color("text2", 0.95))
f.write(color("text3", 0.90))
f.write(color("text4", 0.85))
f.write(color("text5", 0.80))
f.write(color("text6", 0.75))
f.write(color("text7", 0.70))
f.write(color("text8", 0.65))
f.write(color("text9", 0.60))
f.write(color("text10", 0.55))
f.write(color("text11", 0.50))
f.write(color("text12", 0.45))

f.write(color("background0", 0.50))
f.write(color("background1", 0.45))
f.write(color("background2", 0.40))
f.write(color("background3", 0.35))
f.write(color("background4", 0.30))
f.write(color("background5", 0.25))
f.write(color("background5a", 0.23))
f.write(color("background6", 0.20))
f.write(color("background6a", 0.17))
f.write(color("background7", 0.15))

f.write("\t<drawable name=\"bufferBackgroundDrawable_" + themename + "\">@color/" + themename + "_background4</drawable>\n")
f.write("\t<drawable name=\"serverBackgroundDrawable_" + themename + "\">@color/" + themename + "_background6</drawable>\n")
f.write("\t<drawable name=\"bufferBorderDrawable_" + themename + "\">@color/" + themename + "_border9</drawable>\n")
f.write("\t<drawable name=\"serverBorderDrawable_" + themename + "\">@color/" + themename + "_border9</drawable>\n")
f.write("\t<drawable name=\"selectedBackgroundDrawable_" + themename + "\">@color/" + themename + "_text3</drawable>\n")
f.write("\t<drawable name=\"selectedBorderDrawable_" + themename + "\">@color/" + themename + "_text6</drawable>\n")
f.write("\t<drawable name=\"windowBackground_" + themename + "\">@color/" + themename + "_background7</drawable>\n")

f.write("""
\t<style name="{theme}PrefsTheme" parent="{theme}NoActionBar">
\t\t<item name="editTextTheme">@style/{theme}EditText</item>
\t\t<item name="android:windowNoTitle">false</item>
\t\t<item name="android:windowTitleSize">?attr/actionBarSize</item>
\t\t<item name="android:windowTitleBackgroundStyle">@style/PrefsWindowTitleBackground</item>
\t</style>
""".format(**params))

f.write("""
\t<style name="{theme}EditText" parent="{theme}">
\t\t<item name="android:colorControlActivated">?colorControlNormal</item>
\t\t<item name="colorControlActivated">?colorControlNormal</item>
\t</style>
""".format(**params))

f.write("</resources>\n")
f.close()

f = open("themes/drawable/actionbar_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
\t<item>
\t\t<shape android:shape="rectangle" >
\t\t\t<solid android:color="@color/{theme}_border10" />
\t\t</shape>
\t</item>
\t<item android:bottom="2dp">
\t\t<shape android:shape="rectangle" >
\t\t\t<solid android:color="@color/{theme}_background5" />
\t\t</shape>
\t</item>
</layer-list>
""".format(**params))
f.close()

f = open("themes/drawable/buffers_drawer_bg_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
\t<item>
\t\t<shape android:shape="rectangle" >
\t\t\t<solid android:color="@color/{theme}_border9" />
\t\t</shape>
\t</item>
\t<item android:left="8dp">
\t\t<shape android:shape="rectangle" >
\t\t\t<solid android:color="@color/{theme}_background4" />
\t\t</shape>
\t</item>
</layer-list>
""".format(**params))
f.close()

f = open("themes/drawable/row_buffer_bg_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<selector xmlns:android="http://schemas.android.com/apk/res/android" >
    <item android:drawable="@color/{theme}_background2" android:state_pressed="true"/>
    <item android:drawable="@color/{theme}_text3" android:state_selected="true"/>
    <item android:drawable="@color/{theme}_background2" android:state_focused="true"/>
    <item android:drawable="@color/{theme}_background4"/>
</selector>
""".format(**params))
f.close()

f = open("themes/drawable-v21/row_buffer_bg_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?android:attr/colorControlHighlight">
    <item android:drawable="@drawable/bufferBackgroundDrawable_{theme}" />
</ripple>
""".format(**params))
f.close()

f = open("themes/drawable/row_server_bg_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<selector xmlns:android="http://schemas.android.com/apk/res/android" >
    <item android:drawable="@color/{theme}_background2" android:state_pressed="true"/>
    <item android:drawable="@color/{theme}_text3" android:state_selected="true"/>
    <item android:drawable="@color/{theme}_background2" android:state_focused="true"/>
    <item android:drawable="@color/{theme}_background6"/>
</selector>
""".format(**params))
f.close()

f = open("themes/drawable-v21/row_server_bg_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?android:attr/colorControlHighlight">
    <item android:drawable="@drawable/serverBackgroundDrawable_{theme}" />
</ripple>
""".format(**params))
f.close()

f = open("themes/drawable/row_lastseeneid_bg_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle" >
            <solid android:color="@color/{theme}_background7" />
        </shape>
    </item>
    <item android:left="4dp" android:right="4dp">
        <shape android:shape="line" >
            <stroke android:color="@color/{theme}_text10" android:width="1dp"/>
        </shape>
    </item>
</layer-list>
""".format(**params))
f.close()

f = open("themes/drawable/row_timestamp_bg_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
  <item>
    <shape android:shape="rectangle" >
      <solid android:color="@color/{theme}_border9" />
    </shape>
  </item>
  <item android:top="1dp">
    <shape android:shape="rectangle" >
      <solid android:color="@color/{theme}_border6" />
    </shape>
  </item>
  <item android:bottom="3dp" android:top="1dp">
    <shape android:shape="rectangle" >
      <solid android:color="@color/{theme}_background6" />
    </shape>
  </item>
</layer-list>
""".format(**params))
f.close()

f = open("themes/drawable/row_socketclosed_bg_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle" >
            <solid android:color="@color/{theme}_background7" />
        </shape>
    </item>
    <item android:top="-2dp" android:left="8dp" android:right="8dp">
        <shape android:shape="line" >
            <stroke android:color="@color/{theme}_text10" android:width="1dp"/>
        </shape>
    </item>
    <item android:top="2dp" android:left="8dp" android:right="8dp">
        <shape android:shape="line" >
            <stroke android:color="@color/{theme}_text10" android:width="1dp"/>
        </shape>
    </item>
</layer-list>
""".format(**params))
f.close()

f = open("themes/drawable/scrollbar_" + themename + ".xml", "w")
f.truncate()
f.write("""<?xml version="1.0" encoding="utf-8"?>
{license}
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="@dimen/scrollbar_inset"
    android:insetRight="@dimen/scrollbar_inset"
    android:insetTop="@dimen/scrollbar_inset"
    android:insetBottom="@dimen/scrollbar_inset">
    <shape>
        <corners android:radius="4dp" />
        <solid android:color="@color/{theme}_text4" />
    </shape>
</inset>
""".format(**params))
f.close()
