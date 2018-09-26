package com.talagasoft.gojek;

import com.google.api.client.util.Key;

import java.io.Serializable;
import java.util.List;

/**
 * Created by compaq on 03/06/2016.
 */
public class PlacesList  implements Serializable {

    @Key
    public String status;

    @Key
    public List<Place> results;

}