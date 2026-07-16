package com.telcox.springmicroservices.identity.service;

/**
 * Converts externally accepted phone numbers to the local format expected by
 * downstream systems that still persist numbers without a country code.
 */
public final class PhoneNumberNormalizer {

    private static final String TURKEY_COUNTRY_CODE = "90";
    private static final int TURKEY_LOCAL_NUMBER_LENGTH = 10;

    private PhoneNumberNormalizer() {
    }

    public static String normalizeToLocalFormat(String e164Phone) {
        if (e164Phone == null) {
            return null;
        }

        String phoneWithoutPlus = e164Phone.startsWith("+")
                ? e164Phone.substring(1)
                : e164Phone;

        if (hasCountryCodeAndExpectedLength(
                phoneWithoutPlus,
                TURKEY_COUNTRY_CODE,
                TURKEY_LOCAL_NUMBER_LENGTH)) {
            return phoneWithoutPlus.substring(TURKEY_COUNTRY_CODE.length());
        }

        return e164Phone;
    }

    private static boolean hasCountryCodeAndExpectedLength(
            String phone,
            String countryCode,
            int localNumberLength) {
        return phone.startsWith(countryCode)
                && phone.length() == countryCode.length() + localNumberLength
                && phone.chars().allMatch(Character::isDigit);
    }
}
