package com.techstore.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class OnPasswordResetRequestEvent extends ApplicationEvent {

  private final String email;

  public OnPasswordResetRequestEvent(String email) {
    super(email);
    this.email = email;
  }
}
