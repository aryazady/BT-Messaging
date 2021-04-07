package com.bm.messenger.utility;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.bm.messenger.model.LiveDataModel;

public class SharedViewModel extends ViewModel {

    private final MutableLiveData<LiveDataModel> data;

    public SharedViewModel(SavedStateHandle state) {
        data = state.getLiveData("Default");
    }

    public void setData(LiveDataModel data) {
        this.data.setValue(data);
    }

    public LiveData<LiveDataModel> getData() {
        return data;
    }

}
