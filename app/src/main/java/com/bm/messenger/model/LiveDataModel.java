package com.bm.messenger.model;

import java.io.Serializable;

public class LiveDataModel implements Serializable {

    public static final int NONE = 1;
    public static final int BROADCAST = 2;
    public static final int CONVERSATION = 3;
    public static final int HOME = 4;
    public static final int CAUTION = 5;

    private final String title;
    private final String[] data;
    private final int nextPage;
    private int currPage;

    public LiveDataModel(String title, int currPage) {
        this.title = title;
        this.currPage = currPage;
        this.nextPage = NONE;
        this.data = null;
    }

    public LiveDataModel(String title, int currPage, int nextPage) {
        this.title = title;
        this.currPage = currPage;
        this.nextPage = nextPage;
        this.data = null;
    }

    public LiveDataModel(String title, int currPage, int nextPage, String... data) {
        this.title = title;
        this.currPage = currPage;
        this.nextPage = nextPage;
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public int getCurrPage() {
        return currPage;
    }

    public int getNextPage() {
        return nextPage;
    }

    public void updateCurrPage() {
        if (nextPage != NONE)
            currPage = nextPage;
    }

    public String[] getData() {
        return data;
    }
}
