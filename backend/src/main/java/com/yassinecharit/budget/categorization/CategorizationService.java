package com.yassinecharit.budget.categorization;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hybrid categorization:
 * 1. fast and free keyword rules on the transaction label,
 * 2. a language model for the labels the rules do not recognise,
 * 3. "AUTRE" by default.
 */
@Service
public class CategorizationService {

    private static final Map<Category, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put(Category.REVENUS, List.of("salaire", "virement recu", "remboursement", "caf", "prime"));
        RULES.put(Category.LOGEMENT, List.of("loyer", "edf", "engie", "eau ", "assurance habitation", "foncia"));
        RULES.put(Category.ABONNEMENTS, List.of("netflix", "spotify", "deezer", "free mobile", "orange", "sfr",
                "bouygues", "amazon prime", "disney", "canal"));
        RULES.put(Category.ALIMENTATION, List.of("carrefour", "auchan", "leclerc", "lidl", "monoprix", "franprix",
                "intermarche", "casino", "picard", "boulangerie"));
        RULES.put(Category.RESTAURANTS, List.of("restaurant", "mcdonald", "burger king", "kfc", "uber eats",
                "deliveroo", "starbucks", "brasserie", "sushi"));
        RULES.put(Category.TRANSPORT, List.of("sncf", "ratp", "navigo", "uber", "bolt", "total", "esso", "shell",
                "peage", "blablacar"));
        RULES.put(Category.SANTE, List.of("pharmacie", "docteur", "medecin", "dentiste", "mutuelle", "ameli"));
        RULES.put(Category.SHOPPING, List.of("amazon", "fnac", "zara", "decathlon", "ikea", "h&m", "darty", "cdiscount"));
        RULES.put(Category.LOISIRS, List.of("cinema", "ugc", "pathe", "concert", "steam", "playstation", "salle de sport",
                "basic fit"));
    }

    private final LlmCategorizer llmCategorizer;

    public CategorizationService(LlmCategorizer llmCategorizer) {
        this.llmCategorizer = llmCategorizer;
    }

    public CategorizationResult categorize(String label, BigDecimal amount) {
        String normalized = normalize(label);
        for (Map.Entry<Category, List<String>> rule : RULES.entrySet()) {
            if (rule.getKey() == Category.REVENUS && amount.signum() < 0) {
                continue; // an expense can not be an income
            }
            for (String keyword : rule.getValue()) {
                if (normalized.contains(keyword)) {
                    return new CategorizationResult(rule.getKey(), CategorizationSource.RULE);
                }
            }
        }
        if (amount.signum() > 0) {
            return new CategorizationResult(Category.REVENUS, CategorizationSource.RULE);
        }
        return llmCategorizer.categorize(label)
                .map(c -> new CategorizationResult(c, CategorizationSource.LLM))
                .orElse(new CategorizationResult(Category.AUTRE, CategorizationSource.DEFAULT));
    }

    /** Lower case and without accents, so "Écoles" matches "ecoles". */
    static String normalize(String label) {
        String noAccents = Normalizer.normalize(label, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return " " + noAccents.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ") + " ";
    }
}
