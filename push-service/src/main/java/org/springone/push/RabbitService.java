package org.springone.push;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class RabbitService {

	private final Vertx vertx;
	
	private final ConnectionFactory connectionFactory;
	
	private final AmqpAdmin admin;
	
	private final TopicExchange exchange = new TopicExchange(EXCHANGE_NAME, false, false);

	private final AmqpTemplate rabbit;
	
	private final TaskExecutor taskExecutor;
	
	public static final String CREATE_AND_SUBSCRIBE = "rabbitService.createAndSubscribe";
		
	public static final String PUBLISH = "rabbitService.publish";
	
	public static final String BIND = "rabbitService.bind";
	
	public static final String UNBIND = "rabbitService.unbind";
	
	public static final String CLOSE_SESSION = "rabbitService.closeSession";
	
	private static final String EXCHANGE_NAME = "s12gx-push-exchange-01";
	
	private final Map<String, SimpleMessageListenerContainer> sessions = new HashMap<String, SimpleMessageListenerContainer>();
	
	public RabbitService(Vertx vertx, ConnectionFactory connectionFactory, AmqpAdmin admin, AmqpTemplate rabbit, ThreadPoolTaskExecutor taskExecutor) {
		this.vertx = vertx;
		this.connectionFactory = connectionFactory;
		this.admin = admin;
		this.rabbit = rabbit;
		this.taskExecutor = taskExecutor;
		admin.declareExchange(exchange);
		vertx.eventBus().registerHandler(CREATE_AND_SUBSCRIBE, new SubscribeHandler());
		vertx.eventBus().registerHandler(PUBLISH, new PublishHandler());
		vertx.eventBus().registerHandler(BIND, new BindHandler());
		vertx.eventBus().registerHandler(UNBIND, new UnbindHandler());
		vertx.eventBus().registerHandler(CLOSE_SESSION, new CloseHandler());
	}
	
	private final class SubscribeHandler implements Handler<Message<JsonObject>> {
		
		public void handle(Message<JsonObject> msg) {
			String sessionId = msg.body.getString("id");
			String qName = "queue-"+sessionId;
			Queue q = new Queue(qName, false, true, true);
			admin.declareQueue(q);
			
			for(Object binding : msg.body.getArray("bindings")) {
				admin.declareBinding(BindingBuilder.bind(q).to(exchange).with(binding.toString()));
			}
			
			SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
			container.setConnectionFactory(connectionFactory);
			//container.setTaskExecutor(taskExecutor);
			container.setQueueNames(qName);
			container.setMessageListener(new AmqpMessageHandler(sessionId));
			container.start();
			
			sessions.put(sessionId, container);
			msg.reply(new JsonObject());
		}
	}
	
	private final class CloseHandler implements Handler<Message<JsonObject>> {

		public void handle(Message<JsonObject> msg) {
			SimpleMessageListenerContainer container = sessions.remove(msg.body.getString("id"));
			for(String qName : container.getQueueNames()) {
				admin.deleteQueue(qName);
			}
			container.destroy();
		}
	}
	
	private final class PublishHandler implements Handler<Message<JsonObject>> {
		public void handle(Message<JsonObject> msg) {
			String topic = msg.body.getString("topic");
			JsonObject message = msg.body.getObject("message");
			rabbit.convertAndSend(EXCHANGE_NAME, topic, message.toString());
		}
	}
	
	private final class BindHandler implements Handler<Message<JsonObject>> {

		public void handle(Message<JsonObject> msg) {
			String sessionId = msg.body.getString("id");
			String qName = "queue-"+sessionId;
			Queue q = new Queue(qName, false, true, true);
			
			String bindingKey = msg.body.getString("bindingKey");
			
			admin.declareBinding(BindingBuilder.bind(q).to(exchange).with(bindingKey));
			msg.reply(new JsonObject());
		}
	}
	
	private final class UnbindHandler implements Handler<Message<JsonObject>> {

		public void handle(Message<JsonObject> msg) {
			String sessionId = msg.body.getString("id");
			String qName = "queue-"+sessionId;
			Queue q = new Queue(qName, false, true, true);
			
			String bindingKey = msg.body.getString("bindingKey");
			
			admin.removeBinding(BindingBuilder.bind(q).to(exchange).with(bindingKey));	
			msg.reply(new JsonObject());
		}
	}
	
	private final class AmqpMessageHandler implements MessageListener {

		private final String sessionId;
		
		public AmqpMessageHandler(String sessionId) {
			this.sessionId = sessionId;
		}

		public void onMessage(org.springframework.amqp.core.Message message) {
			JsonObject msgBody = new JsonObject();
			msgBody.putString("type", "message").
				putString("topic", message.getMessageProperties().getReceivedRoutingKey()).
				putObject("data", new JsonObject((String) new SimpleMessageConverter().fromMessage(message)));
			vertx.eventBus().send("session-"+sessionId, msgBody);
		}
		
	}
}
