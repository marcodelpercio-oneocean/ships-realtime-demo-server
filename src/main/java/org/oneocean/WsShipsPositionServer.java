package org.oneocean;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.Session;

import org.jboss.logging.Logger;

 
@ApplicationScoped
@ServerEndpoint("/vessels/positions")
public class WsShipsPositionServer {
    private Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static Jsonb jsonb = JsonbBuilder.create();
    private Type listOfPositions = new ArrayList<ShipPosition>(){}.getClass().getGenericSuperclass();
    private static final Logger LOG = Logger.getLogger(WsShipsPositionServer.class);

    @OnOpen
    public void onOpen(Session session) 
    {
        sessions.put(session.getId(), session);
        LOG.info("New session open: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) 
    {
        sessions.remove(session.getId());
        LOG.info("Session " + session.getId() + " closed");    
    }

    @OnError
    public void onError(Session session, Throwable throwable) 
    {
        sessions.remove(session.getId());
        LOG.info("Error on session " + session.getId() + ": " + throwable);
    }

    @OnMessage
    public void onMessage(Session session, String message) 
    {
        LOG.info("Session[" + session.getId() + "] received message: " + message);
    }

    /** 
     * Encodes the ships position data in JSON and broadcasts the message
     * to every active websocket session
     */
    public void sendShipsPositions(List<ShipPosition> shipsData)
    {
        String jsonShipsData = jsonb.toJson(shipsData, listOfPositions);
        broadcast(jsonShipsData);
        //LOG.info("BROADCASTING: " + jsonShipsData);
    }

    public Map<String, Session> getSessions()
    {
        return this.sessions;
    }
    
    private void broadcast(String message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result ->  {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}