package com.web3auth.core.types


enum class Prompt(private val label: String) {
    NONE("none"),
    LOGIN("login"),
    CONSENT("consent"),
    SELECT_ACCOUNT("select_account");

    companion object {
        private val BY_LABEL: MutableMap<String, Prompt> = HashMap()
        fun valueOfLabel(label: String): Prompt? {
            return BY_LABEL[label]
        }

        init {
            for (e in values()) {
                BY_LABEL[e.label] = e
            }
        }
    }

    override fun toString(): String {
        return label
    }
}