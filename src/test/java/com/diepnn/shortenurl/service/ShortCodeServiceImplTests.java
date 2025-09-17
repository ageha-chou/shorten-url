package com.diepnn.shortenurl.service;

import com.diepnn.shortenurl.common.generator.IdGenerator;
import com.diepnn.shortenurl.common.generator.ShortenUrlGenerator;
import com.diepnn.shortenurl.exception.TooManyRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShortCodeServiceImplTests {

    private IdGenerator idGenerator;
    private ShortenUrlGenerator<Long, String> codeGenerator;
    private ShortCodeServiceImpl service;

    @BeforeEach
    void setUp() {
        idGenerator = mock(IdGenerator.class);
        codeGenerator = mock(ShortenUrlGenerator.class);
        service = new ShortCodeServiceImpl(idGenerator, codeGenerator);
    }

    @Test
    void generateShortCode_withProvidedId_delegatesToShortenUrlGenerator() {
        long inputId = 123L;
        when(codeGenerator.generate(inputId)).thenReturn("abc123");

        String result = service.generateShortCode(inputId);

        assertEquals("abc123", result);
        verify(codeGenerator).generate(inputId);
        verifyNoInteractions(idGenerator);
    }

    @Test
    void generateId_delegatesToIdGenerator() throws TooManyRequestException {
        when(idGenerator.generate()).thenReturn(42L);

        Long id = service.generateId();

        assertEquals(42L, id);
        verify(idGenerator).generate();
        verifyNoInteractions(codeGenerator);
    }

    @Test
    void generateShortCode_noArg_usesGeneratedIdThenEncodes() throws TooManyRequestException {
        when(idGenerator.generate()).thenReturn(999L);
        when(codeGenerator.generate(999L)).thenReturn("zzz");

        String code = service.generateShortCode();

        assertEquals("zzz", code);
        verify(idGenerator).generate();
        verify(codeGenerator).generate(999L);
    }

    @Test
    void generateShortCode_noArg_propagatesTooManyRequestException() throws TooManyRequestException {
        when(idGenerator.generate()).thenThrow(new TooManyRequestException("rate-limited"));

        TooManyRequestException ex = assertThrows(TooManyRequestException.class, () -> service.generateShortCode());

        assertTrue(ex.getMessage().contains("rate-limited"));
        verify(idGenerator).generate();
        verifyNoInteractions(codeGenerator);
    }

    @Test
    void generateId_propagatesTooManyRequestException() throws TooManyRequestException {
        when(idGenerator.generate()).thenThrow(new TooManyRequestException("rate-limited"));

        assertThrows(TooManyRequestException.class, () -> service.generateId());
        verify(idGenerator).generate();
    }

    @Test
    void generateShortCode_withProvidedId_propagatesRuntimeFromShortener() {
        long id = 7L;
        when(codeGenerator.generate(id)).thenThrow(new IllegalStateException("encode failure"));

        IllegalStateException ex =
                assertThrows(IllegalStateException.class, () -> service.generateShortCode(id));

        assertTrue(ex.getMessage().contains("encode failure"));
        verify(codeGenerator).generate(id);
        verifyNoInteractions(idGenerator);
    }
}
