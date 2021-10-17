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
implementation 'com.github.tgobbens:Easyimagepicker:1.0.10'
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
            .mode(EasyImagePicker.MODE.BOTH)
            .create(savedInstanceState)
    }
```

Call onSaveInstanceState

```kotlin
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        easyImagePicker.onSaveInstanceState(outState)
    }
```

To show the dialog/camera/gallery, call the `start` method 

```kotlin
    easyImagePicker.start(this)
```

If a result is ready, this will be received in the `onActivityResult`, call `handleActivityResult` to handle 
this, if `true` is returned an uri to the image is available and can be get using `getResultImageUri`

```kotlin
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (easyImagePicker.handleActivityResult(requestCode, resultCode, data, requireActivity())) {
            val uri = easyImagePicker.getResultImageUri()
        
            // do something with this result    
        }
    }
```

If your app has the permission `android.permission.CAMERA` is in the merged manifest, the permission 
`android.permission.CAMERA` should be granted when using the camera. The library will request this 
permission, to make this function correctly implemented the `onRequestPermissionsResult`.

```kotlin
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        easyImagePicker.handleOnRequestPermissionsResult(this, requestCode, grantResults)
    }
```

Changing / localize text

The following strings can be override by putting them in your app `strings.xml`

```xml
    <string name="eip_select_from_gallery">Select from gallery</string>
    <string name="eip_use_camera">Camera</string>
    <string name="eip_dialog_title">Choose</string>
    <string name="eip_dialog_cancel">Cancel</string>
```
 

 