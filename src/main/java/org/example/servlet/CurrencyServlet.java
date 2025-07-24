package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CurrencyDAO;
import org.example.model.Currency;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebServlet("/currency/*")
public class CurrencyServlet extends HttpServlet {
    // В классе CurrencyServlet
    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        // pathInfo пустой, значит список всех валют
        if (pathInfo == null) {
            handleGetAllCurrencies(req, resp);
            return;
        }
        // pathInfo равен /, код валюты отсутствует в адресе
        if (pathInfo.equals("/")) {
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Currency code not in address.");
            return;
        }

        //Если pathInfo не пустой, извлекаем код валюты
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length == 2) {
            String currencyCharCode = pathParts[1];
            handleGetCurrencyByCode(req, resp, currencyCharCode);
            return;
        }
        //Если URL имеет другой формат
        sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format.");
    }

    private void handleGetAllCurrencies(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Currency> currencies = currencyDAO.findAll();
            sendJsonResponse(resp, HttpServletResponse.SC_OK, currencies);
        } catch (SQLException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }

    private void handleGetCurrencyByCode(HttpServletRequest req, HttpServletResponse resp, String currencyCode) throws IOException {
        try {
            Optional<Currency> currencyOptional = currencyDAO.findByCharCode(currencyCode);
            if (currencyOptional.isPresent()) {
                sendJsonResponse(resp, HttpServletResponse.SC_OK, currencyOptional.get());
            } else {
                sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Currency not found.");
            }
        } catch (SQLException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }

    /**
     * Метод отправляет JSON ответ
     *
     * @param resp   ответ клиенту
     * @param status код статуса ответа
     * @param data   объект
     */
    private void sendJsonResponse(HttpServletResponse resp, int status, Object data) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        objectMapper.writeValue(resp.getWriter(), data);
    }

    /**
     * Метод отправляет ответ об ошибке
     *
     * @param resp    ответ клиенту
     * @param status  код статуса ответа
     * @param message сообщение ошибки
     */
    private void sendErrorResponse(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        objectMapper.writeValue(resp.getWriter(), Map.of("status", status, "message", message));
    }
}