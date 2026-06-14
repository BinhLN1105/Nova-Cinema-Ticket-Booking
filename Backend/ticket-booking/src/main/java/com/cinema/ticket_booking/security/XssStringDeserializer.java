package com.cinema.ticket_booking.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;

/**
 * Jackson deserializer to clean up string values from HTML/XSS scripts before
 * binding to DTOs.
 */
public class XssStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }
        // Xóa hoàn toàn các thẻ HTML để chống XSS ở mọi request body dạng text
        String cleaned = Jsoup.clean(value, Safelist.none());
        return cleaned;
    }
}
