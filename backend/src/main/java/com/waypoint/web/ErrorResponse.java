package com.waypoint.web;

import java.util.List;

public record ErrorResponse(
        String error,
        String message,
        List<String> details
) {
}
