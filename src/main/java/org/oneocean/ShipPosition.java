package org.oneocean;

import lombok.Data;

/** Simple data-model which will be JSON-encoded */

@Data
public class ShipPosition 
{
    private String name;
    private Integer imo;
    private Float longitude;
    private Float latitude;
    private Float speed;
    private Float course;
    private String navStatus;
    private Float draught;

    public String toStatusString()
    {
        return String.format("%s (%d) Lon: %f Lat: %f Speed: %f Nav: %s", 
                             name, imo, longitude, latitude, speed, navStatus);
    }
}