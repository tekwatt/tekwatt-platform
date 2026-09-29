package com.tekwatt.support.dto;

import com.tekwatt.support.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerTicketRequest(@NotBlank @Size(max = 200) String subject,
        @NotBlank String description, @NotBlank String category,
        @NotNull TicketPriority priority) { }
