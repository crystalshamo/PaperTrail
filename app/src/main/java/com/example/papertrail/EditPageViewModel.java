package com.example.papertrail;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EditPageViewModel extends ViewModel {
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>();

    public EditPageViewModel() {
        // Initialize current page to 1 (or another default value)
        currentPage.setValue(1);
    }

    public LiveData<Integer> getCurrentPage() {
        return currentPage;
    }

    public void incrementPage() {
        Integer current = currentPage.getValue();
        if (current != null) {
            currentPage.setValue(current + 1);
        }
    }

    public void decrementPage() {
        Integer current = currentPage.getValue();
        if (current != null) {
            currentPage.setValue(current - 1);
        }
    }

    public void setPage(int pageNumber) {
        currentPage.setValue(pageNumber);
    }
}

