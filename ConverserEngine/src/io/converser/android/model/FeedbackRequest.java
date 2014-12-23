package io.converser.android.model;

public class FeedbackRequest {

    private int reaction;
    private String area;
    private String text;

    public FeedbackRequest(int reaction, String area, String text) {
        super();
        this.reaction = reaction;
        this.area = area;
        this.text = text;
    }

    public int getReaction() {
        return reaction;
    }

    public void setReaction(int reaction) {
        if (reaction < 1) {
            reaction = 1;
        }

        if (reaction > 5) {
            reaction = 5;
        }

        this.reaction = reaction;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }


}
