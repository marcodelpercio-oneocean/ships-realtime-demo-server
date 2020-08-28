package org.oneocean;

import java.io.IOException;
import java.util.Objects;

import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/** 
 * Simple RESTful endpoint to manage the websocket session(s) 
 */
@Path("/ws-server")
public class WsServerManagerResource 
{
    private static final String NEWLINE = "<br />\n";

    @Inject
    WsShipsPositionServer wsServer;

    @GET
    @Path("/connections")
    @Produces(MediaType.TEXT_HTML)
    public String connections() 
    {
        StringBuilder response = new StringBuilder();
        response.append("Active connections: " + NEWLINE);
        response.append("<ol>");

        wsServer.getSessions().keySet().forEach(sessionId -> {
            response.append("<li>" + sessionId + " ");
            response.append("<a href=\"connections/kick/" + sessionId + "\">/kick</a></li>");
        });

        response.append("</ol>");
        return response.toString();
    }
    
    @GET
    @Path("/connections/kick/{sessionId}")
    @Produces(MediaType.TEXT_PLAIN) 
    public String kicksession(@PathParam("sessionId") final String sessionId) throws IOException 
    {
        Session wsSession = wsServer.getSessions().get(sessionId);

        if (Objects.isNull(wsSession) || ! wsSession.isOpen())
        {
            return "Please enter a valid session to kick...";
        }

        CloseReason closeReason = new CloseReason(new CloseReason.CloseCode() {
                                                        @Override // custom close codes (4000-4999)
                                                        public int getCode() 
                                                        {
                                                            return 4000;
                                                        }
                                                  }, "Kicked by ServerManager");

        wsSession.close(closeReason);

        return "OK";
    }
}

