package com.techstore.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class OnPasswordChangedEvent extends ApplicationEvent {

    private final String email;

    public OnPasswordChangedEvent(String email) {
        super(email);
        this.email = email;
    }
}