# PilferShush Jammer

Research and project page : <https://www.cityfreqs.com.au/pilfer.php>

Android microphone checker and jamming application built for AOSP LineageOS.  

Application for low battery requirement microphone passive jamming.

Calls audioRecord.startRecording() but DOES NOT READ THE AUDIO BUFFER.

Holds microphone access and should block user apps from gaining focus of microphone.

System telephone calls will override and bump the Jammer from the microphone. 

Adds a notification as a reminder for running while in background.

Tested and blocks Google Voice search (user) app (up to API 34).
Currently testing Chrome/Omnibox/Assistant app voice blocking.

Active jammer - tone and white noise versions, boost EQ for higher amplitude.

Scan user installed apps for key features, possible NUHF/ACR SDK package name matches and services/receivers running.

Jammers run as an instance of MediaRecorder in a foreground service

Note: Android 11 (API 30) changes to foreground services access to microphone - "while-in-use access" only.
see <https://developer.android.com/about/versions/11/privacy/foreground-services>

Note: Android 12 (API 31) has new concurrent audio capture policy
see <https://source.android.com/compatibility/12/android-12-cdd#54_audio_recording>

Note: APK app bundle will be necessary thing for Google Play Store, apk maintained here for FDroid

Note: Microphone source switch setting from VOICE_COMM to DEFAULT will cause
passive jammer to lose concurrent audio focus and stop blocking the microphone

Notes: For Android 12 (<https://source.android.com/compatibility/12/android-12-cdd?hl=sr#54_audio_recording>)
5.4.5. Concurrent Capture
C-1-1 MUST allow concurrent access to microphone by an accessibility service capturing with AudioSource.VOICE_RECOGNITION
also:
C-1-3 ... another app can capture the voice call if it is a privileged (pre-installed) app with permission CAPTURE_AUDIO_OUTPUT.


**TODO:**
- Quick Setting system tile mic revoke causes app crash (retrigger to service, client silenced)
- restart from above reports service running, it is not.

- API 24 (7.0) minimum: add Quick Settings tile service for passive (req: android.permission.BIND_QUICK_SETTINGS_TILE)
- add sdk name print (link to exodus?) to Inspector dialog if nuhf/acr sdk found
- Assistant jammer requires min API 23 (M) for supportsAssist in xml
- background autostart activity for concurrent audio fight via SYSTEM_ALERT_WINDOW permission
- vs AccessibilityService misuse, in Android 10 (does not work in Go version)
- see : <https://developer.android.com/guide/components/activities/background-starts>
- use NotificationCompat classes in services, check for use
- Android 12(S, API31) adds SensorPrivacyManager for device checks as to addition of MIC toggle


**Build:**
- min API 21 (5.0)
- target API 33 (13.0)
- compiled API 34 (14.0)
- Android Studio 2022.3.1 Patch 2
- Gradle 8.1.2


**Changes:**
- 4.6.3 (Oct, 2023)
- new SDK added
- updated build environment
- min Android SDK requirements build
- post notifications permission added


   testing devices
   - EMU : Galaxy Nexus 4.3 (18) (Android Studio AVD, no GApps)
   - EMU : Nexus 4 5.1 (22) (Android Studio AVD, no GApps)
   - EMU : Nexus 5X 7.0 (24) (Android Studio AVD, GApps)
   - EMU : Galaxy Nexus Oreo (27) (Android Studio AVD, GApps)
   - EMU : Pixel 3a 10.0 (29) (Android Studio AVD, GApps)
   - EMU : Pixel 4a 12.0 (31) (Android Studio AVD, GApps)
   - LOW : s4 I9195 (antique) 7.1.2 (25)( /e/ 0.13, Cleanapk)
   - SLO : Mts 5045D (tainted) 7.1.2 (25) (LineageOS 14.1, GApps)
   - MID : Galaxy Tab 2 GT-P5110 (minimal) 7.1.2 (25) (LineageOS 14.1, F-Droid)
   - DEV : s5 G900I (tainted) 10.0 (29)(LineageOS 17.1, GApps)
   - DEV : s5 G900P (useful) 7.1.2 (25) (LineageOS 14.1, F-Droid)
   - PROD: s10 SM-G977B (nominal) 11.0 (30) (LineageOS 18.0, F-Droid)
 
 
**App screenshots:**
- Home fragment
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-v4_home.jpg" height="612px" />

- Inspector fragment
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-v4_inspector.jpg" height="612px" />

- App Entry Info dialog
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-v4_app_entry.jpg" height="612px" />

- Settings fragment
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-v4_settings.jpg" height="612px" />
 
**Active Jammer frequency analysis:**
- Active tone, full NUHF range with random scatter drift test
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-tone_full_nuhf_scatter_drift-test.jpg" height="182px" />
 
- Active tone, slow speed, limited drift test
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-carrier_limit_drift_speed-test.jpg" height="168px" />

- Active tone, carrier and drift limited, fast
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-carrier_limit_conform-test.jpg" height="164px" />

- Active jammer (19 kHz carrier, 1000 Hz limit, EQ on) versus ramp-up audio beacon-like signal : scatter jamming demo
<img src="https://github.com/kaputnikGo/PilferShushJammer/blob/master/images/PS_Jammer-vs-html5_synth.jpg" height="138px" />

# 2025 Kaputnik Go


Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   <http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
