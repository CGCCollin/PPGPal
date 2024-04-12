package com.cgcworks.ppgpal.backend

class PPGRed(var timestamp: Long, var red: Int){
    fun stringify(): String {
        return "${this.timestamp},${this.red}"
    }
    fun fromString(): PPGRed {
        val parts = this.stringify().split(",")
        return PPGRed(parts[0].toLong(), parts[1].toInt())
    }

    @JvmName("getRed1")
    fun getRed(): Int {
        return this.red
    }

    @JvmName("getTimestamp1")
    fun getTimestamp(): Long {
        return this.timestamp
    }
}