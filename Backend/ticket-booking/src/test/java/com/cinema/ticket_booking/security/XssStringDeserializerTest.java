package com.cinema.ticket_booking.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XssStringDeserializerTest {

    @Mock
    private JsonParser jsonParser;

    @Mock
    private DeserializationContext deserializationContext;

    @Test
    void testDeserialize_NullValue() throws IOException {
        XssStringDeserializer deserializer = new XssStringDeserializer();
        when(jsonParser.getValueAsString()).thenReturn(null);

        String result = deserializer.deserialize(jsonParser, deserializationContext);
        assertNull(result);
    }

    @Test
    void testDeserialize_CleanString() throws IOException {
        XssStringDeserializer deserializer = new XssStringDeserializer();
        when(jsonParser.getValueAsString()).thenReturn("Hello World");

        String result = deserializer.deserialize(jsonParser, deserializationContext);
        assertEquals("Hello World", result);
    }

    @Test
    void testDeserialize_DirtyStringWithHtml() throws IOException {
        XssStringDeserializer deserializer = new XssStringDeserializer();
        when(jsonParser.getValueAsString()).thenReturn("<script>alert('XSS')</script>Hello <p>World</p>");

        String result = deserializer.deserialize(jsonParser, deserializationContext);
        assertEquals("Hello World", result);
    }
}
