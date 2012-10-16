package org.springone.push;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Bootstrap {

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		context.registerShutdownHook();
		
		while(true) {
			Thread.sleep(Long.MAX_VALUE);
		}
	}

}
