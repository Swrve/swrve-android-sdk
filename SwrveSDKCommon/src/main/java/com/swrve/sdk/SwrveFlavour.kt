package com.swrve.sdk

enum class SwrveFlavour {
    CORE, FIREBASE, HUAWEI;

    override fun toString(): String {
        return when (this) {
            CORE -> "core"
            FIREBASE -> "firebase"
            HUAWEI -> "huawei"
        }
    }
}
