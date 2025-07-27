package org.example.dao;

import org.example.DatabaseManager;
import org.example.model.ExchangeRate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExchangeRateDAO {

    public ExchangeRate save(ExchangeRate exchangeRate) throws SQLException {
        String sql = "INSERT INTO exchange_rate (id_currency, nominal, rate) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, exchangeRate.getIdCurrency());
            statement.setInt(2, exchangeRate.getNominal());
            statement.setBigDecimal(3, exchangeRate.getRate());

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Creating exchange rate failed, no rows affected.");
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    exchangeRate.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating exchange rate failed, no ID obtained.");
                }
            }

        }
        return exchangeRate;
    }

    public List<ExchangeRate> findAll() throws SQLException {
        List<ExchangeRate> exchangeRates = new ArrayList<>();
        String sql = "select * from exchange_rate";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                exchangeRates.add(mapResultSetToExchangeRate(resultSet));
            }
        }
        return exchangeRates;
    }

    public Optional<ExchangeRate> findByCurrencyCode(String currencyCode) throws SQLException {
        String sql = "select ex.id, ex.id_currency, ex.nominal, ex.rate " +
                "from exchange_rate ex " +
                "JOIN currency c ON ex.id_currency = c.id " +
                "where c.code = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, currencyCode.toUpperCase());
            // ResultSet тоже должен быть в try-with-resources
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToExchangeRate(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public void update(ExchangeRate rate) throws SQLException {
        String sql = "UPDATE exchange_rate SET nominal = ?, rate = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setInt(1, rate.getNominal());
            statement.setBigDecimal(2, rate.getRate());
            statement.setInt(3, rate.getId()); // ID для условия WHERE

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Updating exchange rate failed, no rows affected. Rate with id=" + rate.getId() + " not found.");
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM exchange_rate WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting exchange rate failed, no rows affected. ExchangeRate with id=" + id + " not found.");
            }
        }
    }

    public boolean isCurrencyUsed(int currencyId) throws SQLException {
        String sql = "select * from exchange_rate where id_currency = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, currencyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private ExchangeRate mapResultSetToExchangeRate(ResultSet resultSet) throws SQLException {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setId(resultSet.getInt("id"));
        exchangeRate.setIdCurrency(resultSet.getInt("id_currency"));
        exchangeRate.setNominal(resultSet.getInt("nominal"));
        exchangeRate.setRate(resultSet.getBigDecimal("rate"));
        return exchangeRate;
    }
}


