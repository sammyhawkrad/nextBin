package com.sammyhawkrad.nextbin;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DataViewModel extends ViewModel {

    private final MutableLiveData<String> jsonData = new MutableLiveData<>();

    public void setJsonData(String data) {
        jsonData.setValue(data);
    }

    public MutableLiveData<String> getJsonData() {
        return jsonData;
    }
}

