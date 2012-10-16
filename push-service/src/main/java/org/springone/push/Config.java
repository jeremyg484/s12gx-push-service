package org.springone.push;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.sockjs.SockJSServer;

@Configuration
public class Config {
	
	@Bean
	public PushService pushService() {
		return new PushService(vertx(), httpServer(), sockServer());
	}
	
	@Bean
	public SessionManager sessionManager() {
		return new SessionManager(vertx(), redisTemplate());
	}
	
	@Bean
	public RabbitService rabbitService() {
		return new RabbitService(vertx(), rabbitConnectionFactory(), 
				new RabbitAdmin(rabbitConnectionFactory()), 
				new RabbitTemplate(rabbitConnectionFactory()),
				rabbitTaskExecutor());
	}
	
	@Bean
	public ThreadPoolTaskExecutor rabbitTaskExecutor() {
		return null;
	}
	
	@Bean
	public ConnectionFactory rabbitConnectionFactory() {
		return new CachingConnectionFactory();
	}

	@Bean
	public StringRedisTemplate redisTemplate() {
		return new StringRedisTemplate(jedisConnectionFactory());
	}

	@Bean
	public JedisConnectionFactory jedisConnectionFactory() {
		return new JedisConnectionFactory();
	}

	@Bean
	public Vertx vertx() {
		return Vertx.newVertx();
	}
	
	@Bean
	public HttpServer httpServer() {
		return vertx().createHttpServer();
	}
	
	@Bean
	public SockJSServer sockServer() {
		return vertx().createSockJSServer(httpServer());
	}
	
	@Bean
	public ServiceLifecycle vertxLifecycle() {
		return new ServiceLifecycle(httpServer());
	}
}
