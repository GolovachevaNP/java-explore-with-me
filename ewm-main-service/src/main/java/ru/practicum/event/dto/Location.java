package ru.practicum.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Location {

    @NotNull
    private Double lat;

    @NotNull
    private Double lon;
}