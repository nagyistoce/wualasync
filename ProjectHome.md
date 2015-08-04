Simple application that continuously synchronizes a Wuala shared folder to Android phone.

This application was written because I wanted to automatically synchronize my music folder in [Wuala](http://www.wuala.com) to my Android phone. Currently there is no full featured Wuala client for android, so here is a lightweight client using Wuala web API.

Looking for help developing this software further! The basic functionality works well-enough for me, need additional help for properly finishing this application: fine-tuning, better GUI, ...

# Features #

Some features provides by the application:
  * Configure Wuala web URL and key as the synchronization source
  * Files are synchronized to the phones external storage device (SD card)
  * Synchronization is only one-way! From Wuala to Android.
  * Synchronization process runs in background, possible to schedule every 30 min, 1 hour, ... Scheduling using Android AlarmManager so it's battery-friendly.
  * Graphical interface displaying synchronization progress and application configuration
  * Does not ask for your Wuala username or password!

Configuration options:
  * Enable service - scheduled background syncronization process
  * Wuala shared folder URL and key
  * Destination folder
  * Allow file deletion - if file is removed from Wuala folder, then also remove from phone
  * Synchronization interval - if service is enabled, then how ofter the synchronization is executed
  * Allow sync only when connected with Wifi

# Installation #

Click the Source tab in this website. There you can download the full source code.
You need Android SDK and Eclipse - [Instructions](http://developer.android.com/guide/developing/eclipse-adt.html)

There is also fully compiled APK file, that should be directly installable to the device. [Google should help with that task](http://www.google.ee/search?q=install+apk+file+on+android)