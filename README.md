# PilferShush Jammer
Android microphone checker and jamming application.  

Test application for low battery requirement microphone passive jamming.

Calls audioRecord.startRecording() but DOES NOT READ THE AUDIO BUFFER.

Claims audio focus and should block user apps from gaining focus of microphone.

System apps (telephony) should override and bump the Jammer from the microphone. 

Adds a notification as a reminder for running while in background.

Responds (stop and restart jammer) to telephony audio focus LOSS_TRANSIENT and GAIN.

Handle music player audio focus gain.

Tested and blocks Google Voice search (user) app.

Added active jammer - tone and white noise versions, boost EQ for higher amplitude.

   vers. 2.0.11
   - min API 18 (4.3)
   - target API 23 (6.x)
   - compiled API 26 (8.x)

   testing devices
   - EMU : Galaxy Nexus 4.3 (18) (Android Studio AVD, no GApps)
   - EMU : Nexus 4 5.1 (22) (Android Studio AVD, no GApps)
   - EMU : Nexus 5X 7.0 (24) (Android Studio AVD, GApps)
   - LOW : s4 I9195 (deprecated) 4.3.1 (18)(CyanogenMod 10.2, F-Droid)
   - SLO : Mts 5045D (tainted) 6.0.1 (23) (CyanogenMod 13.0, GApps)
   - DEV : s5 G900I (tainted) 7.1.2 (25)(LineageOS 14.1, GApps)
   - PROD: s5 G900P 7.1.2 (25) (LineageOS 14.1, F-Droid)
 
 TODO:
 - app behaviours with active/passive jammers running
 - full and proper testing ( incl. VOIP )
 
 
 
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



# 2018 Kaputnik Go


Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.