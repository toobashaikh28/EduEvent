package com.tooba.EduEvent.dto.response;

import lombok.*;

/** Count of registered users in a city — powers the registrations-by-city map. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CityBreakdownResponse {
    private String city;
    private long count;
}
