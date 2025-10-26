package com.widedot.calendar.utils;

/**
 * Utilitaire pour le matching de réponses avec tolérance aux fautes
 * 100% compatible GWT - utilise uniquement les classes Java supportées par GWT
 */
public class AnswerMatcher {
    
    // Seuil de similarité unifié (0.0 = pas similaire, 1.0 = identique)
    // Avec le système de scoring pondéré, ce seuil est plus permissif
    private static final double SIMILARITY_THRESHOLD = 0.90;
    
    /**
     * Normalise une chaîne pour la comparaison :
     * - Supprime les accents et diacritiques (manuellement)
     * - Convertit en minuscules
     * - Supprime les espaces et caractères spéciaux
     * 
     * @param text Le texte à normaliser
     * @return Le texte normalisé
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Normalisation en un seul parcours pour optimiser les performances
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char normalized = normalizeChar(c);
            if (normalized != 0) {
                sb.append(normalized);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Normalise un caractère en supprimant les accents (compatible GWT)
     * 
     * @param c Le caractère à normaliser
     * @return Le caractère normalisé ou 0 si à supprimer
     */
    private static char normalizeChar(char c) {
        // Caractères alphanumériques normaux - conversion en minuscules
        if (Character.isLetterOrDigit(c)) {
            return Character.toLowerCase(c);
        }
        
        // Caractères d'espacement à supprimer
        if (Character.isWhitespace(c)) {
            return 0;
        }
        
        // Caractères de ponctuation courants à supprimer
        if (isPunctuationToRemove(c)) {
            return 0;
        }
        
        // Table de correspondance pour les caractères accentués les plus courants
        return getAccentMapping(c);
    }
    
    /**
     * Vérifie si un caractère de ponctuation doit être supprimé
     */
    private static boolean isPunctuationToRemove(char c) {
        return c == '-' || c == '_' || c == '.' || c == ',' || c == ';' || c == ':' || 
               c == '!' || c == '?' || c == '(' || c == ')' || c == '[' || c == ']' || 
               c == '{' || c == '}' || c == '"' || c == '\'' || c == '`';
    }
    
    /**
     * Retourne la correspondance pour les caractères accentués
     * Optimisé pour les caractères les plus courants en français/anglais
     */
    private static char getAccentMapping(char c) {
        switch (c) {
            // A avec accents
            case 'á': case 'à': case 'â': case 'ä': case 'ã': case 'å': case 'ā': case 'ă': case 'ą':
            case 'Á': case 'À': case 'Â': case 'Ä': case 'Ã': case 'Å': case 'Ā': case 'Ă': case 'Ą':
                return 'a';
                
            // E avec accents
            case 'é': case 'è': case 'ê': case 'ë': case 'ẽ': case 'ē': case 'ė': case 'ę':
            case 'É': case 'È': case 'Ê': case 'Ë': case 'Ẽ': case 'Ē': case 'Ė': case 'Ę':
                return 'e';
                
            // I avec accents
            case 'í': case 'ì': case 'î': case 'ï': case 'ĩ': case 'ī': case 'į':
            case 'Í': case 'Ì': case 'Î': case 'Ï': case 'Ĩ': case 'Ī': case 'Į':
                return 'i';
                
            // O avec accents
            case 'ó': case 'ò': case 'ô': case 'ö': case 'õ': case 'ø': case 'ō': case 'ő':
            case 'Ó': case 'Ò': case 'Ô': case 'Ö': case 'Õ': case 'Ø': case 'Ō': case 'Ő':
                return 'o';
                
            // U avec accents
            case 'ú': case 'ù': case 'û': case 'ü': case 'ũ': case 'ū': case 'ů': case 'ű':
            case 'Ú': case 'Ù': case 'Û': case 'Ü': case 'Ũ': case 'Ū': case 'Ů': case 'Ű':
                return 'u';
                
            // Y avec accents
            case 'ý': case 'ỳ': case 'ŷ': case 'ÿ':
            case 'Ý': case 'Ỳ': case 'Ŷ': case 'Ÿ':
                return 'y';
                
            // C avec accents
            case 'ç': case 'ć': case 'č': case 'ĉ': case 'ċ':
            case 'Ç': case 'Ć': case 'Č': case 'Ĉ': case 'Ċ':
                return 'c';
                
            // N avec accents
            case 'ñ': case 'ń': case 'ň': case 'ņ':
            case 'Ñ': case 'Ń': case 'Ň': case 'Ņ':
                return 'n';
                
            // S avec accents
            case 'ş': case 'ś': case 'š': case 'ŝ':
            case 'Ş': case 'Ś': case 'Š': case 'Ŝ':
                return 's';
                
            // Z avec accents
            case 'ž': case 'ź': case 'ż':
            case 'Ž': case 'Ź': case 'Ż':
                return 'z';
                
            // Autres caractères courants
            case 'ř': case 'ŕ': case 'Ř': case 'Ŕ': return 'r';
            case 'ť': case 'ţ': case 'Ť': case 'Ţ': return 't';
            case 'ď': case 'đ': case 'Ď': case 'Đ': return 'd';
            case 'ľ': case 'ĺ': case 'Ľ': case 'Ĺ': return 'l';
            case 'ģ': case 'ğ': case 'Ģ': case 'Ğ': return 'g';
            case 'ķ': case 'Ķ': return 'k';
            case 'ļ': case 'Ļ': return 'l';
            case 'ŗ': case 'Ŗ': return 'r';
            
            // Caractère non reconnu - supprimer
            default:
                return 0;
        }
    }
    
