package org.example.dao;

import org.example.DatabaseManager;
import org.example.exception.*;
import org.example.model.Currency;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.example.SQLiteExceptionTranslator.*;

public class CurrencyDAO {

    public Currency save(Currency currency) {
        String sql = "insert into currency (code, full_name, sign) values (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, currency.getCode().toUpperCase());
            statement.setString(2, currency.getFullName());
            statement.setString(3, currency.getSign());
            // Проверка, что строка действительно была добавлена
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Не удалось вставить строку в валюту.");
            }
            // Получаем сгенерированный ID
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Устанавливаем ID в наш объект
                    currency.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Не удалось создать валюту, id не получен.");
                }
            }
            return currency;
        } catch (SQLException e) {
            // Проверяем на дубликат
            if (isUniqueConstraintError(e)) {
                throw new DuplicateEntityException("Валюта", currency.getCode());
            }
            // Для всех остальных проблем - общий обработчик
            throw translateToGeneralError("сохранение валюты", e);
        }


    }

    public List<Currency> findAll() {
        List<Currency> currencies = new ArrayList<Currency>();
        String sql = "select * from currency limit 501";
        try (
                Connection connection = DatabaseManager.getConnection();
                // Используем PreparedStatement для безопасности и производительности
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                currencies.add(mapResultSetToCurrency(resultSet));
            }

        } catch (SQLException e) {
            throw new DataAccessException("Ошибка. База данных недоступна).", e);
        }
        return currencies;
    }

    public Optional<Currency> findByCode(String Code) {
        String sql = "select * from currency where code = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, Code.toUpperCase());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResultSetToCurrency(resultSet));
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw translateToGeneralError("поиск валюты по идентификатору", e);
        }
    }

    public Optional<Currency> findById(int id) {
        String sql = "select * from currency where id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapResultSetToCurrency(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Ошибка. База данных недоступна).", e);
        }
    }


    public void update(Currency currency) {
        String sql = "UPDATE currency SET code = ?, full_name = ?, sign = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currency.getCode());
            statement.setString(2, currency.getFullName());
            statement.setString(3, currency.getSign());
            statement.setInt(4, currency.getId());

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                throw new EntityNotFoundException("Валюта", currency.getCode());

            }
        } catch (SQLException e) {
            if (isUniqueConstraintError(e)) {
                throw new DuplicateEntityException("Валюта", currency.getCode());
            }
            throw translateToGeneralError("изменение валюты", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM currency WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                throw new EntityNotFoundException("Валюта", String.valueOf(id));
            }
        } catch (SQLException e) {
            // Проверяем на нарушение внешнего ключа
            if (isForeignKeyConstraintError(e)) {
                throw new EntityInUseException("Невозможно удалить валюту с " + id + " потому что она используется.");
            }
            throw translateToGeneralError("удаление валюты", e);
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
