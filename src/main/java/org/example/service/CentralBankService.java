package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dao.CurrencyDAO;
import org.example.dao.ExchangeRateDAO;
import org.example.model.Currency;
import org.example.model.ExchangeRate;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CentralBankService {
    private static final String CBR_API_URL = "https://www.cbr-xml-daily.ru/daily_json.js";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ExchangeRateDAO exchangeRateDAO = new ExchangeRateDAO();

    /**
     * Основной метод, который обновляет курсы валют из API ЦБ.
     */
    public void updateAllExchangeRates() throws IOException, InterruptedException, SQLException {
        // 1. Получаем JSON с курсами от ЦБ
        JsonNode valuteNode = fetchRatesAndGetValuteNode();

        // 2. Получаем список всех валют из нашей БД
        List<Currency> ourCurrencies = currencyDAO.findAll();

        // 3. Проходим по нашим валютам и обновляем курсы для каждой
        for (Currency currency : ourCurrencies) {
            updateRateForCurrency(currency, valuteNode);
        }
    }

    /**
     * Обновляет или создает курс для ОДНОЙ конкретной валюты.
     * Этот метод можно будет вызвать из другого сервиса, например, при добавлении новой валюты.
     *
     * @param currency Объект валюты, для которой нужно обновить курс.
     */
    public void updateRateForCurrency(Currency currency) throws IOException, InterruptedException, SQLException {
        JsonNode valuteNode = fetchRatesAndGetValuteNode();
        updateRateForCurrency(currency, valuteNode);
    }

    /**
     * Приватный метод, содержащий основную логику обновления/создания курса.
     *
     * @param currency   Валюта для обновления.
     * @param valuteNode Корневой узел 'Valute' из ответа ЦБ.
     */
    private void updateRateForCurrency(Currency currency, JsonNode valuteNode) throws SQLException {
        String currencyCode = currency.getCode();
        JsonNode currencyData = valuteNode.path(currencyCode);

        if (currencyData.isMissingNode()) {
            System.out.println("No data for " + currencyCode + " in CBR response. Skipping.");
            return;
        }

        int nominal = currencyData.path("Nominal").asInt();
        BigDecimal rate = new BigDecimal(currencyData.path("Value").asText());

        Optional<ExchangeRate> existingRateOpt = exchangeRateDAO.findByCurrencyCode(currencyCode);

        if (existingRateOpt.isPresent()) {
            ExchangeRate rateToUpdate = existingRateOpt.get();
            rateToUpdate.setNominal(nominal);
            rateToUpdate.setRate(rate);
            exchangeRateDAO.update(rateToUpdate);
            System.out.println("Updated rate for " + currencyCode);
        } else {
            ExchangeRate newRate = new ExchangeRate();
            newRate.setIdCurrency(currency.getId());
            newRate.setNominal(nominal);
            newRate.setRate(rate);
            exchangeRateDAO.save(newRate);
            System.out.println("Created new rate for " + currencyCode);
        }
    }

    /**
     * Приватный хелпер для получения данных от ЦБ.
     */
    private JsonNode fetchRatesAndGetValuteNode() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(CBR_API_URL)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch rates from CBR. Status code: " + response.statusCode());
        }
        JsonNode rootNode = objectMapper.readTree(response.body());
        return rootNode.path("Valute");
    }
}

