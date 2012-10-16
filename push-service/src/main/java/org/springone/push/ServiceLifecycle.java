package org.springone.push;

import org.springframework.context.SmartLifecycle;
import org.vertx.java.core.http.HttpServer;

public class ServiceLifecycle implements SmartLifecycle {

	private final HttpServer server;
	
	private boolean running = false;
	
	public ServiceLifecycle(HttpServer httpServer) {
		this.server = httpServer;
	}

	public void start() {
		server.listen(8080);
		System.out.println("Push Service started and listening on port 8080");
		running = true;
	}

	public void stop() {
		server.close();
		System.out.println("Push Service has been shut down");
		running = false;
	}

	public boolean isRunning() {
		return running;
	}

	public int getPhase() {
		return 0;
	}

	public boolean isAutoStartup() {
		return true;
	}

	public void stop(Runnable callback) {
		stop();
		callback.run();				
	}
}
