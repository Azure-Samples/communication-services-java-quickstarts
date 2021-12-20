package com.communication.incomingcallsample.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import com.communication.incomingcallsample.utils.ResponseHandler;

@RestController
public class IncomingCallController {

	@GetMapping("/hello")
	public ResponseEntity<?> greeting() {
		return new ResponseEntity<>("OK", HttpStatus.OK);
	}

	@PostMapping(value = "/OnIncomingCall")
	public static ResponseEntity<?> onIncomingRequestAsync(@RequestBody(required = false) String data) {

		EventGridEvent eventGridEvent = null;
		// parse EventGridEvent
		try{
			List<EventGridEvent> eventGridEvents = EventGridEvent.fromString(data);
			eventGridEvent = eventGridEvents.get(0);
		}
		catch(Exception e) {
			return new ResponseEntity<String>("Failed to parse EventGridEvent:" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if(eventGridEvent == null){
			return new ResponseEntity<String>("Could not get EventGridEvent", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// get event type
		String type = eventGridEvent.getEventType();
		if(type.equals("Microsoft.EventGrid.SubscriptionValidationEvent")) {
			SubscriptionValidationEventData d = eventGridEvent.getData().toObject(SubscriptionValidationEventData.class);
			String validationCode = d.getValidationCode();
			return ResponseEntity.status(HttpStatus.OK).body(Map.of(
            "validationResponse", validationCode));
		} else {
			return ResponseHandler.generateResponse("unknown EventGridEvent type: " + eventGridEvent.toString() , HttpStatus.BAD_REQUEST, null);
		}
	}
}

