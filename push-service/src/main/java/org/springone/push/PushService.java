package org.springone.push;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;

public class PushService {

	private final Vertx vertx;
	
	private final HttpServer httpServer;
	
	private final SockJSServer sockServer;
	
	private final Map<String, String> sessions = new HashMap<String, String>();
	
	public PushService(Vertx vertx, HttpServer httpServer, SockJSServer sockServer) {
		this.vertx = vertx;
		this.httpServer = httpServer;
		this.sockServer = sockServer;
		
		this.httpServer.requestHandler(new CorsHandler(getHttpHandler()));
		//this.httpServer.websocketHandler(new WebSocketHandler());
		this.sockServer.installApp(new JsonObject().putString("prefix", "/socket"), new SockJSHandler());
	}
	
	private Handler<HttpServerRequest> getHttpHandler() {
		RouteMatcher matcher = new RouteMatcher();
		
		matcher.get("/", new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest request) {
				request.response.end("This is the SpringOne Push Messaging Service");
			}
		});
		
		//TODO - Add HTTP handler to publish messages
		
		//TODO - Add HTTP handler to allow additional subscriptions
		
		return matcher;
	}
	
	private class SockJSHandler implements Handler<SockJSSocket> {

		public void handle(final SockJSSocket socket) {
			System.out.println("SockJS connected...");
			socket.dataHandler(new Handler<Buffer>(){

			    public void handle(Buffer data) {
			        String msg = data.toString();
			        socket.writeBuffer(new Buffer("Echo: "+msg));
			    }
			    
			});
			
			socket.endHandler(new Handler<Void>(){

			    public void handle(Void event) {
			        System.out.println("SockJS closed.");
			    }
			    
			});
		}

	}

}
