package org.nr31.backend.service;

import java.util.Locale;
import java.util.Map;

public interface EmailSenderService {
    void sendHtmlEmail(String to, String subjectKey, String templateName, Map<String, Object> variables, Locale locale);
}
