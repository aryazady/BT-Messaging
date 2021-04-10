package com.bm.messenger.model;

import java.io.Serializable;

public class ExcludeDestaionModel implements Serializable {

    private String message;
    private String destination;

    public ExcludeDestaionModel(String message, String destination) {
        this.message = message;
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    public String getMessage() {
        return message;
    }
}
