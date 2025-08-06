package org.example.service;

import org.example.dao.CurrencyDAO;
import org.example.dao.ExchangeRateDAO;
import org.example.exception.DuplicateEntityException;
import org.example.exception.EntityNotFoundException;
import org.example.exception.ValidationException;
import org.example.model.Currency;
import org.example.model.ExchangeRate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ExchangeRateService {
    private static final String BASE_CURRENCY_CODE = "RUB";
    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ExchangeRateDAO exchangeRateDAO = new ExchangeRateDAO();

    public ExchangeRate addExchangeRate(String currencyCode, int nominal, BigDecimal rate) throws SQLException, ValidationException, DuplicateEntityException {
        validateExchangeRateFields(currencyCode, nominal, rate);

        Currency currency = currencyDAO.findByCode(currencyCode)
                .orElseThrow(() -> new EntityNotFoundException("Currency", currencyCode));

        // Проверка на дубликат курса для этой валюты
        if (exchangeRateDAO.isCurrencyUsed(currency.getId())) {
            throw new DuplicateEntityException("ExchangeRate", currency.getCode());
        }
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setIdCurrency(currency.getId());
        exchangeRate.setNominal(nominal);
        exchangeRate.setRate(rate);

        return exchangeRateDAO.save(exchangeRate);
    }

    public List<ExchangeRate> getAllExchangeRates() throws SQLException {
        return exchangeRateDAO.findAll();
    }

    public Optional<ExchangeRate> getExchangeRateByCode(String currencyCode) throws SQLException {
        return exchangeRateDAO.findByCurrencyCode(currencyCode);
    }

    public ExchangeRate updateExchangeRate(String currencyCode, int nominal, BigDecimal rate) throws SQLException, ValidationException {
        validateExchangeRateFields(currencyCode, nominal, rate);

        Optional<ExchangeRate> exchangeRate = getExchangeRateByCode(currencyCode);
        exchangeRate.get().setNominal(nominal);
        exchangeRate.get().setRate(rate);
        return exchangeRateDAO.save(exchangeRate.get());
    }

    public void deleteExchangeRate(String currencyCode) throws SQLException, EntityNotFoundException {
        ExchangeRate rateToDelete = exchangeRateDAO.findByCurrencyCode(currencyCode.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("ExchangeRate", currencyCode));
        exchangeRateDAO.delete(rateToDelete.getId());
    }

    public BigDecimal convertAmount(BigDecimal amount, BigDecimal rate)
            throws SQLException, EntityNotFoundException {
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCrossRate(String fromCode, String toCode)
            throws SQLException, EntityNotFoundException {

        // Случай 0: Валюты совпадают
        if (fromCode.equals(toCode)) {
            return BigDecimal.ONE;
        }

        // Случай 1: Конвертация ИЗ РУБЛЯ в другую валюту (RUB -> USD)
        if (fromCode.equals(BASE_CURRENCY_CODE)) {
            ExchangeRate toRate = exchangeRateDAO.findByCurrencyCode(toCode)
                    .orElseThrow(() -> new EntityNotFoundException("Exchange rate for " + toCode, "not found"));

            // Нам нужен курс 1 / (USD -> RUB)
            BigDecimal ratePerOneUnit = toRate.getRate().divide(
                    BigDecimal.valueOf(toRate.getNominal()), 12, RoundingMode.HALF_UP);

            return BigDecimal.ONE.divide(ratePerOneUnit, 12, RoundingMode.HALF_UP);
        }

        // Случай 2: Конвертация В РУБЛЬ из другой валюты (USD -> RUB)
        if (toCode.equals(BASE_CURRENCY_CODE)) {
            ExchangeRate fromRate = exchangeRateDAO.findByCurrencyCode(fromCode)
                    .orElseThrow(() -> new EntityNotFoundException("Exchange rate for " + fromCode, "not found"));

            // Просто возвращаем курс этой валюты к рублю
            return fromRate.getRate().divide(
                    BigDecimal.valueOf(fromRate.getNominal()), 12, RoundingMode.HALF_UP);
        }

        // Случай 3: Кросс-курс между двумя НЕ-РУБЛЕВЫМИ валютами (USD -> EUR)
        // (Этот блок остается таким же, как мы писали ранее)
        ExchangeRate fromRate = exchangeRateDAO.findByCurrencyCode(fromCode)
                .orElseThrow(() -> new EntityNotFoundException("Exchange rate for " + fromCode, "not found"));
        ExchangeRate toRate = exchangeRateDAO.findByCurrencyCode(toCode)
                .orElseThrow(() -> new EntityNotFoundException("Exchange rate for " + toCode, "not found"));

        BigDecimal fromRatePerOneUnit = fromRate.getRate().divide(
                BigDecimal.valueOf(fromRate.getNominal()), 12, RoundingMode.HALF_UP);

        BigDecimal toRatePerOneUnit = toRate.getRate().divide(
                BigDecimal.valueOf(toRate.getNominal()), 12, RoundingMode.HALF_UP);

        // Формула: (EUR -> RUB) / (USD -> RUB) = курс USD -> EUR
        return toRatePerOneUnit.divide(fromRatePerOneUnit, 12, RoundingMode.HALF_UP);
    }

    private void validateExchangeRateFields(String currencyCode, Integer nominal, BigDecimal rate) throws ValidationException {
        Map<String, String> validationErrors = new HashMap<>();

        if (currencyCode == null || currencyCode.isBlank()) {
            validationErrors.put("code", "Currency code is required.");
        } else if (currencyCode.length() != 3) {
            validationErrors.put("code", "Currency code must be 3 characters long.");
        } else if (!currencyCode.matches("[a-zA-Z]+")) {
            validationErrors.put("code", "Currency code must contain only letters.");
        }

        // nominal: обязательно, положительное число
        if (nominal == null) {
            validationErrors.put("nominal", "Nominal is required.");
        } else if (nominal <= 0) {
            validationErrors.put("nominal", "Nominal must be a positive integer.");
        }

        // rate: обязательно, положительное число, не больше 12 знаков всего и 6 после запятой
        if (rate == null) {
            validationErrors.put("rate", "Rate is required.");
        } else if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            validationErrors.put("rate", "Rate must be a positive number.");
        } else {
            // Проверка на максимальное количество знаков
            int precision = rate.precision(); // всего знаков
            int scale = rate.scale();         // знаков после запятой
            if (precision > 12) {
                validationErrors.put("rate", "Rate cannot have more than 12 digits in total.");
            }
            if (scale > 6) {
                validationErrors.put("rate", "Rate cannot have more than 6 digits after the decimal point.");
            }
        }

        if (!validationErrors.isEmpty()) {
            throw new ValidationException(validationErrors);
        }
    }
}
