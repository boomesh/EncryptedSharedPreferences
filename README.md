# EncryptedSharedPreferences

## Purpose
- this library gives you an out of the box solution for encrypting values in a Shared Preferences file via the Android Keystore
- no need to worry about managing/generating keys in your app, just plug'n'play

## Requirements
- Minimum of API 21 (Android Lollipop), [approximately 85% of all devices](https://developer.android.com/about/dashboards/index.html)
- All the requirements of what the Android Keystore requires
- uses `com.android.support.appcompat` (androidX is coming in the future!)

## Why use this library?
- it's a very small library with very few dependencies
  - Comparatively [Armadillo](https://github.com/patrickfav/armadillo) is a really good library, and really feature rich.  But you might not use all of those features, and it would bulk up your app
- The AndroidX SDK Team will be releasing their [EncryptedSharedPreferences](https://developer.android.com/topic/security/data.md#classes-in-library), it's currently in [alpha](https://developer.android.com/jetpack/androidx/releases/security#1.0.0-alpha01), and I expect it to eventually replace this repo
  - the API for this library matches the API for AndroidX's library, so when it's ready you could switch to using this with minimal friction 
  - [here's AndroidX's source](https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-master-dev/security/crypto/src/main/java/androidx/security/crypto/EncryptedSharedPreferences.java)

## What does it do
- everything the [`android.content.SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences) does with an added layer of encryption on top
- the encryption under the hood relies on the Keystore (so your keys are _mostly_ managed by the OS keystore process<sup>1</sup>)

## Examples 
### Initialize
```kotlin
val prefs = EncryptedSharedPreferences.create(filename = "default", context)
```

### Store a String
```kotlin
val editor = prefs.edit()
editor.putString("KEY", "value")
editor.apply()
```

### Get a String
```kotlin
prefs.getString("KEY", "default value")
```

# Plans for the Future
- replace support libs with androidx
- introduce a mechanism for migrating from current shared prefs, to androidx shared prefs
- perhaps some instrumentation tests?


# Caveats
1. for below API 23 (Android Marshmallow), Symmetric Encryption is not supported out of the box.  A key is generated, and stored in shared preferences (asymmetrically encrypted)