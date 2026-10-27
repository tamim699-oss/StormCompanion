# StormCompanion

Android companion prototype for the Nasir Syntax Storm 1500W electric scooter.

## Current status

This project currently focuses on Bluetooth Low Energy (BLE) device discovery and GATT service inspection. It is a read-only prototype; it does not yet decode scooter telemetry or send control commands.

## Safety note

Do not use this app to change speed modes, lock the scooter, or send undocumented BLE commands. The automatic mode-switching issue must be diagnosed against the scooter controller/app protocol before any control feature is implemented.

## Build

Open this project in Android Studio with Android SDK 35 and Java 17, or use the included GitHub Actions workflow if configured.
