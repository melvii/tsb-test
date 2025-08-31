package com.tsb.banking.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Converter(autoApply = false)
public class AccountNumberConverter implements AttributeConverter<String, String> {

    private static AesCryptoService cryptoService;

    @Autowired
    public void setCryptoService(AesCryptoService s) {
        cryptoService = s;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return cryptoService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return cryptoService.decrypt(dbData);
    }
}
