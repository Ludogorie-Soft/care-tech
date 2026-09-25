package com.techstore.service.filter;

import com.techstore.exception.ValidationException;
import com.techstore.service.CategoryAliasResolver;
import com.techstore.service.filter.parser.CountParser;
import com.techstore.util.SecurityHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FilterConfigService — проверка на регулярните изрази")
class FilterConfigServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final FilterConfigService service = new FilterConfigService(jdbc, mock(CategoryFilterService.class),
            mock(CategoryAliasResolver.class), mock(SecurityHelper.class), List.of(new CountParser()));

    @Test
    @DisplayName("Изразът се компилира от PostgreSQL и грешката се връща на админа без префикса ERROR:")
    void invalidPatternIsRejectedWithThePostgresMessage() {
        PSQLException cause = new PSQLException("ERROR: invalid regular expression: parentheses () not balanced",
                PSQLState.DATA_ERROR);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("(a")))
                .thenThrow(new DataIntegrityViolationException("StatementCallback", cause));

        ValidationException e = assertThrows(ValidationException.class, () -> service.validatePattern("(a"));
        assertEquals("Невалиден регулярен израз: invalid regular expression: parentheses () not balanced", e.getMessage());
    }

    @Test
    @DisplayName("Валиден израз минава")
    void validPatternPasses() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("(^|[^[:alnum:]])ddr5"))).thenReturn(false);
        assertDoesNotThrow(() -> service.validatePattern("(^|[^[:alnum:]])ddr5"));
    }

    @Test
    @DisplayName("Прекалено дълъг израз се отказва, без да стига до базата")
    void tooLongPatternIsRejected() {
        assertThrows(ValidationException.class, () -> service.validatePattern("a".repeat(501)));
    }
}
