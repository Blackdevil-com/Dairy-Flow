package com.dairyflow.app

import com.dairyflow.app.data.remote.dto.CustomerCreateRequest
import com.dairyflow.app.data.remote.dto.CustomerDto
import org.junit.Assert.*
import org.junit.Test

class CustomerManagementTest {

    @Test
    fun testCustomerDtoDefaultValues() {
        val customer = CustomerDto(
            id = "CUST-TEST-001",
            name = "Sri Lakshmi Stores",
            phone = "+91 9845012345"
        )
        assertEquals("RETAIL_SHOP", customer.customer_type)
        assertEquals(0.0, customer.credit_limit, 0.001)
        assertFalse(customer.credit_enabled)
        assertEquals(0.0, customer.current_balance, 0.001)
        assertTrue(customer.is_active)
        assertNull(customer.trade_name)
        assertNull(customer.contact_person)
    }

    @Test
    fun testCustomerCreateRequestModel() {
        val req = CustomerCreateRequest(
            name = "Mauli Supermarket",
            trade_name = "Mauli Mart",
            customer_type = "RETAIL_SHOP",
            contact_person = "Mahesh Patil",
            phone = "+91 9822110022",
            area = "Kothrud",
            city = "Pune",
            credit_enabled = true,
            credit_limit = 35000.0,
            payment_terms_days = 7
        )
        assertEquals("Mauli Supermarket", req.name)
        assertEquals("Mauli Mart", req.trade_name)
        assertEquals("RETAIL_SHOP", req.customer_type)
        assertEquals("+91 9822110022", req.phone)
        assertTrue(req.credit_enabled)
        assertEquals(35000.0, req.credit_limit, 0.001)
        assertEquals(7, req.payment_terms_days)
    }

    @Test
    fun testCreditLimitDisabledDefaultsToZero() {
        val creditEnabled = false
        val enteredCreditLimit = 50000.0
        val effectiveCreditLimit = if (creditEnabled) enteredCreditLimit else 0.0

        assertEquals(0.0, effectiveCreditLimit, 0.001)
    }

    @Test
    fun testDeliveryAddressFallbackLogic() {
        val billingAddress = "Shop 12, Station Road, Pune 411001"
        var deliveryAddress: String? = null

        val effectiveDelivery = deliveryAddress ?: billingAddress
        assertEquals("Shop 12, Station Road, Pune 411001", effectiveDelivery)

        deliveryAddress = "Back Entrance Warehouse Depot, Pune 411001"
        val explicitDelivery = deliveryAddress ?: billingAddress
        assertEquals("Back Entrance Warehouse Depot, Pune 411001", explicitDelivery)
    }

    @Test
    fun testCustomerSearchMatching() {
        val customerList = listOf(
            CustomerDto(id = "1", name = "Sri Lakshmi Stores", trade_name = "Lakshmi Bazaar", phone = "+91 9845012345", area = "Shivajinagar"),
            CustomerDto(id = "2", name = "Mauli Supermarket", trade_name = "Mauli Mart", phone = "+91 9822110022", area = "Kothrud"),
            CustomerDto(id = "3", name = "Ganesh Sweets & Milk", trade_name = null, phone = "+91 9822110033", contact_person = "Ganesh Rao", area = "Camp")
        )

        // Query by trade name
        val q1 = "bazaar"
        val res1 = customerList.filter {
            it.name.contains(q1, ignoreCase = true) || (it.trade_name?.contains(q1, ignoreCase = true) == true)
        }
        assertEquals(1, res1.size)
        assertEquals("1", res1.first().id)

        // Query by contact person
        val q2 = "ganesh"
        val res2 = customerList.filter {
            it.name.contains(q2, ignoreCase = true) || (it.contact_person?.contains(q2, ignoreCase = true) == true)
        }
        assertEquals(1, res2.size)
        assertEquals("3", res2.first().id)

        // Query by area
        val q3 = "kothrud"
        val res3 = customerList.filter {
            it.area?.contains(q3, ignoreCase = true) == true
        }
        assertEquals(1, res3.size)
        assertEquals("2", res3.first().id)
    }
}
