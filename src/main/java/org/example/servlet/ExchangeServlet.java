package org.example.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.exception.*;
import org.example.service.ExchangeRateService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static org.example.JsonResponseUtil.*;

@WebServlet("/exchange")
public class ExchangeServlet extends HttpServlet {
    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fromCurrencyCode = req.getParameter("from");
        String toCurrencyCode = req.getParameter("to");
        String amountStr = req.getParameter("amount");

        if (fromCurrencyCode == null || toCurrencyCode == null || amountStr == null ||
                fromCurrencyCode.isBlank() || toCurrencyCode.isBlank() || amountStr.isBlank()) {
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Укажите сумму для конвертации.");
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.signum() < 0) { // Проверяем, что сумма не отрицательная
                sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Сумма должна быть неотрицательным числом.");
                return;
            }

        } catch (NumberFormatException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Неверный формат суммы. Это должно быть число.");
            return;
        }

        BigDecimal crossRate = exchangeRateService.calculateCrossRate(fromCurrencyCode, toCurrencyCode);

        BigDecimal convertedAmount = exchangeRateService.convertAmount(amount, crossRate);


        Map<String, Object> responseBody = Map.of(
                "from", fromCurrencyCode,
                "to", toCurrencyCode,
                "rate", crossRate,
                "amount", amount,
                "convertedAmount", convertedAmount
        );

        sendJsonResponse(resp, HttpServletResponse.SC_OK, responseBody);


    }
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            super.service(req, resp);
        } catch (ValidationException e) {
            sendValidationErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, e.getErrors());
        } catch (EntityNotFoundException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (DuplicateEntityException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (DataAccessResourceFailureException e) {
            log("База данных недоступна!", e);
            sendErrorResponse(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Сервис временно недоступен.");
        } catch (DataAccessException e) {
            log("Непредвиденная ошибка базы данных.", e);
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Произошла ошибка на стороне сервера.");
        } catch (NumberFormatException | com.fasterxml.jackson.databind.JsonMappingException e) {
            // Ошибки парсинга JSON или параметров запроса
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный формат данных в запросе.");
        } catch (Exception e) {
            log("Непредвиденная ошибка базы данных.d", e);
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Произошла внутренняя ошибка приложения.");
        }
    }
}
