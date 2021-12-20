package com.communication.incomingcallsample.Controller;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class IncomingCallController {
    private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	public class Greeting {
		private final long id;
		private final String content;
		public Greeting(long id, String content) {
			this.id = id;
			this.content = content;
		}
		public long getId() {
			return id;
		}
		public String getContent() {
			return content;
		}
	}

	@GetMapping("/greeting")
	public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		return new Greeting(counter.incrementAndGet(), String.format(template, name));
	}

	@GetMapping("/hello")
	public String greeting() {
		return "OK";
	}
}

