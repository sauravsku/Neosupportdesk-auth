package com.centneo.fintech.authApp.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
public class AuthenticationRequest implements Serializable {

	private static final long serialVersionUID = 5926468583005150707L;

	private String ssoId;
	private String password;
	
	public AuthenticationRequest() {
		
	}
	
	public AuthenticationRequest(String ssoId, String password) {
		this.setSsoId(ssoId);
		this.setPassword(password);
	}
	
	public String getSsoId() {
		return ssoId;
	}

	public void setSsoId(String username) {
		this.ssoId = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}