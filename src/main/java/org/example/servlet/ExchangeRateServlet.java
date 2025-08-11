package org.example.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.exception.*;
import org.example.model.ExchangeRate;
import org.example.service.ExchangeRateService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.example.JsonResponseUtil.*;

@WebServlet("/exchangeRate/*")
public class ExchangeRateServlet extends HttpServlet {
    private final ExchangeRateService service = new ExchangeRateService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            List<ExchangeRate> rates = service.getAllExchangeRates();
            sendJsonResponse(resp, HttpServletResponse.SC_OK, rates);
            return;
        }

        String[] pathParts = pathInfo.split("/");
        if (pathParts.length == 2) {
            String currencyCode = pathParts[1];
            ExchangeRate rate = service.getExchangeRateByCode(currencyCode);
            sendJsonResponse(resp, HttpServletResponse.SC_OK, rate);
            return;
        }

        sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный URL.");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.equals("/")) {
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "URL не поддерживается POST.");
            return;
        }

        String idCurrency = req.getParameter("idCurrency");
        int nominal = Integer.parseInt(req.getParameter("nominal"));
        BigDecimal rate = new BigDecimal(req.getParameter("rate"));

        service.addExchangeRate(idCurrency, nominal, rate);

        ExchangeRate correctedRate = new ExchangeRate();
        correctedRate.setNominal(nominal);
        correctedRate.setRate(rate);
        sendJsonResponse(resp, HttpServletResponse.SC_CREATED, correctedRate);
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
