The official Android app for IRCCloud.com
=======
Chat on IRC from anywhere, and never miss a message.

* All your chats and logs are stored in the cloud. Access them on the go
* Push notifications on highlights and PMs
* Fully syncs with IRCCloud.com on the web
* Works on phones and tablets

Join our #feedback channel on irc.irccloud.com for feedback and suggestions so we can improve the app.
You can also email us on team@irccloud.com or find us on Twitter [@irccloud](https://twitter.com/irccloud).

IRCCloud for Android is available on [Google Play](https://play.google.com/store/apps/details?id=com.irccloud.android).

Screenshots
------
<img src="https://blog.irccloud.com/static/android-announce/sidebar.png" height="640">
&nbsp;
<img src="https://blog.irccloud.com/static/android-announce/keyboard.png" height="640">


Requirements
------
* Android Studio 0.2.x
* Android 4.3 SDK
* Android Support Library, revision 18 or newer
* Google Repository & Google Play service tool, version 32 or new.
* android-websockets library: https://github.com/irccloud/android-websockets
* An Android device running Android 2.2 or newer

Building
------
* Make sure you've installed the Android 4.3 SDK and upgraded to the latest version of Android Studio
* Check out android-websockets and the IRCCloud Android project from github
* Mac/Linux: Launch Android Studio and open the irccloud android folder as a project
* Windows: Import the build.gradle files from both the android and android-websocket projects into a new project
* Android Studio will warn you that "android-websockets" is unrecognized, you can ignore this warning
* Android Studio will notify you that the Android framework has been detected, click on the "Configure" button and then click OK
* Click Build > Make Project, or Run > Run 'android' to automatically deploy the apk to your device

The app can also be built using Gradle from the command-line using the build.gradle file located in the IRCCloud Android project.

_You must uninstall the Play Store version of the app first before installing a debug version, as the signing keys will not match._

License
------
Copyright (C) 2013 IRCCloud, Ltd.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
