package org.example.dao;

import org.example.DatabaseManager;
import org.example.model.Currency;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CurrencyDAO {

    public Currency save(Currency currency) throws SQLException {
        String sql = "insert into currency (code, full_name, sign) values (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, currency.getCode().toUpperCase());
            statement.setString(2, currency.getFullName());
            statement.setString(3, currency.getSign());
            // Проверка, что строка действительно была добавлена
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to insert row into currency.");
            }
            // Получаем сгенерированный ID
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Устанавливаем ID в наш объект
                    currency.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating currency failed, no ID obtained.");
                }
            }
        }
        return currency;
    }

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

    public Optional<Currency> findByCode(String Code) throws SQLException {
        String sql = "select * from currency where code = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Code.toUpperCase());

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCurrency(rs));
                } else {
                    return Optional.empty();
                }
            }

        }
    }
    public Optional<Currency> findById(int id) throws SQLException {
        String sql = "select * from currency where id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapResultSetToCurrency(rs)) : Optional.empty();
            }
        }
    }


    public void update(Currency currency) throws SQLException {
        String sql = "UPDATE currency SET code = ?, full_name = ?, sign = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currency.getCode());
            statement.setString(2, currency.getFullName());
            statement.setString(3, currency.getSign());
            statement.setInt(4, currency.getId());

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Updating currency failed, no rows affected. Currency with id="
                        + currency.getId() + " not found.");
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM currency WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Deleting currency failed, no rows affected. Currency with id="
                        + id + " not found.");
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
