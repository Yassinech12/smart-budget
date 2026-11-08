package com.yassinecharit.budget.categorization;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CategorizationServiceTest {

    private final CategorizationService withoutLlm = new CategorizationService(label -> Optional.empty());

    @Test
    void categorizesKnownMerchantsWithRules() {
        assertThat(withoutLlm.categorize("CB CARREFOUR MARKET PARIS 15", new BigDecimal("-64.32")))
                .isEqualTo(new CategorizationResult(Category.ALIMENTATION, CategorizationSource.RULE));
        assertThat(withoutLlm.categorize("PRLV NETFLIX.COM", new BigDecimal("-13.49")).category())
                .isEqualTo(Category.ABONNEMENTS);
        assertThat(withoutLlm.categorize("CB UBER EATS", new BigDecimal("-27.80")).category())
                .isEqualTo(Category.RESTAURANTS);
        assertThat(withoutLlm.categorize("CB UBER TRIP", new BigDecimal("-15.00")).category())
                .isEqualTo(Category.TRANSPORT);
    }

    @Test
    void ignoresAccentsAndCase() {
        assertThat(withoutLlm.categorize("Pharmacie Saint-Étienne", new BigDecimal("-9.90")).category())
                .isEqualTo(Category.SANTE);
    }

    @Test
    void positiveAmountsAreIncome() {
        assertThat(withoutLlm.categorize("VIREMENT RECU SALAIRE OCTOBRE", new BigDecimal("2450")).category())
                .isEqualTo(Category.REVENUS);
        assertThat(withoutLlm.categorize("VIR M. DUPONT", new BigDecimal("50")).category())
                .isEqualTo(Category.REVENUS);
    }

    @Test
    void usesTheLlmWhenNoRuleMatches() {
        CategorizationService service = new CategorizationService(label -> Optional.of(Category.LOISIRS));
        assertThat(service.categorize("CB LIBRAIRIE GIBERT", new BigDecimal("-24.60")))
                .isEqualTo(new CategorizationResult(Category.LOISIRS, CategorizationSource.LLM));
    }

    @Test
    void fallsBackToOtherWithoutLlm() {
        assertThat(withoutLlm.categorize("CB LIBRAIRIE GIBERT", new BigDecimal("-24.60")))
                .isEqualTo(new CategorizationResult(Category.AUTRE, CategorizationSource.DEFAULT));
    }

    @Test
    void parsesTheModelAnswer() {
        assertThat(MistralCategorizer.parse("Loisirs.")).contains(Category.LOISIRS);
        assertThat(MistralCategorizer.parse("I don't know")).isEmpty();
    }
}
