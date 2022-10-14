# Easyimagepicker

An simple and easy to use image picker or open camera and take a picture library. Although this library
does not include some of the more fancy features some other libraries have, it does do the basic well 
and is easy and reliable to use.

# Integration

1. Dependencies:

```groovy
allprojects {
   repositories {
       	maven { url "https://jitpack.io" }
   }
}
```

```groovy
implementation 'com.github.tgobbens:Easyimagepicker:1.2.0'
```

2. Code setup

Add to the activity/fragment 

```kotlin
private lateinit var easyImagePicker: EasyImagePicker
```

Initialise in the activity/fragment `onCreate`.

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        easyImagePicker = EasyImagePicker.Builder()
            .themeResId(R.style.AppThemeDialog)
            .create(savedInstanceState, this) { uri ->
                // do something with this result, can be `null` if nothing was selected
            }
    }
```

Call onSaveInstanceState

```kotlin
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        easyImagePicker.onSaveInstanceState(outState)
    }
```

To show the dialog/camera/gallery, call the `start` method, optionally pass in a mode to only show
the camera, gallery or show a dialog

```kotlin
    easyImagePicker.start()
```

If your app has the permission `android.permission.CAMERA` is in the merged manifest, the permission 
`android.permission.CAMERA` should be granted when using the camera. The library will request this 
permission and handle the result.

Changing / localize text

The following strings can be override by putting them in your app `strings.xml`

```xml
    <string name="eip_select_from_gallery">Select from gallery</string>
    <string name="eip_use_camera">Camera</string>
    <string name="eip_dialog_title">Choose</string>
    <string name="eip_dialog_cancel">Cancel</string>
```
 

 