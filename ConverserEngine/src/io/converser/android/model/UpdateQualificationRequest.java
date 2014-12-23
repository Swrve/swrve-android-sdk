package io.converser.android.model;


public class UpdateQualificationRequest {

    private Qualifications add;
    private Qualifications remove;

    public UpdateQualificationRequest() {
        add = new Qualifications();
        remove = new Qualifications();
    }


    /**
     * Include the qualification to be removed as part of the request
     *
     * @param qualToRemove
     */
    public void remove(String qualToRemove) {
        remove.add(qualToRemove);
    }

    /**
     * Include the qualification to be added as part of the request
     *
     * @param qualToAdd
     */
    public void add(String qualToAdd) {
        add.add(qualToAdd);
    }
}
