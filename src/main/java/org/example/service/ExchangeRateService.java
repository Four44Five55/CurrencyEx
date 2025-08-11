package org.example.service;

import org.example.dao.CurrencyDAO;
import org.example.dao.ExchangeRateDAO;
import org.example.exception.EntityNotFoundException;
import org.example.exception.ValidationException;
import org.example.model.Currency;
import org.example.model.ExchangeRate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExchangeRateService {
    private static final String BASE_CURRENCY_CODE = "RUB";
    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ExchangeRateDAO exchangeRateDAO = new ExchangeRateDAO();

    public ExchangeRate addExchangeRate(String currencyCode, int nominal, BigDecimal rate) {
        validateExchangeRateFields(currencyCode, nominal, rate);

        Currency currency = currencyDAO.findByCode(currencyCode)
                .orElseThrow(() -> new EntityNotFoundException("Валюта", currencyCode));

        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setIdCurrency(currency.getId());
        exchangeRate.setNominal(nominal);
        exchangeRate.setRate(rate);

        return exchangeRateDAO.save(exchangeRate);
    }

    public List<ExchangeRate> getAllExchangeRates() {
        return exchangeRateDAO.findAll();
    }

    public ExchangeRate getExchangeRateByCode(String currencyCode) {
        return exchangeRateDAO.findByCurrencyCode(currencyCode.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Обменный курс для валюты", currencyCode));
    }

    public ExchangeRate updateExchangeRate(String currencyCode, int nominal, BigDecimal rate) {
        validateExchangeRateFields(currencyCode, nominal, rate);
        ExchangeRate exchangeRate = getExchangeRateByCode(currencyCode);
        exchangeRate.setNominal(nominal);
        exchangeRate.setRate(rate);
        exchangeRateDAO.update(exchangeRate);

        return exchangeRate;
    }

    public void deleteExchangeRate(String currencyCode) throws EntityNotFoundException {
        ExchangeRate rateToDelete = getExchangeRateByCode(currencyCode);
        exchangeRateDAO.delete(rateToDelete.getId());
    }

    public BigDecimal convertAmount(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCrossRate(String fromCode, String toCode) {

        // Валюты совпадают
        if (fromCode.equals(toCode)) {
            return BigDecimal.ONE;
        }

        // Конвертация ИЗ РУБЛЯ в другую валюту (RUB -> USD)
        if (fromCode.equals(BASE_CURRENCY_CODE)) {
            ExchangeRate toRate = exchangeRateDAO.findByCurrencyCode(toCode)
                    .orElseThrow(() -> new EntityNotFoundException("Обменный курс валюты " + toCode, "не найден."));

            // Нам нужен курс 1 / (USD -> RUB)
            BigDecimal ratePerOneUnit = toRate.getRate().divide(
                    BigDecimal.valueOf(toRate.getNominal()), 12, RoundingMode.HALF_UP);

            return BigDecimal.ONE.divide(ratePerOneUnit, 12, RoundingMode.HALF_UP);
        }

        //  Конвертация В РУБЛЬ из другой валюты (USD -> RUB)
        if (toCode.equals(BASE_CURRENCY_CODE)) {
            ExchangeRate fromRate = exchangeRateDAO.findByCurrencyCode(fromCode)
                    .orElseThrow(() -> new EntityNotFoundException("Обменный курс валюты " + fromCode, "не найден."));

            // Просто возвращаем курс этой валюты к рублю
            return fromRate.getRate().divide(
                    BigDecimal.valueOf(fromRate.getNominal()), 12, RoundingMode.HALF_UP);
        }

        // Кросс-курс между двумя НЕ-РУБЛЕВЫМИ валютами (USD -> EUR)
        // (Этот блок остается таким же, как мы писали ранее)
        ExchangeRate fromRate = exchangeRateDAO.findByCurrencyCode(fromCode)
                .orElseThrow(() -> new EntityNotFoundException("Обменный курс валюты " + fromCode, "не найден."));
        ExchangeRate toRate = exchangeRateDAO.findByCurrencyCode(toCode)
                .orElseThrow(() -> new EntityNotFoundException("Обменный курс валюты " + toCode, "не найден."));

        BigDecimal fromRatePerOneUnit = fromRate.getRate().divide(
                BigDecimal.valueOf(fromRate.getNominal()), 12, RoundingMode.HALF_UP);

        BigDecimal toRatePerOneUnit = toRate.getRate().divide(
                BigDecimal.valueOf(toRate.getNominal()), 12, RoundingMode.HALF_UP);

        // Формула: (EUR -> RUB) / (USD -> RUB) = курс USD -> EUR
        return fromRatePerOneUnit.divide(toRatePerOneUnit, 12, RoundingMode.HALF_UP);
    }

    private void validateExchangeRateFields(String currencyCode, Integer nominal, BigDecimal rate) throws ValidationException {
        Map<String, String> validationErrors = new HashMap<>();

        // --- Валидация кода валюты ---
        if (currencyCode == null || currencyCode.isBlank()) {
            validationErrors.put("code", "Код валюты является обязательным полем.");
        } else if (currencyCode.length() != 3) {
            validationErrors.put("code", "Код валюты должен состоять из 3 символов.");
        } else if (!currencyCode.matches("[a-zA-Z]+")) {
            validationErrors.put("code", "Код валюты должен содержать только буквы.");
        }

        // --- Валидация номинала ---
        if (nominal == null) {
            validationErrors.put("nominal", "Номинал является обязательным полем.");
        } else if (nominal <= 0) {
            validationErrors.put("nominal", "Номинал должен быть положительным числом.");
        }

        // --- Валидация курса ---
        if (rate == null) {
            validationErrors.put("rate", "Курс является обязательным полем.");
        } else if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            validationErrors.put("rate", "Курс должен быть положительным числом.");
        } else {
            // Улучшенная проверка на количество знаков.
            // precision() - scale() считает количество цифр ДО запятой.
            if (rate.precision() - rate.scale() > 6) {
                // Пример: 1234567.89 -> precision=9, scale=2. 9-2=7 > 6. Ошибка.
                // Пример: 123456.789 -> precision=9, scale=3. 9-3=6. OK.
                validationErrors.put("rate", "В курсе не может быть более 6 цифр до запятой.");
            }
            if (rate.scale() > 6) {
                validationErrors.put("rate", "В курсе не может быть более 6 цифр после запятой.");
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }
    }
}
