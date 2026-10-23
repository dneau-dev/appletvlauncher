@echo off
REM 1) Android Studio: Build > Build Bundle(s) / APK(s) > Build APK(s)
REM 2) Run this from the project folder with the TV connected over ADB.
set APK=app\build\outputs\apk\debug\app-debug.apk
if not exist %APK% (echo Build the APK first. & exit /b 1)
adb install -r %APK%
adb shell cmd package set-home-activity com.example.appletvlauncher/.MainActivity
adb shell input keyevent KEYCODE_HOME
adb shell dumpsys activity activities | findstr /i mFocusedApp
