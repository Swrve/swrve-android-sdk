package com.swrve.sdk.converser.engine.model;

import java.util.HashMap;

/**
 * Created by shanemoore on 08/01/2015.
 */
public class ConverserInputResult{
    public String type;
    public Object result;

    public String getType()
    {
        return type;
    }

    public String getResultAsString(){
        return result.toString();
    }
}
