package com.communication.incomingcallsample.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.azure.communication.callingserver.CallingServerClient;
import com.azure.communication.callingserver.CallingServerClientBuilder;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.communication.incomingcallsample.EventHandler.EventAuthHandler;
import com.communication.incomingcallsample.EventHandler.EventDispatcher;
import com.communication.incomingcallsample.Log.Logger;
import com.communication.incomingcallsample.Utils.CallConfiguration;
import com.communication.incomingcallsample.Utils.ConfigurationManager;
import com.communication.incomingcallsample.Utils.IncomingCallHandler;
import com.communication.incomingcallsample.Utils.ResponseHandler;

@RestController
public class IncomingCallController {
	private final CallingServerClient callingServerClient;
	private CallConfiguration callConfiguration;
	private EventAuthHandler eventAuthHandler;

	public IncomingCallController(){
		ConfigurationManager configurationManager = ConfigurationManager.getInstance();
		this.eventAuthHandler = new EventAuthHandler(configurationManager.getAppSettings("SecretValue"));
		this.callConfiguration = CallConfiguration.GetCallConfiguration(configurationManager, this.eventAuthHandler.GetSecretQuerystring());

		this.callingServerClient = new CallingServerClientBuilder()
			.connectionString(this.callConfiguration.connectionString)
			.buildClient();
	}

	@PostMapping(value = "CallingServerAPICallBacks")
	public String callingServerAPICallBacks(@RequestBody(required = false) String data,
			@RequestParam(value = "secret", required = false) String secretKey) {

		// Validating the incoming request by using secret set in config.properties
		if (this.eventAuthHandler.authorize(secretKey)) {
			Logger.logMessage(Logger.MessageType.INFORMATION, "call back event: " + data);
			EventDispatcher.getInstance().processNotification(data);
		} else {
			Logger.logMessage(Logger.MessageType.ERROR, "Unauthorized Request");
		}

		return "OK";
	}

	@PostMapping(value = "/OnIncomingCall")
	public ResponseEntity<?> onIncomingRequestAsync(@RequestBody(required = false) String data) {

		// parse EventGridEvent
		EventGridEvent eventGridEvent = null;
		try{
			List<EventGridEvent> eventGridEvents = EventGridEvent.fromString(data);
			eventGridEvent = eventGridEvents.get(0);
		}
		catch(Exception e) {
			return new ResponseEntity<String>("Failed to parse EventGridEvent:" + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if(eventGridEvent == null){
			return ResponseHandler.generateResponse("Could not get EventGridEvent", HttpStatus.INTERNAL_SERVER_ERROR, null);
		}

		Logger.logEventGridEvent(Logger.MessageType.INFORMATION, eventGridEvent);

		String type = eventGridEvent.getEventType();
		if(type.equals("Microsoft.EventGrid.SubscriptionValidationEvent")) {
			return getRegisterEventGridResponse(eventGridEvent);
		} else if(type.equals("Microsoft.Communication.IncomingCall")) {
			String to = getRecipientMRI(eventGridEvent);
			return handleIncomingCall(data, to);
		} else {
			return ResponseHandler.generateResponse("unknown EventGridEvent type: " + eventGridEvent.toString() , HttpStatus.BAD_REQUEST, null);
		}
	}

	private static ResponseEntity<?> getRegisterEventGridResponse(EventGridEvent eventGridEvent){
		SubscriptionValidationEventData subscriptionValidationEventData = eventGridEvent.getData().toObject(
				SubscriptionValidationEventData.class);
		String validationCode = subscriptionValidationEventData.getValidationCode();
		Logger.logMessage(Logger.MessageType.INFORMATION, "Registered ACS resource Event Grid.");
		return ResponseEntity.status(HttpStatus.OK).body(Map.of(
            "validationResponse", validationCode));
	}

	private ResponseEntity<?> handleIncomingCall(String data, String to) {
		try {
			String incomingCallContext = data.split("\"incomingCallContext\":\"")[1].split("\"}")[0];
			if(new ArrayList<>(Arrays.asList(this.callConfiguration.allowedRecipientList)).contains(to)){
				new IncomingCallHandler(this.callingServerClient, this.callConfiguration).report(incomingCallContext);
				return ResponseHandler.generateResponse("answer call done", HttpStatus.OK, null);
			} else {
				Logger.logMessage(Logger.MessageType.INFORMATION, to + " is not in the recipient list that this app supports to answer, skip answering");
				return ResponseHandler.generateResponse("call to " + to + "ignored as it is not in the allow list", HttpStatus.OK, null);
			}
		} catch(Exception e) {
			String message = "Fails in OnIncomingCall ---> " + e.getMessage();
			Logger.logMessage(Logger.MessageType.ERROR, message);
			return ResponseHandler.generateResponse(message, HttpStatus.INTERNAL_SERVER_ERROR, null);
		}
	}

	private String getRecipientMRI(EventGridEvent eventGridEvent) {
		return eventGridEvent.getSubject().split("recipient/")[1];
	}
}

