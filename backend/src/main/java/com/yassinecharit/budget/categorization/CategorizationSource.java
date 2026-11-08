package com.yassinecharit.budget.categorization;

/** How the category of a transaction was decided. */
public enum CategorizationSource {
    RULE,
    LLM,
    MANUAL,
    DEFAULT
}
