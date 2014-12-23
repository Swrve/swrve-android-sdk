package io.converser.android.model;


public class OptInOutRequest {

    private boolean choice;

    public OptInOutRequest(boolean c) {
        this.choice = c;
    }

    public boolean getChoice() {
        return choice;
    }

    public void setChoice(boolean c) {
        this.choice = c;
    }

}
