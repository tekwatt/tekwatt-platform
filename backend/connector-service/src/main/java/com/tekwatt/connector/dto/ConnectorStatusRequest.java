package com.tekwatt.connector.dto;

import com.tekwatt.connector.entity.ConnectorStatus;
import jakarta.validation.constraints.NotNull;

public record ConnectorStatusRequest(@NotNull ConnectorStatus status) {}
