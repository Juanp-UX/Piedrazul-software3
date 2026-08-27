package com.unicauca.piedrazul.users.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public record UserRegisteredEvent(
         Long userId,
         String login,
         String nombreCompleto,
         String rol
) {
}
