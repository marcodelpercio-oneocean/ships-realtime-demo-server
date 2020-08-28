package org.oneocean;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;
import com.grum.geocalc.Coordinate;
import com.grum.geocalc.EarthCalc;
import com.grum.geocalc.Point;

import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;

@Startup
@ApplicationScoped
public class PositionDataGenerator {
    private static final Logger LOG = Logger.getLogger(PositionDataGenerator.class);
    private static final String NAVST_UNDERWAY_ENGINE = "Under way using engine";
    private static final String NAVST_UNDERWAY_SAILING = "Under way sailing";
    private static final String NAVST_RESTR_MANOUVR = "Restricted manouverability";

    private List<ShipPosition> shipsData; 
    
    @Inject
    WsShipsPositionServer wsServer;

    @PostConstruct
    public void init()
    {
        shipsData = readInitialPositions();
    }

    /** At regular intervals every vessel has a high chance to get updated position data */
    @Scheduled(every="3s")     
    void changeShipsData() 
    {
        for (ShipPosition sp : shipsData)
        {
            if (getChance(80))
            {
                //LOG.info("CHANGING: " + sp.toStatusString());
                updateShipPosition(sp);
                LOG.info("CHANGED: " + sp.toStatusString());
            }
        }

        wsServer.sendShipsPositions(shipsData);
    }

    /** Parses the initial vessels data from the json file "initial-ships-data.json" */
    private List<ShipPosition> readInitialPositions()
    {
        try (
              Jsonb jsonb = JsonbBuilder.create();
              InputStream inStream = getClass().getResourceAsStream("/META-INF/resources/initial-ships-data.json");
            ) 
        {
            Type listOfPositions = new ArrayList<ShipPosition>(){}.getClass().getGenericSuperclass();
            return jsonb.fromJson(inStream, listOfPositions);
            
        }
        catch(Exception ex)
        {
            LOG.error("Error parsing the initial vessels data", ex);
        }

        return new ArrayList<>();
    }

    /** Internal helper method used to exctract a random chance [1, 100] */
    private static boolean getChance(int probability) 
    {
        int min = 1;
        int max = 100;
        int randomChance = (int)(Math.random() * ((max - min) + 1)) + min;
        return (randomChance <= probability);
	}

    /** This is the core logic to update the ship position data with pseudo-random logic */
    private void updateShipPosition(ShipPosition pos)
    {
        Coordinate lat = Coordinate.fromDegrees(pos.getLatitude().doubleValue());
        Coordinate lng = Coordinate.fromDegrees(pos.getLongitude().doubleValue());
        Point startPos = Point.at(lat, lng);

        //Distance away point, bearing is 45deg
        float mtPerSec = pos.getSpeed().floatValue() / 1.944f;
        float metresAfterSeconds = mtPerSec * 600 /* metres after 300 seconds */;
        Point dstPoint = EarthCalc.pointAt(startPos, pos.getCourse().doubleValue(), metresAfterSeconds);
        pos.setLongitude(Float.valueOf( (float) dstPoint.longitude));
        pos.setLatitude(Float.valueOf( (float) dstPoint.latitude));

        /* Change course with 35% chance */
        if (getChance(35))
        {
            float deltaCourse = getChance(50) ? 1.5f : -1.0f;
            float newCourse = pos.getCourse().floatValue() + deltaCourse;
            pos.setCourse(Float.valueOf(newCourse));
        }

        //* Change speed with 45% chance */
        if (getChance(45))
        {
            float deltaSpeed = getChance(50) ? 0.5f : -0.5f;
            float newSpeed = pos.getSpeed().floatValue() + deltaSpeed;
            pos.setSpeed(Float.valueOf(newSpeed));
        }

        //* Change navstatus with 25% chance */
        if (getChance(25))
        {
            if (pos.getNavStatus().equals(NAVST_UNDERWAY_ENGINE))
            {
                pos.setNavStatus(NAVST_UNDERWAY_SAILING);
            }
            else if (pos.getNavStatus().equals(NAVST_UNDERWAY_SAILING))
            {
                pos.setNavStatus(NAVST_RESTR_MANOUVR);
            }
            else if (pos.getNavStatus().equals(NAVST_RESTR_MANOUVR))
            {
                pos.setNavStatus(NAVST_UNDERWAY_ENGINE);
            }
        }

        /* Change draught with 25% chance */
        if (getChance(25))
        {
            float deltaDraught = getChance(50) ? 0.35f : -0.35f;
            float newDraught = pos.getDraught().floatValue() + deltaDraught;
            pos.setDraught(Float.valueOf(newDraught));
        }
    }
}