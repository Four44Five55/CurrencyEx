package org.example.service;

import org.example.dao.CurrencyDAO;
import org.example.exception.EntityNotFoundException;
import org.example.exception.ValidationException;
import org.example.model.Currency;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurrencyService {
    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final CentralBankService centralBankService = new CentralBankService();

    /**
     * Добавляет новую валюту. Валидирует данные и делегирует сохранение DAO.
     * DAO сам обработает ошибку дублирования.
     */
    public Currency addCurrency(String code, String fullName, String sign) {

        validateCurrencyFields(code, fullName, sign);


        Currency newCurrency = new Currency();
        newCurrency.setCode(code.toUpperCase());
        newCurrency.setFullName(fullName);
        newCurrency.setSign(sign);

        Currency savedCurrency = currencyDAO.save(newCurrency);

        fetchRateForNewCurrencyAsync(savedCurrency);

        return savedCurrency;
    }

    /**
     * Возвращает список всех валют.
     */
    public List<Currency> getAllCurrencies() {
        return currencyDAO.findAll();
    }

    /**
     * Находит валюту по коду. Если не найдена, бросает исключение.
     * Это основной метод для получения одной сущности.
     */
    public Currency getCurrencyByCode(String code) {
        return currencyDAO.findByCode(code.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Валюта", code));
    }

    /**
     * Обновляет существующую валюту.
     */
    public Currency updateCurrency(String code, String newFullName, String newSign) {
        validateCurrencyFields(code, newFullName, newSign);

        Currency currencyToUpdate = getCurrencyByCode(code);

        // Шаг 3: Обновляем поля
        currencyToUpdate.setFullName(newFullName);
        currencyToUpdate.setSign(newSign);

        currencyDAO.update(currencyToUpdate);

        return currencyToUpdate;
    }

    /**
     * Удаляет валюту по ее коду.
     */
    public void deleteCurrency(String code) {
        Currency currencyToDelete = getCurrencyByCode(code);

        currencyDAO.delete(currencyToDelete.getId());
    }

    private void fetchRateForNewCurrencyAsync(Currency currency) {

        try {
            System.out.println("Получение обменного курса для новой валюты: " + currency.getCode());
            centralBankService.updateRateForCurrency(currency);
        } catch (Exception e) {
            System.err.println("Не удалось получить обменный курс " + currency.getCode() + ". " +
                    "Валюта была создана, но ставка должна быть обновлена позже. Ошибка: " + e.getMessage());
        }
    }

    private void validateCurrencyFields(String code, String fullName, String sign) throws ValidationException {
        Map<String, String> validationErrors = new HashMap<>();

        if (code == null || code.isBlank()) {
            validationErrors.put("code", "Заполните код валюты (Code).");
        } else if (code.length() != 3) {
            validationErrors.put("code", "Код валюты (Code) должен быть не более 3 букв.");
        } else if (!code.matches("[a-zA-Z]+")) {
            validationErrors.put("code", "Код валюты (Code) должен содержать только латинские буквы.");
        }

        if (fullName == null || fullName.isBlank()) {
            validationErrors.put("fullName", "Заполните наименование валюты (Name).");
        } else if (fullName.length() > 100) {
            validationErrors.put("fullName", "Наименование валюты (Name) должно быть не более 100 букв.");
        }

        if (sign == null || sign.isBlank()) {
            validationErrors.put("sign", "Заполните поле знак валюты (Sign).");
        } else if (sign.length() > 5) {
            validationErrors.put("sign", "Знак валюты (Sign) не может быть больше 5 символов.");
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }
    }
}
