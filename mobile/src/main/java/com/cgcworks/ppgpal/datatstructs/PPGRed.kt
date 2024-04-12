package com.cgcworks.ppgpal.datatstructs

class PPGRed(private var timestamp: Long, private var red: Int){
    fun stringify(): String {
        return "${this.timestamp},${this.red}"
    }
    fun fromString(): PPGRed {
        val parts = this.stringify().split(",")
        return PPGRed(parts[0].toLong(), parts[1].toInt())
    }

    fun getRed(): Int {
        return this.red
    }

    fun getTimestamp(): Long {
        return this.timestamp
    }
}