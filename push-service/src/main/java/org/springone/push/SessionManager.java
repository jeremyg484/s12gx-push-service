package org.springone.push;

import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class SessionManager {

	public static final String CREATE = "sessionManager.create";
	
	public static final String GET = "sessionManager.get";
	
	public static final String UPDATE = "sessionManager.update";
	
	public static final String DELETE = "sessionManager.delete";
	
	private final StringRedisTemplate redis;
	
	public SessionManager(Vertx vertx, StringRedisTemplate redis) {
		this.redis = redis;
		vertx.eventBus().registerHandler(CREATE, new CreateHandler());
		vertx.eventBus().registerHandler(GET, new GetHandler());
		vertx.eventBus().registerHandler(UPDATE, new UpdateHandler());
		vertx.eventBus().registerHandler(DELETE, new DeleteHandler());
	}
	
	private class CreateHandler implements Handler<Message<JsonObject>> {

		public void handle(Message<JsonObject> msg) {
			JsonObject session = newSession();
			System.out.println("Creating session: "+session.getString("id"));
			redis.boundValueOps("session:"+session.getString("id")).set(session.toString());
			msg.reply(session);
		}
		
		private JsonObject newSession() {
			String id = UUID.randomUUID().toString();
			return new JsonObject().
					putString("id", id).
					putArray("bindings", new JsonArray().addString("broadcast").addString("connection."+id));
		}
		
	}
	
	private class GetHandler implements Handler<Message<JsonObject>> {
		public void handle(Message<JsonObject> msg) {
			JsonObject session = new JsonObject(redis.boundValueOps("session:"+msg.body.getString("id")).get());
			System.out.println("Retrieved session: "+session.toString());
			msg.reply(session);
		}
	}
	
	private class UpdateHandler implements Handler<Message<JsonObject>> {
		public void handle(Message<JsonObject> msg) {
			System.out.println("Updating session: "+msg.body.toString());
			redis.boundValueOps("session:"+msg.body.getString("id")).set(msg.body.toString());
		} 
	}
	
	private class DeleteHandler implements Handler<Message<JsonObject>> {
		public void handle(Message<JsonObject> msg) {
			String sessionId = msg.body.getString("id");
			System.out.println("Removing session: "+sessionId);
			redis.delete("session:"+sessionId);
		}
	}
	
}
