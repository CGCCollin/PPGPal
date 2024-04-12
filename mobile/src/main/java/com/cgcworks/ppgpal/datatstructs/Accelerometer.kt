package com.cgcworks.ppgpal.datatstructs

import kotlin.properties.Delegates

class Accelerometer(private var timestamp: Long, private var x: Int,private var y: Int,private var z: Int) {
    init{
        this.timestamp = timestamp
        this.x = x
        this.y = y
        this.z = z
    }


    fun stringify(): String {
        return "${this.timestamp},${this.x},${this.y},${this.z}"
    }
    fun fromString(): Accelerometer {
        val parts = this.stringify().split(",")
        return Accelerometer(parts[0].toLong(), parts[1].toInt(), parts[2].toInt(), parts[3].toInt())
    }

    fun getX(): Int {
        return this.x
    }

    fun getY(): Int {
        return this.y
    }

    fun getZ(): Int {
        return this.z
    }

    fun getTimestamp(): Long {
        return this.timestamp
    }

}