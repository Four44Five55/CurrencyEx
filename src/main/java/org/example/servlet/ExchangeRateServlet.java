package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.exception.DuplicateEntityException;
import org.example.exception.ValidationException;
import org.example.model.ExchangeRate;
import org.example.service.ExchangeRateService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.example.JsonResponseUtil.sendErrorResponse;
import static org.example.JsonResponseUtil.sendJsonResponse;

@WebServlet("/exchangeRate/*")
public class ExchangeRateServlet extends HttpServlet {
    private final ExchangeRateService service = new ExchangeRateService();
    private final ExchangeRate exchangeRate = new ExchangeRate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        // pathInfo пустой, значит список всех валют
        if (pathInfo == null) {
            handleGetAllExchangeRates(req, resp);
            return;
        }
        // pathInfo равен /, код валюты отсутствует в адресе
        if (pathInfo.equals("/")) {
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Exchange rate code not in address.");
            return;
        }
        //Если pathInfo не пустой, извлекаем код валюты
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length == 2) {
            String currencyCode = pathParts[1];
            if (currencyCode.isBlank()) {
                sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Exchange rate is missing in the path.");
                return;
            }
            handleGetExchangeRateByCode(req, resp, currencyCode);
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
        String idCurrency = req.getParameter("idCurrency");
        int nominal = Integer.parseInt(req.getParameter("nominal"));
        BigDecimal rate = new BigDecimal(req.getParameter("rate"));

        try {
            // Вся логика, включая валидацию и проверку дубликатов, находится в сервисе.
            ExchangeRate savedRate = service.addExchangeRate(idCurrency, nominal, rate);
            sendJsonResponse(resp, HttpServletResponse.SC_CREATED, savedRate);
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


    private void handleGetAllExchangeRates(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            List<ExchangeRate> rates = service.getAllExchangeRates();
            sendJsonResponse(resp, HttpServletResponse.SC_OK, rates);
        } catch (SQLException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "A database error occurred.");
        }
    }

    private void handleGetExchangeRateByCode(HttpServletRequest req, HttpServletResponse resp, String currencyCode) throws IOException {
        try {
            Optional<ExchangeRate> rateOptional = service.getExchangeRateByCode(currencyCode);
            if (rateOptional.isPresent()) {
                sendJsonResponse(resp, HttpServletResponse.SC_OK, rateOptional.get());
            } else {
                sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, "Exchange rate with code '" + currencyCode + "' not found.");
            }
        } catch (SQLException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "A database error occurred.");
        }
    }
}
