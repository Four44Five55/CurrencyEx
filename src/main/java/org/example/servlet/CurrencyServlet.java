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

@WebServlet("/currency")
public class CurrencyServlet extends HttpServlet {
    // В классе CurrencyServlet
    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Получаем список объектов. Если внутри DAO произойдет SQLException,
        // он будет обернут в RuntimeException, и выполнение этого метода прервется.
        // Tomcat сам обработает этот RuntimeException и вернет клиенту ошибку 500.
        List<Currency> currencies = currencyDAO.findAll();

        // Если мы дошли сюда, значит, ошибки не было.
        // Готовим успешный JSON-ответ.
        resp.setContentType("application/json; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        objectMapper.writeValue(resp.getWriter(), currencies);
    }
}