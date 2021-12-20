package com.communication.incomingcallsample.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class IncomingCallController {

	@GetMapping("/hello")
	public String greeting() {
		return "OK";
	}
}

