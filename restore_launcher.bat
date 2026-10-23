@echo off
REM Puts the Google launcher back as Home (safe to run any time).
adb shell pm enable com.google.android.tvlauncher
adb shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
adb shell input keyevent KEYCODE_HOME
adb shell dumpsys activity activities | findstr /i mFocusedApp
