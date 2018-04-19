# PilferShush Jammer
Android microphone checker and passive jamming application.  

Test application for low battery requirement microphone jamming.
Calls audioRecord.startRecording() but DOES NOT READ THE AUDIO BUFFER.
Claims audio focus and should block user apps from gaining focus of microphone.
System apps (telephony) should override and bump the Jammer from the microphone. 
  


   vers. 1.0.01
   - min API 18 (4.3)
   - target API 23 (6.x)
   - compiled API 26 (8.x)

   testing devices
   - DEV : s5 G900I (tainted) 7.1.2 (25)(LineageOS 14.1, GApps)
 
 TODO:
 - full and proper testing
 - run as a background service
 - telephony response
 - user app whitelist
 - active jamming

# 2018 Kaputnik Go