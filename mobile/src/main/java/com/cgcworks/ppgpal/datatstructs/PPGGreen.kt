package com.cgcworks.ppgpal.datatstructs

class PPGGreen (private var timestamp: Long, private var ppg: Int) {
    init {
        this.timestamp = timestamp
        this.ppg = ppg
    }
    fun stringify(): String {
        return "${this.timestamp},${this.ppg}"
    }
    fun fromString(): PPGGreen {
        val parts = this.stringify().split(",")
        return PPGGreen(parts[0].toLong(), parts[1].toInt())
    }

    fun getPPG(): Int {
        return this.ppg
    }

    fun getTimestamp(): Long {
        return this.timestamp
    }

}