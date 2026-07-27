package com.example.sharpoclockserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SharpOclockServerApplication

fun main(args: Array<String>) {
	runApplication<SharpOclockServerApplication>(*args)
}
