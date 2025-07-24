package org.example.dao;

import org.example.DatabaseManager;
import org.example.model.Currency;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CurrencyDAO {
    public List<Currency> findAll() throws SQLException {
        List<Currency> currencies = new ArrayList<Currency>();
        String sql = "select * from currency";
        try (
                Connection connection = DatabaseManager.getConnection();
                // Используем PreparedStatement для безопасности и производительности
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                currencies.add(mapResultSetToCurrency(resultSet));
            }

        }
        return currencies;
    }

    public Optional<Currency> findByCharCode(String charCode) throws SQLException {
        String sql = "select * from currency where code = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, charCode.toUpperCase());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCurrency(rs));
                } else {
                    return Optional.empty();
                }
            }

        }
    }


    private Currency mapResultSetToCurrency(ResultSet rs) throws SQLException {
        Currency currency = new Currency();
        currency.setId(rs.getInt("id"));
        currency.setCode(rs.getString("code"));
        currency.setFullName(rs.getString("full_name"));
        currency.setSign(rs.getString("sign"));
        return currency;
    }

}
