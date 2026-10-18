package com.library.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {
    @Test
    void malformedRequestDoesNotLogParserValues(CapturedOutput output) {
        String sensitiveValue = "synthetic-password-not-for-logs";
        var exception = new HttpMessageNotReadableException(
                "Cannot parse value: " + sensitiveValue, new MockHttpInputMessage(new byte[0]));

        var response = new GlobalExceptionHandler().handleUnreadable(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(output.getAll()).contains("HttpMessageNotReadableException").doesNotContain(sensitiveValue);
    }
}
