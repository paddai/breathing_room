package com.breathingroom.backend.logic;

/** Turns a short technical AI failure ("HTTP 401", "ResourceAccessException") into a sentence a user can act on. Pure, so it is easy to test. */
public final class AiErrorMessages {

    private AiErrorMessages() {}

    public static String friendly(String code, String provider) {
        String c = code == null ? "" : code.trim();
        if (c.startsWith("HTTP ")) {
            return switch (c.substring(5)) {
                case "400" -> "The request was not accepted. Check the model name in your settings.";
                case "401" -> "The API key was rejected. Check that it is copied correctly and still active.";
                case "402" -> "The " + provider + " account has no credit left. Add credit or a payment method.";
                case "403" -> "This key is not allowed to use that model or feature. Check the key's permissions.";
                case "404" -> "The model was not found. Check the model name in your settings.";
                case "408", "504" -> provider + " took too long to answer. Try again in a moment.";
                case "413" -> "The request was too large.";
                case "429" -> "Too many requests, or the " + provider + " account is out of credit. Wait a minute, then check your plan and credit.";
                case "500", "502", "503", "529" -> provider + " is having problems or is overloaded right now. Try again in a few minutes.";
                default -> provider + " returned an error (" + c + "). Try again later.";
            };
        }
        if (c.contains("Timeout") || c.contains("ResourceAccess") || c.contains("Connect") || c.contains("UnknownHost")) {
            return "The app could not reach " + provider + ". Check your internet connection, then try again.";
        }
        return c.isEmpty() ? provider + " did not send an answer. Try again." : provider + " did not answer. Try again later.";
    }
}
