package com.unicauca.piedrazul.users.events;


public record UserRegisteredEvent(
         Long userId,
         String login,
         String nombreCompleto,
         String rol
) {
}
