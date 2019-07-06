### 4.0.0 (2019-7-06)
* update buildtools
* redesign UI
* implement fragments
* restructure code
* simplify information
* debug log mode

### 3.3.0 (unreleased)
* update buildtools
* new colour UI
* new icons
* prep for ver 4.0
* moved to Archive branch

### 3.2.0 (2019-6-14)
* add ignore doze function
* fix toast token exception
* add receivers list to SDK name check routine
* more info about App Summary checks for ui
* remove protected modifier as appears to cause crashes in some handsets
* activity scope bugfix
* add new SDK names

### 3.1.2 (2019-5-22)
* add UI wording for false positives, caveats etc
* Voice Interactive Advert addition to SDK list
* new icons to adhere to Google Play spec changes

### 3.1.1 (2019-3-07)
* bugfix for AUDIO_RECORD permission deny handling
* clarity of rationale for permission request

### 3.1.0 (2019-2-27)
* add buffer read lock option to About dialog, defaults to off. Calls audioRecord.read(buffer, ...) for a status report, not audio data.
* Android build code: 17

### 3.0.4 (unreleased)
* add audioRecord.read(buffer,...) boolean switch
* test debug messages option
* audioRecord null bug
* add metadata folder for FDroid

### 3.0.3 (2019-1-29)
* active jammer cleanup
* background checker bugfix

### 3.0.2 (2018-12-16)
* general clean up
* bugfix for bad user input
* add closeApp for no permissions
* permission check fix
* added to SDK search list

### 3.0.1 (2018-12-09)
* passive jammer as a service
* active jammer as a service
* change AudioSettings data to Bundle
* move superfluous UI statements behind debug switch
* Android build code: 14

### 2.2.6 (unreleased)
* add ACR package scan and information
* update AndroidX libs
* notification state fix
* Android build code: 13

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