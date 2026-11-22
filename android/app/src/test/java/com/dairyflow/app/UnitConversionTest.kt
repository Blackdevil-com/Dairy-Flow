package com.dairyflow.app

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionTest {

    @Test
    fun testCowMilkCrateConversion() {
        // 1 Crate of Cow Milk contains 24 pouches of 500ml = 12 Litres
        val crateMultiplier = 12.0
        val cratesReceived = 10.0
        val baseQuantityLitres = cratesReceived * crateMultiplier
        assertEquals(120.0, baseQuantityLitres, 0.001)
    }

    @Test
    fun testCurdTubConversion() {
        // 1 Tub of standard curd is 400g = 0.4 kg
        val packetMultiplier = 0.40
        val packetsSold = 15.0
        val baseQuantityKg = packetsSold * packetMultiplier
        assertEquals(6.0, baseQuantityKg, 0.001)
    }

    @Test
    fun testPaneerCrateConversion() {
        // 1 Crate of Paneer contains 50 blocks of 200g = 10 kg
        val crateMultiplier = 10.0
        val cratesSold = 3.5
        val baseQuantityKg = cratesSold * crateMultiplier
        assertEquals(35.0, baseQuantityKg, 0.001)
    }

    @Test
    fun testFifoPaymentAllocationCalculation() {
        var outstandingInvoice1 = 1120.0
        var outstandingInvoice2 = 500.0
        var paymentReceived = 1500.0

        // Allocate to invoice 1
        val alloc1 = minOf(paymentReceived, outstandingInvoice1)
        outstandingInvoice1 -= alloc1
        paymentReceived -= alloc1

        // Allocate to invoice 2
        val alloc2 = minOf(paymentReceived, outstandingInvoice2)
        outstandingInvoice2 -= alloc2
        paymentReceived -= alloc2

        assertEquals(0.0, outstandingInvoice1, 0.001)
        assertEquals(120.0, outstandingInvoice2, 0.001)
        assertEquals(0.0, paymentReceived, 0.001)
    }
}
