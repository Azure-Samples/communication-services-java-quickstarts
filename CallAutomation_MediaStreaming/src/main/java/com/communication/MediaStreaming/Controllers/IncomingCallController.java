package com.communication.MediaStreaming.Controllers;

import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.SystemEventNames;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationEventData;
import com.azure.messaging.eventgrid.systemevents.SubscriptionValidationResponse;
import com.communication.MediaStreaming.CallConfiguration;
import com.communication.MediaStreaming.EventHandler.EventAuthHandler;
import com.communication.MediaStreaming.EventHandler.EventDispatcher;
import com.communication.MediaStreaming.Logger;
import com.communication.MediaStreaming.MediaStreaming;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IncomingCallController {

  CallConfiguration callConfiguration;

  IncomingCallController() {
    callConfiguration = CallConfiguration.initiateConfiguration();
  }

  @PostMapping(
    value = "/OnIncomingCall",
    consumes = "application/json",
    produces = "application/json"
  )
  public ResponseEntity<?> OnIncomingCall(
    @RequestBody(required = false) String data
  ) {
    List<EventGridEvent> eventGridEvents = EventGridEvent.fromString(data);

    if (eventGridEvents.stream().count() > 0) {
      EventGridEvent eventGridEvent = eventGridEvents.get(0);
      BinaryData eventData = eventGridEvent.getData();

      if (
        eventGridEvent
          .getEventType()
          .equals(SystemEventNames.EVENT_GRID_SUBSCRIPTION_VALIDATION)
      ) {
        try {
          SubscriptionValidationEventData subscriptionValidationEvent = eventData.toObject(
            SubscriptionValidationEventData.class
          );
          SubscriptionValidationResponse responseData = new SubscriptionValidationResponse();
          responseData.setValidationResponse(
            subscriptionValidationEvent.getValidationCode()
          );

          return new ResponseEntity<>(responseData, HttpStatus.OK);
        } catch (Exception e) {
          e.printStackTrace();
          return new ResponseEntity<>(
            e.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR
          );
        }
      } else if (
        eventGridEvent
          .getEventType()
          .equals("Microsoft.Communication.IncomingCall")
      ) {
        try {
          JsonObject jsonData = new Gson()
            .fromJson(eventGridEvent.getData().toString(), JsonObject.class);
          if (data != null) {
            String callerId = jsonData
              .getAsJsonObject("from")
              .get("rawId")
              .getAsString();

            if (
              data != null &&
              (
                callerId == "*" ||
                callConfiguration.acceptCallsFrom.contains(callerId)
              )
            ) {
              String incomingCallContext = jsonData
                .get("incomingCallContext")
                .getAsString();
              Logger.logMessage(
                Logger.MessageType.INFORMATION,
                incomingCallContext
              );
              new MediaStreaming(callConfiguration).report(incomingCallContext);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          return new ResponseEntity<>(
            e.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR
          );
        }
      } else {
        return new ResponseEntity<>(
          eventGridEvent.getEventType() + " is not handled.",
          HttpStatus.BAD_REQUEST
        );
      }
    }
    return new ResponseEntity<>(
      "Event count is not available.",
      HttpStatus.BAD_REQUEST
    );
  }

  @RequestMapping("/api/MediaStreaming/callback")
  public static String CallAutomationApiCallBack(
    @RequestBody(required = false) String data,
    @RequestParam(value = "secret", required = false) String secretKey
  ) {
    EventAuthHandler eventhandler = EventAuthHandler.getInstance();

    /// Validating the incoming request by using secret set in app.settings
    if (eventhandler.authorize(secretKey)) {
      (EventDispatcher.getInstance()).processNotification(data);
    } else {
      Logger.logMessage(Logger.MessageType.ERROR, "Unauthorized Request");
    }
    return "OK";
  }
}
