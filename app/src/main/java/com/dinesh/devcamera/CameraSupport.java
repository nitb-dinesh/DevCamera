package com.dinesh.devcamera;

/**
 * Created by Dinesh on 7/10/2017.
 */

public interface CameraSupport {
    CameraSupport open(int cameraId);
    int getOrientation(int cameraId);
}
