package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.exception.DuplicateEntityException;
import org.example.exception.ValidationException;
import org.example.model.Currency;
import org.example.service.CurrencyService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.example.JsonResponseUtil.sendErrorResponse;
import static org.example.JsonResponseUtil.sendJsonResponse;

@WebServlet("/currency/*")
public class CurrencyServlet extends HttpServlet {
    private final CurrencyService currencyService = new CurrencyService();
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
            String currencyCode = pathParts[1];
            if (currencyCode.isBlank()) {
                sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Currency code is missing in the path.");
                return;
            }
            handleGetCurrencyByCode(req, resp, currencyCode);
            return;
        }
        //Если URL имеет другой формат
        sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid URL format.");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        // Проверка запроса URL
        if (pathInfo != null && !pathInfo.equals("/")) {
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "This endpoint does not support POST.");
            return;
        }
        //Получение данных из тела
        String code = req.getParameter("code");
        String fullName = req.getParameter("fullName");
        String sign = req.getParameter("sign");

        try {
            // Вся логика, включая валидацию и проверку дубликатов, находится в сервисе.
            Currency savedCurrency = currencyService.addCurrency(code, fullName, sign);
            sendJsonResponse(resp, HttpServletResponse.SC_CREATED, savedCurrency);
        } catch (ValidationException e) {
            // Сервис обнаружил ошибки в данных
            sendJsonResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getErrors());
        } catch (DuplicateEntityException e) {
            // Сервис обнаружил, что такая валюта уже существует
            sendErrorResponse(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "A database error occurred.");
        }


    }

    private void handleGetAllCurrencies(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<Currency> currencies = currencyService.getAllCurrencies();
            sendJsonResponse(resp, HttpServletResponse.SC_OK, currencies);
        } catch (SQLException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "A database error occurred.");
        }
    }

    private void handleGetCurrencyByCode(HttpServletRequest req, HttpServletResponse resp, String currencyCode) throws IOException {
        try {
            Optional<Currency> currencyOptional = currencyService.getCurrencyByCode(currencyCode);
            if (currencyOptional.isPresent()) {
                sendJsonResponse(resp, HttpServletResponse.SC_OK, currencyOptional.get());
            } else {
                sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Currency with code '" + currencyCode + "' not found.");
            }
        } catch (SQLException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "A database error occurred.");
        }
    }

}