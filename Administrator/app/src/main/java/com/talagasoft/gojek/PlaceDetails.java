package com.talagasoft.gojek;

import com.google.android.gms.location.places.Place;
import com.google.api.client.util.Key;

import java.io.Serializable;

/**
 * Created by compaq on 03/06/2016.
 */
public class PlaceDetails implements Serializable {

    @Key
    public String status;

    @Key
    public Place result;

    @Override
    public String toString() {
        if (result!=null) {
            return result.toString();
        }
        return super.toString();
    }
}