    /**
     * Vérifie si deux chaînes sont similaires en utilisant le score pondéré unifié
     * 
     * @param answer La réponse correcte
     * @param userInput La réponse de l'utilisateur
     * @return true si les chaînes sont considérées comme similaires
     */
    public static boolean isSimilar(String answer, String userInput) {
        if (answer == null || userInput == null) {
            return false;
        }
        
        // Utiliser le score pondéré unifié pour la cohérence
        double score = getSimilarityScore(answer, userInput);
        return score >= SIMILARITY_THRESHOLD;
    }
    
    /**
     * Calcule la distance de Levenshtein entre deux chaînes
     * Implémentation 100% compatible GWT
     * 
     * @param s1 Première chaîne
     * @param s2 Deuxième chaîne
     * @return La distance de Levenshtein
     */
    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        if (len1 == 0) return len2;
        if (len2 == 0) return len1;
        
        int[][] matrix = new int[len1 + 1][len2 + 1];
        
        // Initialiser la première ligne et colonne
        for (int i = 0; i <= len1; i++) {
            matrix[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            matrix[0][j] = j;
        }
        
        // Calculer la distance
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                matrix[i][j] = Math.min(
                    Math.min(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1),
                    matrix[i - 1][j - 1] + cost
                );
            }
        }
        
        return matrix[len1][len2];
    }
    
    /**
     * Calcule la similarité de Jaro-Winkler entre deux chaînes
     * Implémentation 100% compatible GWT
     * 
     * @param s1 Première chaîne
     * @param s2 Deuxième chaîne
     * @return Le score de similarité Jaro-Winkler (0.0 à 1.0)
     */
    private static double jaroWinklerSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        
        int len1 = s1.length();
        int len2 = s2.length();
        
        if (len1 == 0 || len2 == 0) return 0.0;
        
        int matchWindow = Math.max(len1, len2) / 2 - 1;
        if (matchWindow < 0) matchWindow = 0;
        
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];
        
        int matches = 0;
        int transpositions = 0;
        
        // Trouver les correspondances
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchWindow);
            int end = Math.min(i + matchWindow + 1, len2);
            
            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) continue;
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }
        
        if (matches == 0) return 0.0;
        
        // Compter les transpositions
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) continue;
            while (!s2Matches[k]) k++;
            if (s1.charAt(i) != s2.charAt(k)) transpositions++;
            k++;
        }
        
        double jaro = (matches / (double) len1 + matches / (double) len2 + 
                      (matches - transpositions / 2.0) / matches) / 3.0;
        
        // Appliquer le bonus Winkler pour les préfixes communs
        int prefixLength = 0;
        int maxPrefix = Math.min(4, Math.min(len1, len2));
        for (int i = 0; i < maxPrefix; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLength++;
            } else {
                break;
            }
        }
        
        return jaro + (0.1 * prefixLength * (1.0 - jaro));
    }
    
    
    /**
     * Calcule le score de similarité entre deux chaînes (0.0 à 1.0)
     * 
     * @param answer La réponse correcte
     * @param userInput La réponse de l'utilisateur
     * @return Le score de similarité (0.0 = pas similaire, 1.0 = identique)
     */
    public static double getSimilarityScore(String answer, String userInput) {
        if (answer == null || userInput == null) {
            return 0.0;
        }
        
        String normalizedAnswer = normalize(answer);
        String normalizedInput = normalize(userInput);
        
        // Vérification exacte
        if (normalizedAnswer.equals(normalizedInput)) {
            return 1.0;
        }
        
        // Utiliser Jaro-Winkler comme score principal (plus précis pour les noms)
        double jaroScore = jaroWinklerSimilarity(normalizedAnswer, normalizedInput);
        
        // Utiliser Levenshtein comme score secondaire
        int distance = levenshteinDistance(normalizedAnswer, normalizedInput);
        int maxLength = Math.max(normalizedAnswer.length(), normalizedInput.length());
        double levenshteinScore = maxLength > 0 ? 1.0 - (double) distance / maxLength : 1.0;
        
        // Utiliser la moyenne pondérée pour être plus strict
        // Jaro-Winkler a un poids de 0.6, Levenshtein de 0.4
        double weightedScore = (jaroScore * 0.6) + (levenshteinScore * 0.4);
        
        // Appliquer une pénalité si les longueurs sont très différentes
        int lengthDiff = Math.abs(normalizedAnswer.length() - normalizedInput.length());
        int maxLen = Math.max(normalizedAnswer.length(), normalizedInput.length());
        if (maxLen > 0) {
            double lengthPenalty = 1.0 - (double) lengthDiff / maxLen * 0.3; // Pénalité max de 30%
            weightedScore *= lengthPenalty;
        }
        
        return weightedScore;
    }
    
    /**
     * Vérifie si une réponse utilisateur correspond à une liste de réponses acceptables
     * 
     * @param userInput La réponse de l'utilisateur
     * @param acceptableAnswers Les réponses acceptables
     * @return true si une correspondance est trouvée
     */
    public static boolean matchesAny(String userInput, String[] acceptableAnswers) {
        if (userInput == null || acceptableAnswers == null) {
            return false;
        }
        
        // Log de debug pour voir l'évaluation de chaque alternative
        com.badlogic.gdx.Gdx.app.log("AnswerMatcher", "=== ÉVALUATION DES RÉPONSES ===");
        com.badlogic.gdx.Gdx.app.log("AnswerMatcher", "Input utilisateur: '" + userInput + "'");
        com.badlogic.gdx.Gdx.app.log("AnswerMatcher", "Seuil de similarité: " + GwtCompatibleFormatter.formatFloat((float)SIMILARITY_THRESHOLD, 2));
        
        boolean foundMatch = false;
        for (int i = 0; i < acceptableAnswers.length; i++) {
            String answer = acceptableAnswers[i];
            double score = getSimilarityScore(answer, userInput);
            boolean isMatch = score >= SIMILARITY_THRESHOLD;
            
            com.badlogic.gdx.Gdx.app.log("AnswerMatcher", "  [" + (i+1) + "] '" + answer + "' -> score: " + 
                GwtCompatibleFormatter.formatFloat((float)score, 3) + " " + (isMatch ? "✓ ACCEPTÉ" : "✗ REJETÉ"));
            
            if (isMatch) {
                foundMatch = true;
            }
        }
        
        com.badlogic.gdx.Gdx.app.log("AnswerMatcher", "Résultat final: " + (foundMatch ? "CORRESPONDANCE TROUVÉE" : "AUCUNE CORRESPONDANCE"));
        com.badlogic.gdx.Gdx.app.log("AnswerMatcher", "========================================");
        
        return foundMatch;
    }
    
    /**
     * Trouve la meilleure correspondance dans une liste de réponses
     * 
     * @param userInput La réponse de l'utilisateur
     * @param possibleAnswers Les réponses possibles
     * @return La réponse avec le meilleur score de similarité, ou null si aucune correspondance
     */
    public static String findBestMatch(String userInput, String[] possibleAnswers) {
        if (userInput == null || possibleAnswers == null || possibleAnswers.length == 0) {
            return null;
        }
        
        String bestMatch = null;
        double bestScore = 0.0;
        
        for (String answer : possibleAnswers) {
            double score = getSimilarityScore(answer, userInput);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = answer;
            }
        }
        
        // Retourner la meilleure correspondance seulement si elle dépasse le seuil
        return bestScore >= SIMILARITY_THRESHOLD ? bestMatch : null;
    }
    
    /**
     * Trouve la meilleure correspondance dans une liste de réponses SANS restriction de seuil
     * Utile pour le debug pour voir le score même s'il est en dessous du seuil
     * 
     * @param userInput La réponse de l'utilisateur
     * @param possibleAnswers Les réponses possibles
     * @return Un objet contenant la meilleure correspondance et son score
     */
    public static MatchResult findBestMatchWithScore(String userInput, String[] possibleAnswers) {
        if (userInput == null || possibleAnswers == null || possibleAnswers.length == 0) {
            return new MatchResult(null, 0.0);
        }
        
        String bestMatch = null;
        double bestScore = 0.0;
        
        for (String answer : possibleAnswers) {
            double score = getSimilarityScore(answer, userInput);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = answer;
            }
        }
        
        return new MatchResult(bestMatch, bestScore);
    }
    
    /**
     * Résultat d'une recherche de correspondance
     */
    public static class MatchResult {
        public final String match;
        public final double score;
        
        public MatchResult(String match, double score) {
            this.match = match;
            this.score = score;
        }
    }
}
