package com.swrve.sdk;

public class SwrveABTestDetails {

    private final String id;
    private final String name;
    private final int caseIndex;

    protected SwrveABTestDetails(String id, String name, int caseIndex) {
        this.id = id;
        this.name = name;
        this.caseIndex = caseIndex;
    }

    /**
     * @return the id of the AB Test the user is part of.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the name of the AB Test the user is part of.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the index of the AB Test variant the user was assigned to.
     */
    public int getCaseIndex() {
        return caseIndex;
    }
}
