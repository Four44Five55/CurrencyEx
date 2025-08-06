package org.example.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.exception.EntityNotFoundException;
import org.example.service.ExchangeRateService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

import static org.example.JsonResponseUtil.sendErrorResponse;
import static org.example.JsonResponseUtil.sendJsonResponse;

@WebServlet("/exchange")
public class ExchangeServlet extends HttpServlet {
    private final ExchangeRateService exchangeRateService = new ExchangeRateService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fromCurrencyCode = req.getParameter("from");
        String toCurrencyCode = req.getParameter("to");
        String amountStr = req.getParameter("amount");

        if (fromCurrencyCode == null || toCurrencyCode == null || amountStr == null ||
                fromCurrencyCode.isBlank() || toCurrencyCode.isBlank() || amountStr.isBlank()) {
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, amountStr);
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.signum() < 0) { // Проверяем, что сумма не отрицательная
                sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Amount must be a non-negative number.");
                return;
            }

        } catch (NumberFormatException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid 'amount' format. It must be a number.");
            return;
        }

        try {
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

        } catch (EntityNotFoundException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }
}
