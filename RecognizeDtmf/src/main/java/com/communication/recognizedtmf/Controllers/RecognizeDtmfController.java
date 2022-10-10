package com.communication.recognizedtmf.Controllers;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.File;
import java.io.FileInputStream;
import com.communication.recognizedtmf.Logger;
import com.communication.recognizedtmf.EventHandler.EventAuthHandler;
import com.communication.recognizedtmf.EventHandler.EventDispatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecognizeDtmfController {

	@RequestMapping("/api/recognizedtmf/callback")
	public static String onIncomingRequestAsync(@RequestBody(required = false) String data,
			@RequestParam(value = "secret", required = false) String secretKey) {
		EventAuthHandler eventhandler = EventAuthHandler.getInstance();

		/// Validating the incoming request by using secret set in app.settings
		if (eventhandler.authorize(secretKey)) {
			(EventDispatcher.getInstance()).processNotification(data);
		} else {
			Logger.logMessage(Logger.MessageType.ERROR, "Unauthorized Request");
		}

		return "OK";
	}

	@RequestMapping("/audio/{fileName}")
	public ResponseEntity<Object> loadFile(@PathVariable(value = "fileName", required = false) String fileName) {
		String filePath = "src/main/java/com/communication/recognizedtmf/audio/" + fileName;
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

		return ResponseEntity.ok().headers(headers).contentLength(file.length())
				.contentType(MediaType.parseMediaType("audio/x-wav")).body(resource);
	}
}
