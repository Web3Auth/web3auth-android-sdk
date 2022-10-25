package com.web3auth.core.types

enum class Display(private val label: String) {
    PAGE("page"), POPUP("popup"), TOUCH("touch"), WAP("wap");

    companion object {
        private val BY_LABEL: MutableMap<String, Display> = HashMap()
        fun valueOfLabel(label: String): Display? {
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
