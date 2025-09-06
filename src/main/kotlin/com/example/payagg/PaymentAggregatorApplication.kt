package com.example.payagg

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PaymentAggregatorApplication

fun main(args: Array<String>) {
    runApplication<PaymentAggregatorApplication>(*args)
}
