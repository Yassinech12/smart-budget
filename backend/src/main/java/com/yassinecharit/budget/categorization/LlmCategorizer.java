package com.yassinecharit.budget.categorization;

import java.util.Optional;

/** Asks a language model for the category of a bank transaction label. */
public interface LlmCategorizer {

    Optional<Category> categorize(String label);
}
