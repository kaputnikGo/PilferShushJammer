### 4.5.3 (unreleased)
* android manifest config for screen off display rotate
* audioManager property support near ultrasound check

### 4.5.2 (2020-10-16)
* bugfix for adding already added fragment
* mediaRecord check catch for exception on some devices
* revert to DEFAULT as VOICE_COMM may cause Process_VoIP
* added Settings switch for VOICE_COMM or DEFAULT
* notes in AudioSettings.java

### 4.5.1 (2020-10-13)
* micInfoList for multiple mics print
* change Inspector to caution for AudioRecord with SDK name
* improve and increase info from Inspector
* move micInfo check to Home
* appEntry dialog view
* update gradle 6.5, Android Studio 4.1
* permissions bugfix for mic info check

### 4.5.0 (2020-10-09)
* added passive control appwidget in prep for Android 11
* add boot receiver for auto restart app at device reboot
* add receive boot permission
* androidx and deps update
* mediaRecorder source switched to DEFAULT
* updates to Spanish translation
* add AudioRecord.Builder in prep for Android 11
* API28 microphoneInfo list enum
* API check for requestAudioFocus check method
* Inspector Fragment has mic info check print
* remove Color.Accent resource need
* add chromecast as NUHF SDK
* add ActiveJammer shadow method
* cleaning up Active jammer code mess
* removed click artifacts from NUHF tone active jamming
* add sine/sqr/saw waveforms
* config optional AudioFX equaliser to NUHF preset

### 4.4.2 (2020-8-26)
* upgrade build target to API 29, Google Play comply
* update buildtools, platform-tools
* remove lockscreen notify as can cause dupe activity
* getActivity npe bugfix
* auto-backup to false
* remove old drawable xml, possible ex cause
* update gradle dep
* add new SDK
* prep code for Android 11

### 4.4.1 (2020-7-12)
* Android 10 concurrent audio mitigation test build
* centered popup toast warning for concurrent audio capture state
* passive jammer auto restart during C-1-4 conflict 
* caution text to system YELLOW
* mediaRecordPlacebo to VOICE_COMM source
* browser intent ex null
* add accessibility service permission to background check
* updated Inspector and Readme text

### 4.4.0 (2020-6-27)
* bugfix - add context for entryLogger
* language clarity
* Spanish translation
* add README dialog to app for detailed more info and link to project page
* test Android 10 <https://source.android.com/compatibility/android-cdd#5_4_5_concurrent_capture>
* audio source set to VOICE_COMMUNICATION (5.4.5 `[C-1-3]` silence other app while VOICE_COMM)
* for API >= 29 setAllowedCapturePolicy to ALLOW_CAPTURE_BY_NONE (for VoIP vs VoIP)
* added popup toast warning for concurrent audio capture state

### 4.3.0 (2020-6-13)
* added headset receiver to passive service for autoswitch inputs
* code clean up
* update AndroidStudio 4.0, Gradle
* new SDK name

### 4.2.0 (2020-2-21)
* new SDK names
* MediaRecorder placebo in passive jammer service for API28+
* remove deprecated LocalBroadcastManager
* updated dev device to Android 9

### 4.1.2 (2020-2-03)
* version bump for F-Droid skip
* new SDK name
* update buildtools and AndroidStudio 3.5.3

### 4.1.1 (2019-12-08)
* add scrollview to settings fragment for small screens
* add to SDK names
* remove bad codehelp for perms checker

### 4.1.0 (2019-10-05)
* add notifications toggle for passive jammer off

### 4.0.6 (2019-8-27)
* remove EQ set in checkOnboardEQ as not application specific
* split determineAudio function to only check output if active jammer engaged

### 4.0.5 (2019-7-24)
* changed audio focus loss handling to system

### 4.0.4 (2019-7-18)
* no args construct
* unique notification IDs

### 4.0.3 (2019-7-14)
* permissions bugfix
* audio type check
* code cleanup
* dialog view clean

### 4.0.2 (2019-7-10)
* fix to sensorPortrait

### 4.0.1 (2019-7-09)
* bugfix for audioFocus
* passive jammer mic state notify
* static fragment instantiation

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