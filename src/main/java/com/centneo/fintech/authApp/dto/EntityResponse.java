package com.centneo.fintech.authApp.dto;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class EntityResponse {
	public static ResponseEntity<Object> generateResponse(
			String username,
			String message,
			HttpStatus status,
			Object responseObj,
			boolean mfaRequired,
			boolean isMfaRegistered
	) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("username", username);
		map.put("TimeStamp", new Date());
		map.put("Message", message);
		map.put("mfaRequired", mfaRequired);
		map.put("isMfaRegistered", isMfaRegistered);
		map.put("Status", status.value());
		map.put("Data", responseObj);

		return new ResponseEntity<>(map, status);
	}
}
