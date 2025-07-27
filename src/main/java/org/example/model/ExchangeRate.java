package org.example.model;

import java.math.BigDecimal;

public class ExchangeRate {
    private int id;
    private int idCurrency;
    private int nominal;
    private BigDecimal  rate;

    public ExchangeRate() {
    }

    public ExchangeRate(int id, int idCurrency, int nominal, double rate) {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdCurrency() {
        return idCurrency;
    }

    public void setIdCurrency(int idCurrency) {
        this.idCurrency = idCurrency;
    }

    public int getNominal() {
        return nominal;
    }

    public void setNominal(int nominal) {
        this.nominal = nominal;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal  rate) {
        this.rate = rate;
    }
}
