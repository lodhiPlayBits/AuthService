package com.lodhi.auth.dtos;

public record RefreshTokenRequest (
        String refreshToken
){
    public RefreshTokenRequest (String refreshToken){
        this.refreshToken = refreshToken;
    }


}
