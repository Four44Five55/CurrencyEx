package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonResponseUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void sendJsonResponse(HttpServletResponse resp, int status, Object data) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        mapper.writeValue(resp.getWriter(), data);
    }

    public static void sendErrorResponse(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        mapper.writeValue(resp.getWriter(), Map.of("status", status, "message", message));
    }

    public static void sendValidationErrorResponse(HttpServletResponse resp, int status, Map<String, String> errors) throws IOException {
        String combinedValidationMessage = errors.values().stream()
                .collect(Collectors.joining("; "));

        Map<String, Object> errorBody = Map.of(
                "status", status,
                "message", combinedValidationMessage
        );

        sendJsonResponse(resp, status, errorBody);
    }
}
