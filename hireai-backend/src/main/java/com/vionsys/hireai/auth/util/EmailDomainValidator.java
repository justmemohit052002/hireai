package com.vionsys.hireai.auth.util;

import java.util.Set;
import java.util.regex.Pattern;

import com.vionsys.hireai.exception.InvalidEmailException;

public class EmailDomainValidator {

    // RFC 5322 compliant regex ensuring valid local part, @ symbol, valid domain hierarchy, and 2-15 char TLD
    private static final Pattern STRICT_EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,15}$"
    );

    // List of known disposable / temporary / burner email domains
    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            "tempmail.com", "tempmail.net", "temp-mail.org", "temp-mail.io",
            "10minutemail.com", "10minutemail.net", "10minute.email",
            "guerrillamail.com", "guerrillamail.net", "guerrillamail.org", "guerrillamailblock.com",
            "mailinator.com", "mailinator2.com", "mailinator.net",
            "trashmail.com", "trashmail.net", "trashmail.me", "trashmail.org",
            "yopmail.com", "yopmail.fr", "yopmail.net",
            "sharklasers.com", "grr.la", "guerrillamail.biz",
            "getnada.com", "abovethemap.com", "crapmail.org",
            "fakemail.net", "fakemailgenerator.com", "dispostable.com",
            "throwawaymail.com", "burnermail.io", "fakeinbox.com",
            "crazymailing.com", "mytemp.email", "tempinbox.com",
            "generator.email", "inboxkitten.com", "maildrop.cc",
            "nada.ltd", "discard.email", "mohmal.com", "emailondeck.com",
            "getairmail.com", "throwaway.email", "armyspy.com", "cuvox.de",
            "dayrep.com", "fleckens.hu", "gustr.com", "jourrapide.com",
            "rhyta.com", "superrito.com", "teleworm.us", "tinypng.email"
    );

    private EmailDomainValidator() {
        // utility class
    }

    /**
     * Validates that the email has a correct RFC-compliant format and does not originate from a disposable/burner domain.
     *
     * @param email Email address to validate
     * @throws InvalidEmailException if format is invalid or domain is disposable
     */
    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidEmailException("Email address cannot be blank.");
        }

        String cleaned = email.trim().toLowerCase();

        if (!STRICT_EMAIL_PATTERN.matcher(cleaned).matches()) {
            throw new InvalidEmailException("Invalid email format. Please provide a valid email address (e.g. name@domain.com).");
        }

        int atIndex = cleaned.lastIndexOf('@');
        if (atIndex == -1 || atIndex == cleaned.length() - 1) {
            throw new InvalidEmailException("Invalid email address format.");
        }

        String domain = cleaned.substring(atIndex + 1);

        if (DISPOSABLE_DOMAINS.contains(domain)) {
            throw new InvalidEmailException("Temporary or disposable email domains (" + domain + ") are not permitted. Please use a valid personal or corporate email address.");
        }

        // Subdomain check: e.g. foo.tempmail.com
        for (String disposable : DISPOSABLE_DOMAINS) {
            if (domain.endsWith("." + disposable)) {
                throw new InvalidEmailException("Temporary or disposable email domains are not permitted. Please use a valid personal or corporate email address.");
            }
        }
    }

    /**
     * Returns true if email format is valid and not disposable.
     */
    public static boolean isValid(String email) {
        try {
            validateEmail(email);
            return true;
        } catch (InvalidEmailException e) {
            return false;
        }
    }
}
