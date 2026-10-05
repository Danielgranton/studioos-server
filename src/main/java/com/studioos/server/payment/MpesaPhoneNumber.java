package com.studioos.server.payment;

import com.studioos.server.shared.exceptions.StudioosException;

import java.util.regex.Pattern;

public final class MpesaPhoneNumber {

    private static final Pattern KENYAN_MOBILE = Pattern.compile("254[17]\\d{8}");

    private MpesaPhoneNumber() {
    }

    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            throw StudioosException.badRequest("Enter a valid Kenyan M-Pesa phone number");
        }

        String phone = input.trim().replaceAll("[\\s()\\-]", "");
        if (phone.startsWith("+")) {
            phone = phone.substring(1);
        }
        if (phone.matches("0[17]\\d{8}")) {
            phone = "254" + phone.substring(1);
        } else if (phone.matches("[17]\\d{8}")) {
            phone = "254" + phone;
        }

        if (!KENYAN_MOBILE.matcher(phone).matches()) {
            throw StudioosException.badRequest("Enter a valid Kenyan mobile number, such as 2547XXXXXXXX");
        }
        return phone;
    }
}
