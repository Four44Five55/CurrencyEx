package org.example.dao;

import org.example.DatabaseManager;
import org.example.exception.DataAccessException;
import org.example.exception.DuplicateEntityException;
import org.example.exception.EntityNotFoundException;
import org.example.model.ExchangeRate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.example.SQLiteExceptionTranslator.isUniqueConstraintError;
import static org.example.SQLiteExceptionTranslator.translateToGeneralError;

public class ExchangeRateDAO {

    public ExchangeRate save(ExchangeRate exchangeRate) {
        String sql = "INSERT INTO exchange_rate (id_currency, nominal, rate) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, exchangeRate.getIdCurrency());
            statement.setInt(2, exchangeRate.getNominal());
            statement.setBigDecimal(3, exchangeRate.getRate());
            // Проверка, что строка действительно была добавлена
            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                throw new DataAccessException("Не удалось создать обменный курс, строка не добавлена.", null);
            }
            // Получаем сгенерированный ID
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Устанавливаем ID в наш объект
                    exchangeRate.setId(generatedKeys.getInt(1));
                } else {
                    throw new DataAccessException("Не удалось создать обменный курс, id не получен.", null);
                }
            }
            return exchangeRate;
        } catch (SQLException e) {
            // Проверяем на дубликат
            if (isUniqueConstraintError(e)) {
                throw new DuplicateEntityException("Обменный курс", "для валюты id= " + exchangeRate.getIdCurrency());
            }
            // Для всех остальных проблем - общий обработчик
            throw translateToGeneralError("сохранение обменного курса", e);
        }
    }

    public List<ExchangeRate> findAll() {
        List<ExchangeRate> exchangeRates = new ArrayList<>();
        String sql = "select * from exchange_rate";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                exchangeRates.add(mapResultSetToExchangeRate(resultSet));
            }
        } catch (SQLException e) {
            throw translateToGeneralError("Получение всех обменных курсов", e);
        }
        return exchangeRates;
    }

    public Optional<ExchangeRate> findByCurrencyCode(String currencyCode) {
        String sql = "select ex.id, ex.id_currency, ex.nominal, ex.rate " +
                "from exchange_rate ex " +
                "JOIN currency c ON ex.id_currency = c.id " +
                "where c.code = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currencyCode.toUpperCase());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToExchangeRate(resultSet));
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw translateToGeneralError("поиск обменного курса по идентификатору валюты", e);
        }
    }

    public void update(ExchangeRate rate) {
        String sql = "UPDATE exchange_rate SET nominal = ?, rate = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {

            statement.setInt(1, rate.getNominal());
            statement.setBigDecimal(2, rate.getRate());
            statement.setInt(3, rate.getId()); // ID для условия WHERE

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new EntityNotFoundException("Обменный курс", "для валюты id= " + rate.getIdCurrency());
            }
        } catch (SQLException e) {
            if (isUniqueConstraintError(e)) {
                throw new DuplicateEntityException("Обменный курс", "для валюты id= " + rate.getIdCurrency());
            }
            throw translateToGeneralError("изменение обменного курса по идентификатору валюты", e);
        }

    }

    public void delete(int id) {
        String sql = "DELETE FROM exchange_rate WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, id);
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new EntityNotFoundException("Обменный курс", String.valueOf(id));
            }
        } catch (SQLException e) {
            throw translateToGeneralError("удаление обменного курса", e);
        }
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


