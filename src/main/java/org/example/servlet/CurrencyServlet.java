package org.example.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.exception.*;
import org.example.model.Currency;
import org.example.service.CurrencyService;

import java.io.IOException;
import java.util.List;

import static org.example.JsonResponseUtil.*;

@WebServlet("/currency/*")
public class CurrencyServlet extends HttpServlet {
    private final CurrencyService currencyService = new CurrencyService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            List<Currency> currencies = currencyService.getAllCurrencies();
            sendJsonResponse(resp, HttpServletResponse.SC_OK, currencies);
            return;
        }

        String[] pathParts = pathInfo.split("/");
        if (pathParts.length == 2) {
            String currencyCode = pathParts[1];
            Currency currency = currencyService.getCurrencyByCode(currencyCode);
            sendJsonResponse(resp, HttpServletResponse.SC_OK, currency);
            return;
        }


        sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный URL.");
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        // Проверка запроса URL
        if (pathInfo != null && !pathInfo.equals("/")) {
            sendErrorResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "Некорректный URL не поддерживается POST.");
            return;
        }

        //Получение данных из тела
        String code = req.getParameter("code");
        String fullName = req.getParameter("fullName");
        String sign = req.getParameter("sign");

        currencyService.addCurrency(code, fullName, sign);

        Currency currency = new Currency();
        currency.setCode(code);
        currency.setFullName(fullName);
        currency.setSign(sign);


        sendJsonResponse(resp, HttpServletResponse.SC_CREATED, currency);

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
        } catch (EntityInUseException e) {
            sendErrorResponse(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (DataAccessResourceFailureException e) {
            // Ошибка доступа к БД
            log("Database is unavailable!", e);
            sendErrorResponse(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Сервис временно недоступен. Попробуйте позже.");
        } catch (DataAccessException e) {
            log("An unexpected database error occurred", e);
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Произошла ошибка на стороне сервера.");
        } catch (Exception e) {
            // непредвиденные ошибок
            log("An unexpected application error occurred", e);
            sendErrorResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Произошла внутренняя ошибка приложения.");
        }
    }

}