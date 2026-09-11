package com.lodhi.auth.dtos;

import java.time.Instant;

public record ErrorMessage(String message, org.springframework.http.HttpStatus status, String Error, Instant timestamp) {


}
