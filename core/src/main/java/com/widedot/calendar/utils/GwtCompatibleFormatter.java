package com.widedot.calendar.utils;

/**
 * Classe utilitaire pour le formatage compatible avec GWT
 * Remplace String.format() qui n'est pas supporté par GWT
 */
public class GwtCompatibleFormatter {
    
    /**
     * Formate un float avec un nombre spécifié de décimales
     * @param value La valeur à formater
     * @param decimals Le nombre de décimales
     * @return La chaîne formatée
     */
    public static String formatFloat(float value, int decimals) {
        if (decimals == 1) {
            return String.valueOf(Math.round(value * 10) / 10.0f);
        } else if (decimals == 2) {
            return String.valueOf(Math.round(value * 100) / 100.0f);
        } else if (decimals == 3) {
            return String.valueOf(Math.round(value * 1000) / 1000.0f);
        } else {
            return String.valueOf(value);
        }
    }
    
    /**
     * Formate plusieurs floats avec un nombre spécifié de décimales
     * @param format Le format (non utilisé, gardé pour compatibilité)
     * @param values Les valeurs à formater
     * @return La chaîne formatée
     */
    public static String format(String format, float... values) {
        if (values.length == 1) {
            return formatFloat(values[0], 1);
        } else if (values.length == 4) {
            return formatFloat(values[0], 1) + ", " + 
                   formatFloat(values[1], 1) + ", " + 
                   formatFloat(values[2], 1) + ", " + 
                   formatFloat(values[3], 1);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatFloat(values[i], 1));
            }
            return sb.toString();
        }
    }
}
