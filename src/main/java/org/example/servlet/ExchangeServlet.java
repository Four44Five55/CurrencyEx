package org.example.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.service.ExchangeRateService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static org.example.JsonResponseUtil.sendErrorResponse;
import static org.example.JsonResponseUtil.sendJsonResponse;

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
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, amountStr);
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
}
