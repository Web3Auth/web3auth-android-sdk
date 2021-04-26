# openlogin-android-sdk

Torus OpenLogin SDK for Android applications

## Start integrating

Before you can start integrating OpenLogin in your own app, you must configure a OpenLogin project
in [Developer Dashboard](https://developer.tor.us) and set up your Android Studio project. The steps
on this page do just that. The next steps then describe how to integrate OpenLogin into your app.

### Prerequisites

OpenLogin for Android has the following requirements:

- A compatible Android device that runs Android 7.0 or newer or an emulator with an AVD that runs
based on Android 7.0 or newer.

- The latest version of the Android SDK, including the SDK Tools component. The SDK is available
from the Android SDK Manager in Android Studio.

- A project configured to compile against Android 7.0 (API level 24) or newer.

This guide is written for users of Android Studio, which is the recommended development environment.

### Add Jitpack repository

In your project's top-level `build.gradle` file, ensure that Google's Maven repository is included:

```groovy
allprojects {
    repositories {
        // ...
        maven { url "https://jitpack.io" }
    }
}
```

Then, in your app-level `build.gradle` file, declare OpenLogin as a dependency:

```groovy
plugins {
    id 'com.android.application'
}

// ...

dependencies {
    implementation 'com.openlogin:core:1.0.0'
}
```

### Configure an OpenLogin project

Go to [Developer Dashboard](https://developer.tor.us) and create an OpenLogin project.

## Add Sign-In

Add Sign-In button(s) to your app's layout that starts different sign-in flows that's appropriate
for your app.

## Create an OpenLogin client

In your sign-in activity's `onCreate` method, create an `OpenLogin` object with your OpenLogin
project's configurations.

```kotlin
openlogin = OpenLogin(this, "YOUR CLIENT ID")
```