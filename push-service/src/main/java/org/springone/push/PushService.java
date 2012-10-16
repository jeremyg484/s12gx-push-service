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
		this.sockServer.installApp(new JsonObject().putString("prefix", "/socket"), new SockJSHandler());
	}
	
	private Handler<HttpServerRequest> getHttpHandler() {
		RouteMatcher matcher = new RouteMatcher();
		
		matcher.get("/", new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest request) {
				request.response.end("This is the SpringOne Push Messaging Service");
			}
		});
		
		matcher.post("/messages/:topic/", new Handler<HttpServerRequest>() {
			public void handle(final HttpServerRequest request) {
				final String topic = request.params().get("topic");
				request.bodyHandler(new Handler<Buffer>(){
					public void handle(Buffer body) {
						JsonObject message = 
								new JsonObject().putString("topic", topic).
								putObject("message", new JsonObject(body.toString()));
						vertx.eventBus().publish(RabbitService.PUBLISH, message);
						request.response.end("ok");
					}
				});
			}
		});
		
		matcher.post("/subscriptions/:bindingKey/", new Handler<HttpServerRequest>(){
			public void handle(final HttpServerRequest request) {
				final String bindingKey = request.params().get("bindingKey");
				request.bodyHandler(new Handler<Buffer>(){
					public void handle(Buffer body) {
						
						JsonObject data = new JsonObject(body.toString());
						
						vertx.eventBus().send(SessionManager.GET, data, new Handler<Message<JsonObject>>(){
							public void handle(Message<JsonObject> sessionMsg) {
								JsonObject session = sessionMsg.body;
								if (!session.getArray("bindings").contains(bindingKey)) {
									session.getArray("bindings").addString(bindingKey);
									vertx.eventBus().send(SessionManager.UPDATE, session);
									
									JsonObject bindMessage = 
											new JsonObject().putString("id", session.getString("id")).
											putString("bindingKey", bindingKey);
									
									vertx.eventBus().send(RabbitService.BIND, bindMessage, new Handler<Message<JsonObject>>(){
										public void handle(Message<JsonObject> bindMsg) {
											request.response.end("ok");
										}
									});
								}
							}
						});
					}
				});
			}
		});
		
		return matcher;
	}
	
	private class SockJSHandler implements Handler<SockJSSocket> {

		public void handle(final SockJSSocket socket) {
			final long timerId = vertx.setTimer(5000, new Handler<Long>() {
				public void handle(Long timerId) {
					socket.close();
				}
			});
			
			socket.dataHandler(new Handler<Buffer>() {
				public void handle(Buffer payload) {
					vertx.cancelTimer(timerId);
					JsonObject jsonPayload = new JsonObject(payload.toString());
					
					vertx.eventBus().send(SessionManager.CREATE, jsonPayload, 
							new Handler<Message<JsonObject>>() {
								public void handle(Message<JsonObject> sessionMsg) {
									final String sessionId = sessionMsg.body.getString("id");
									sessions.put(socket.writeHandlerID, sessionId);
									vertx.eventBus().registerHandler("session-"+sessionId, new Handler<Message<JsonObject>>(){
										public void handle(Message<JsonObject> dataMessage) {
											socket.writeBuffer(new Buffer(dataMessage.body.toString()));
										}
									});
									vertx.eventBus().send(RabbitService.CREATE_AND_SUBSCRIBE, sessionMsg.body, new Handler<Message<JsonObject>>(){
										public void handle(Message<JsonObject> msg) {
											JsonObject connResponse = 
													new JsonObject().
														putString("type", "session-ok").
														putString("id", sessionId);
											socket.writeBuffer(new Buffer(connResponse.toString()));
										}
									});
								}
							});
				}
				
			});
			
			socket.endHandler(new Handler<Void>(){
				public void handle(Void closeEvent) {
					String sessionId = sessions.get(socket.writeHandlerID);
					vertx.eventBus().send(SessionManager.DELETE, new JsonObject().putString("id", sessionId));
					vertx.eventBus().send(RabbitService.CLOSE_SESSION, new JsonObject().putString("id", sessionId));
				}
			});
		}

	}

}
