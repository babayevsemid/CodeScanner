# CodeScanner

### Installation

Add this to your ```build.gradle``` file

```
repositories {
    maven {
        url "https://jitpack.io"
    }
}

dependencies {
    implementation 'com.github.babayevsemid:CodeScanner:1.0.5'
      
}
```

### Add XML
```
<com.semid.qrcodescanner.CodeScannerView
        android:id="@+id/scannerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" /> 
```

### Use in activity

```
scannerView.init(this)

scannerView.onResult = {
    //it result
}

scannerView.cameraPermission = {
    //it == true permission granted

    //if permission denied request again 
    scannerView.requestCameraPermission(context)
}

``` 

### Use in fragment with check permission

```
scannerView.init(this)

scannerView.onResult = {
    //it result

    // For read next
    scannerView.readNext()
}

```
 
### Scan from image file

```
scannerView.init(this)

scannerView.onResultFromFile = {
    //it result

    if(it.isEmpty){
        //Image read failed
    }

    // For read next
    scannerView.readNext()
}

// From image path
scannerView.scanFromPath(path)

// From bitmap
scannerView.scanFromBitmap(bitmap)

// From image uri
scannerView.scanFromUri(uri)

```


### Use in Java

```
scannerView.init(this)

scannerView.onResult = result -> { 
    //result

    // For read next
    scannerView.readNext();

    return null;
}

```

### Extra
```
scannerView.setBarcodeFormats(arrayListOf(BarcodeFormat.FORMAT_QR_CODE)) //Only QrCode

scannerView.torchState = {
    //it Torch ON
}

scannerView.changeTorchState() // if torch is off change to on

scannerView.isEnabledTorch() //check torch status
 ```
 
 ### Attributes
  ``` 
  csvMaskColor //Default #77000000
  csvVibratorDuration // Default: 100ms
  csvFrameColor // Default: white
  csvFrameThickness // Default: 4dp
  csvFrameCornersSize // Default: 40dp
  csvFrameCornersRadius // Default: 10dp
  csvFrameAspectRatioWidth // Default: 1
  csvFrameAspectRatioHeight // Default: 1
  csvFrameSize // Default: width percent of screen 0.75
  csvLazerHeight // Default: 1dp
  csvLazerColor // Default: white
  csvLazerVisible // Default: true
  csvLazerAnimDuration // Default: 2000ms
  csvLazerAnim // linear or fade. Default: linear
   ```
