package com.communication.outboundcallreminder.Controllers;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.FileInputStream;
import com.communication.outboundcallreminder.Logger;
import com.communication.outboundcallreminder.EventHandler.EventAuthHandler;
import com.communication.outboundcallreminder.EventHandler.EventDispatcher;

@RestController
public class OutboundCallController {

	@RequestMapping("/api/outboundcall/callback")
	public static String OnIncomingRequestAsync(@RequestBody(required = false) String data,
			@RequestParam(value = "secret", required = false) String secretKey) {
		EventAuthHandler eventhandler = EventAuthHandler.GetInstance();

		/// Validating the incoming request by using secret set in app.settings
		if (eventhandler.Authorize(secretKey)) {
			(EventDispatcher.GetInstance()).ProcessNotification(data);
		} else {
			Logger.LogMessage(Logger.MessageType.ERROR, "Unauthorized Request");
		}

		return "OK";
	}

	@RequestMapping("/audio/{fileName}")
	public ResponseEntity<Object> loadFile(@PathVariable(value = "fileName", required = false) String fileName) {
		String filePath = "src/main/java/com/communication/outboundcallreminder/audio/" + fileName;
		File file = new File(filePath);
		InputStreamResource resource = null;

		try {
			resource = new InputStreamResource(new FileInputStream(file));
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
		headers.add("Pragma", "no-cache");
		ResponseEntity<Object> responseEntity = ResponseEntity.ok().headers(headers).contentLength(file.length())
				.contentType(MediaType.parseMediaType("audio/x-wav")).body(resource);

		return responseEntity;
	}
}
