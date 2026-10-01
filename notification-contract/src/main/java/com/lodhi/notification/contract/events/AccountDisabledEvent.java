package com.lodhi.notification.contract.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDisabledEvent implements Serializable {
    private Long userId;
    private String email;
    private String message;
    private Long timestamp;
}
