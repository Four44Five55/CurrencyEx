package org.example.service;

import org.example.dao.CurrencyDAO;
import org.example.dao.ExchangeRateDAO;
import org.example.exception.DuplicateEntityException;
import org.example.exception.EntityInUseException;
import org.example.exception.EntityNotFoundException;
import org.example.exception.ValidationException;
import org.example.model.Currency;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CurrencyService {
    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ExchangeRateDAO exchangeRateDAO = new ExchangeRateDAO();
    private final CentralBankService centralBankService = new CentralBankService();

    public Currency addCurrency(String code, String fullName, String sign) throws SQLException, ValidationException, DuplicateEntityException {

        validateCurrencyFields(code, fullName, sign);

        String upperCaseCode = code.toUpperCase();
        //проверка на наличие валюты в бд
        if (currencyDAO.findByCode(upperCaseCode).isPresent()) {
            throw new DuplicateEntityException("Currency", upperCaseCode);
        }

        Currency newCurrency = new Currency();
        newCurrency.setCode(upperCaseCode);
        newCurrency.setFullName(fullName);
        newCurrency.setSign(sign);

        Currency savedCurrency = currencyDAO.save(newCurrency);

        // 2. СРАЗУ ПОСЛЕ СОХРАНЕНИЯ, пытаемся подтянуть для нее курс
        try {
            System.out.println("Attempting to fetch exchange rate for new currency: " + savedCurrency.getCode());
            centralBankService.updateRateForCurrency(savedCurrency);
        } catch (Exception e) {
            // Если не удалось получить курс (нет интернета, нет данных в ЦБ),
            // это не должно отменять создание самой валюты.
            // Просто логируем ошибку.
            System.err.println("Could not fetch exchange rate for " + savedCurrency.getCode() + ". " +
                    "The currency was created, but the rate needs to be updated later. Error: " + e.getMessage());
            // В реальном приложении здесь был бы вызов логгера, e.g., log.warn(...)
        }

        // Возвращаем созданную валюту
        return savedCurrency;
    }


    public List<Currency> getAllCurrencies() throws SQLException {
        return currencyDAO.findAll();
    }

    public Optional<Currency> getCurrencyByCode(String code) throws SQLException, EntityNotFoundException {
        return Optional.ofNullable(currencyDAO.findByCode(code.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Currency", code)));
    }

    public Currency updateCurrency(String code, String newFullName, String newSign) throws SQLException, ValidationException, DuplicateEntityException {
        validateCurrencyFields(code, newFullName, newSign);

        Optional<Currency> currencyToUpdate = getCurrencyByCode(code);

        currencyToUpdate.get().setFullName(newFullName);
        currencyToUpdate.get().setSign(newSign);

        currencyDAO.update(currencyToUpdate.orElse(null));

        return currencyToUpdate.orElse(null);
    }

    public void deleteCurrency(String code) throws SQLException, EntityNotFoundException, EntityInUseException {
        // 1. Находим валюту по ее бизнес-ключу.
        Currency currencyToDelete = currencyDAO.findByCode(code.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Currency", code));

        // 2. БИЗНЕС-ПРАВИЛО: Проверяем, не используется ли эта валюта в таблице курсов.
        //    Это КЛЮЧЕВАЯ задача сервисного слоя - координация нескольких DAO!
        //    Вызываем метод из ДРУГОГО DAO.
        if (exchangeRateDAO.isCurrencyUsed(currencyToDelete.getId())) {
            // Если используется, бросаем специальное исключение.
            throw new EntityInUseException("Cannot delete currency '" + code + "' because it is used in exchange rates.");
        }

        // 3. Если все проверки пройдены - удаляем.
        currencyDAO.delete(currencyToDelete.getId());
    }


    private void validateCurrencyFields(String code, String fullName, String sign) throws ValidationException {
        Map<String, String> validationErrors = new HashMap<>();

        if (code == null || code.isBlank()) {
            validationErrors.put("code", "Currency code is required.");
        } else if (code.length() != 3) {
            validationErrors.put("code", "Currency code must be 3 characters long.");
        } else if (!code.matches("[a-zA-Z]+")) {
            validationErrors.put("code", "Currency code must contain only letters.");
        }

        if (fullName == null || fullName.isBlank()) {
            validationErrors.put("fullName", "Full name is required.");
        } else if (fullName.length() > 100) {
            validationErrors.put("fullName", "Full name cannot be longer than 100 characters.");
        }

        if (sign == null) {
            validationErrors.put("sign", "Currency sign is required (can be empty, but not null).");
        } else if (sign.length() > 5) {
            validationErrors.put("sign", "Currency sign cannot be longer than 5 characters.");
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }
    }
}
