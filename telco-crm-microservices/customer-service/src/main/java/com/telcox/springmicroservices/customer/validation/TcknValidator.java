package com.telcox.springmicroservices.customer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class TcknValidator implements ConstraintValidator<ValidTckn, String> {

    @Override
    public boolean isValid(String tckn, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(tckn)) {
            return false;
        }
        
        if (tckn.length() != 11 || !tckn.matches("\\d+")) {
            return false;
        }
        
        if (tckn.startsWith("0")) {
            return false;
        }

        int[] digits = new int[11];
        for (int i = 0; i < 11; i++) {
            digits[i] = tckn.charAt(i) - '0';
        }

        int sumOdd = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
        int sumEven = digits[1] + digits[3] + digits[5] + digits[7];

        int digit10 = ((sumOdd * 7) - sumEven) % 10;
        if (digit10 < 0) {
            digit10 += 10;
        }
        
        if (digit10 != digits[9]) {
            return false;
        }

        int sum10 = 0;
        for (int i = 0; i < 10; i++) {
            sum10 += digits[i];
        }

        return (sum10 % 10) == digits[10];
    }
}
