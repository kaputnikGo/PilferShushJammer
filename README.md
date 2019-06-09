# PilferShush Jammer

Research and project page : https://www.cityfreqs.com.au/pilfer.php

Android microphone checker and jamming application built for AOSP LineageOS.  

Application for low battery requirement microphone passive jamming.

Calls audioRecord.startRecording() but DOES NOT READ THE AUDIO BUFFER.

Holds microphone access and should block user apps from gaining focus of microphone.

System telephone calls will override and bump the Jammer from the microphone. 

Adds a notification as a reminder for running while in background.

Tested and blocks Google Voice search (user) app.

Active jammer - tone and white noise versions, boost EQ for higher amplitude.

Scan user installed apps for key features, possible NUHF/ACR SDK package name matches and services/receivers running.

Jammers run as a foreground service

Build update: compile API 28 (Pie, 9.0), Android Studio 3.2.1 stable

Note: On devices running Android 9 (API level 28) or higher, apps running in the background cannot access the microphone. 
Therefore, your app should record audio only when it's in the foreground or 
**when you include an instance of MediaRecorder in a foreground service.**

**TODO:**
- rebuild the active jammer (testing with Oboe)
- add definitions at start of print for user app summary listings
- user app summary is services only, incl receivers?
- consider user app summary include and print package name of NUHF/ACR if found

- check errors from BackgroundChecker.java:192 (input dispatching timed out)
- consider add DEBUG switch in About dialog
- consider setCameraDisabled(ComponentName admin, boolean disabled) in menu, to run with passive
- optional jammer state persistence over boot


**Changes:**
- toast pop-up fix for exception
- add receivers list to SDK name check routine
- more info about App Summary checks for ui


   vers. 3.1.3
   - min API 18 (4.3)
   - target API 26 (8.x)
   - compiled API 28 (9.x)

   testing devices
   - EMU : Galaxy Nexus 4.3 (18) (Android Studio AVD, no GApps)
   - EMU : Nexus 4 5.1 (22) (Android Studio AVD, no GApps)
   - EMU : Nexus 5X 7.0 (24) (Android Studio AVD, GApps)
   - LOW : s4 I9195 (deprecated) 4.3.1 (18)(CyanogenMod 10.2, F-Droid)
   - SLO : Mts 5045D (tainted) 6.0.1 (23) (CyanogenMod 13.0, GApps)
   - DEV : s5 G900I (tainted) 7.1.2 (25)(LineageOS 14.1, GApps)
   - PROD: s5 G900P 7.1.2 (25) (LineageOS 14.1, F-Droid)
 
 
**Active Jammer frequency analysis:**
- Active tone, full NUHF range with random scatter drift test
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-tone_full_nuhf_scatter_drift-test.jpg" height="182px" />
 
- Active tone, slow speed, limited drift test
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-carrier_limit_drift_speed-test.jpg" height="168px" />

- Active tone, carrier and drift limited, fast
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-carrier_limit_conform-test.jpg" height="164px" />

- Active jammer (19 kHz carrier, 1000 Hz limit, EQ on) versus ramp-up audio beacon-like signal : scatter jamming demo
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-vs-html5_synth.jpg" height="138px" />

**App screenshots:**
- Full app start up (Passive and Active version)
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_jammer_active_startup.png" height="612px" />

- Passive start up (Passive version)
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-start.png" height="612px" />

- About pop up (Passive version)
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-about.png" height="612px" />

- Passive mode running (Passive version)
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-running.png" height="612px" />

- App notification example (Passive version)
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer_notify.png" height="612px" />



# 2019 Kaputnik Go


Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.