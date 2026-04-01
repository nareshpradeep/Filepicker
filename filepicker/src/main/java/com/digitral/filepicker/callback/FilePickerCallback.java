package com.digitral.filepicker.callback;

import android.content.Intent;

import java.io.Serializable;

public interface FilePickerCallback extends Serializable {

    long serialVersionUID = 1L;

    void onResult(int requestId, int status, Intent response,Object returnObject);
}
