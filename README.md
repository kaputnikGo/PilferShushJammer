# PilferShush Jammer
Android microphone checker and passive jamming application.  

Test application for low battery requirement microphone jamming.

Calls audioRecord.startRecording() but DOES NOT READ THE AUDIO BUFFER.

Claims audio focus and should block user apps from gaining focus of microphone.

System apps (telephony) should override and bump the Jammer from the microphone. 

Adds a notification as a reminder for running while in background 


   vers. 1.0.02
   - min API 18 (4.3)
   - target API 23 (6.x)
   - compiled API 26 (8.x)

   testing devices
   - LOW : s4 I9195 (deprecated) 4.3.1 (18)(CyanogenMod 10.2, F-Droid)
   - SLO : Mts 5045D (tainted) 6.0.1 (23) (CyanogenMod 13.0, GApps)
   - DEV : s5 G900I (tainted) 7.1.2 (25)(LineageOS 14.1, GApps)
 
 TODO:
 - full and proper testing
 - telephony response
 - user app whitelist
 - active jamming

# 2018 Kaputnik Go