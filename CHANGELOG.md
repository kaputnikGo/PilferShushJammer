### 2.2.6 (unreleased)
* add ACR package scan and information
* update AndroidX libs

### 2.2.5 (2018-11-12)
* Removed headset receiver as unnecessary for jammer app.
* Prep for possible passive jammer as a foreground service.
* Android build code: 12

### 2.2.2.4 (2018-10-22)
* App store release build version.
* Android build code: 10


### 2.2.2.3 (2018-10-19)
* back button behaviour improvement: audiofocus loss false positive
* reload app icons for updated build paths
* change audio focus LOSS with jammers now ignoring request
* android version code: 9

### 2.2.2 (2018-10-18)
* Back button behaviour changes to account for audio focus loss false positive
* Android build version code: 8

### 2.2.1 (2018-10-16)
* Build update: compile API 28 (Pie, 9.0), Android Studio 3.2.1 stable
* Conform to AndroidX dependencies
* New notifications channel.
* Testing version.

### 2.1.1 (2018-10-15)
* Added user app summary - lists capabilities: record audio, boot, services, receivers and NUHF beacon SDK
* Added user app scanner - lists any receivers and services for a chosen user app
* Move EQ to on by default if EQ available, moved noise switch to options menu
* Android Version Code : 6

### 2.0.13 (2018-7-11)
* Versioning bump and some string minimise. 
* Android versionCode: 5

### 2.0.12 (2018-6-13)
* Bugfixes
* frequency clamping

### 2.0.10 (2018-6-11)
* UI clean up
* eq bugfix
* maximum frequency output checks. 
* App store release numbering.

### 2.0.08 (2018-6-08)
* Bugfix and code clean up
* testing for api 22

### 2.0.06 (2018-5-20)
* Initial public release. 
* Passive jammer accesses microphone and blocks other user apps from using it. 
* Active jammer attempts to block NUHF audio beacon tracker signals by emitting sound between 18 kHz - 24 kHz. Active jammer is experimental.