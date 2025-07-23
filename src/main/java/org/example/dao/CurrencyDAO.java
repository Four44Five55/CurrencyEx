package org.example.dao;

import org.example.DatabaseManager;
import org.example.model.Currency;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CurrencyDAO {
    public List<Currency> findAll() {
        List<Currency> currencies = new ArrayList<Currency>();
        String sql = "select * from currency";
        try (Connection connection = DatabaseManager.getConnection();
             // Используем PreparedStatement для безопасности и производительности
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Currency currency = new Currency();
                currency.setId(resultSet.getInt("id"));
                currency.setCharCode(resultSet.getString("char_code"));
                currency.setFullName(resultSet.getString("full_name"));
                currency.setSign(resultSet.getString("sign"));
                currency.setNumCode(resultSet.getInt("num_code"));
                currencies.add(currency);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return currencies;
    }
}
