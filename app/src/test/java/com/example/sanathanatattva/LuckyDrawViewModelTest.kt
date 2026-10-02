package com.example.sanathanatattva

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.sanathanatattva.data.api.ApiService
import com.example.sanathanatattva.data.model.*
import com.example.sanathanatattva.data.prefs.DrawMark
import com.example.sanathanatattva.data.prefs.DrawSeenStore
import com.example.sanathanatattva.payments.PaymentEvent
import com.example.sanathanatattva.payments.PaymentEvents
import com.example.sanathanatattva.ui.screens.luckydraw.LuckyDrawViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class LuckyDrawViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val scheme = DrawScheme(2, "Round One", 100.0)
    private val enrolled = DrawEnrollment(1, 2, "Round One", "running", "active", "2026-10-01 00:00:00")
    private var serverStatus = LuckyDrawStatus(open_schemes = listOf(scheme))
    private var enrollmentCalls = 0
    private var claimCalls = 0
    private var verified: VerifyDrawRequest? = null
    private var requested: EnrollDrawRequest? = null
    private var verifyFails = false
    private var statusFails = false
    private var mocked = false
    private var hasDealer = true
    private var hasAddress = true
    private val seen = mutableSetOf<String>()
    private val prefs = object : DrawSeenStore {
        override suspend fun hasSeen(consumerId: Long, schemeId: Long, month: String, mark: DrawMark) =
            "$mark/$consumerId/$schemeId/$month" in seen
        override suspend fun markSeen(consumerId: Long, schemeId: Long, month: String, mark: DrawMark) {
            seen += "$mark/$consumerId/$schemeId/$month"
        }
    }
    private fun serverError(): Nothing = throw HttpException(Response.error<Any>(503, """{"error":"Temporarily unavailable"}""".toResponseBody()))
    private val api = Proxy.newProxyInstance(ApiService::class.java.classLoader, arrayOf(ApiService::class.java)) { _, method, args ->
        when (method.name) {
            "luckyDrawStatus" -> if (statusFails) serverError() else serverStatus
            "me" -> ConsumerResponse(Consumer(7, name = "Test Member", linked_dealer_id = if (hasDealer) 9 else null))
            "addresses" -> AddressesResponse(if (hasAddress) listOf(Address(3)) else emptyList())
            "luckyDrawGifts" -> WelcomeGiftsResponse(product = Product(4, "Groundnut Oil 200ml"))
            "luckyDrawWinners" -> DrawWinnersResponse()
            "enrollLuckyDraw" -> {
                enrollmentCalls++
                requested = args[0] as EnrollDrawRequest
                if (mocked) serverStatus = LuckyDrawStatus(active_enrollment = enrolled)
                EnrollDrawResponse(mocked = mocked, enrollment_id = 1, razorpay_order_id = "order_draw", amount = 10000, key_id = "key_test")
            }
            "verifyLuckyDraw" -> {
                if (verifyFails) serverError()
                verified = args[0] as VerifyDrawRequest
                serverStatus = LuckyDrawStatus(active_enrollment = enrolled)
                DrawActionResponse(success = true, order_number = "BCGIFT-123", welcome_gift = "Groundnut Oil 200ml")
            }
            "claimLuckyDrawReward" -> {
                claimCalls++
                serverStatus = serverStatus.copy(cycle = serverStatus.cycle!!.copy(reward = serverStatus.cycle!!.reward!!.copy(this_month_ordered = true)))
                DrawActionResponse(success = true, order_number = "BCFREE-123")
            }
            "leaveLuckyDraw" -> { serverStatus = LuckyDrawStatus(); DrawActionResponse(success = true, containers_returned = 1) }
            "linkDealer" -> { hasDealer = true; SuccessResponse(success = true) }
            else -> error("Unexpected API call ${method.name}")
        }
    } as ApiService

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun vm(saved: SavedStateHandle = SavedStateHandle(), consumerId: Long = 7): LuckyDrawViewModel =
        LuckyDrawViewModel(api, prefs, saved, consumerId).also { store.put("vm$consumerId", it) }

    @Test fun `paid enrollment launches correct amount ignores foreign payments and verifies once`() = runTest {
        val vm = vm()
        vm.refresh(true); runCurrent()
        vm.enroll(scheme, false); vm.enroll(scheme, false); runCurrent()
        val payment = vm.payments.first()
        assertEquals(10000, payment.amount)
        assertEquals(1, enrollmentCalls)
        assertFalse(requested!!.welcome_gift)
        PaymentEvents.events.emit(PaymentEvent.Success("pay_other", "order_cart", "sig")); runCurrent()
        assertNull(verified)
        PaymentEvents.events.emit(PaymentEvent.Success("pay_draw", "order_draw", "sig_draw")); runCurrent()
        assertEquals(VerifyDrawRequest(1, "order_draw", "pay_draw", "sig_draw"), verified)
        assertEquals("active", vm.state.value.status!!.enrollment!!.status)
        assertNull(vm.state.value.busy)
        assertTrue(vm.state.value.message!!.contains("BCGIFT-123"))
    }

    @Test fun `verification failure retains proof and retry does not create another payment`() = runTest {
        val vm = vm()
        vm.refresh(true); runCurrent(); vm.enroll(scheme, true); runCurrent()
        verifyFails = true
        PaymentEvents.events.emit(PaymentEvent.Success("pay_draw", "order_draw", "sig_draw")); runCurrent()
        assertTrue(vm.state.value.verificationPending)
        assertTrue(vm.state.value.error!!.contains("pay_draw"))
        vm.enroll(scheme, true); runCurrent()
        assertEquals(1, enrollmentCalls)
        verifyFails = false
        vm.verifyPayment(); runCurrent()
        assertFalse(vm.state.value.verificationPending)
        assertNotNull(vm.state.value.status!!.enrollment)
        assertEquals(1, enrollmentCalls)
    }

    @Test fun `recreation reconciles server enrollment before charging again`() = runTest {
        serverStatus = LuckyDrawStatus(active_enrollment = enrolled)
        val saved = SavedStateHandle(mapOf("draw_enrollment" to 1L, "draw_order" to "order_draw"))
        val vm = vm(saved)
        vm.refresh(true); runCurrent()
        assertNull(saved.get<String>("draw_order"))
        assertEquals("Your enrollment is confirmed.", vm.state.value.message)
        vm.enroll(scheme, true); runCurrent()
        assertEquals(0, enrollmentCalls)
    }

    @Test fun `cancelled payment can retry and mock enrollment skips checkout`() = runTest {
        val vm = vm()
        vm.refresh(true); runCurrent(); vm.enroll(scheme, true); runCurrent()
        PaymentEvents.events.emit(PaymentEvent.Failure("cancelled")); runCurrent()
        assertNull(vm.state.value.busy)
        assertNull(vm.state.value.status!!.enrollment)
        mocked = true
        vm.enroll(scheme, false); runCurrent()
        assertEquals(2, enrollmentCalls)
        assertNotNull(vm.state.value.status!!.enrollment)
    }

    @Test fun `requires delivery address and linked trader before enrollment`() = runTest {
        hasDealer = false
        val vm = vm()
        vm.refresh(true); runCurrent(); vm.enroll(scheme, true); runCurrent()
        assertEquals(0, enrollmentCalls)
        vm.linkDealer("TRADER"); runCurrent()
        assertNotNull(vm.state.value.consumer!!.linked_dealer_id)
        hasAddress = false
        vm.refresh(true); runCurrent(); vm.enroll(scheme, true); runCurrent()
        assertEquals(0, enrollmentCalls)
    }

    @Test fun `final month reward claims once and leaving refreshes enrollment`() = runTest {
        serverStatus = LuckyDrawStatus(active_enrollment = enrolled.copy(status = "won"),
            cycle = DrawCycle(1, 2, "Round One", month_number = 12, won = true, reward = DrawReward(months_remaining = 0)))
        val vm = vm()
        vm.refresh(); runCurrent()
        vm.claimReward(); vm.claimReward(); runCurrent()
        assertEquals(1, claimCalls)
        assertFalse(vm.state.value.status!!.cycle!!.canClaim)
        vm.claimReward(); runCurrent(); assertEquals(1, claimCalls)
        vm.leave(); runCurrent()
        assertNull(vm.state.value.status!!.enrollment)
        assertTrue(vm.state.value.message!!.contains("collect"))
    }

    @Test fun `reveal is acknowledged only after viewing and is scoped to the account`() = runTest {
        serverStatus = LuckyDrawStatus(active_enrollment = enrolled,
            cycle = DrawCycle(1, 2, "Round One", latest_draw = DrawResult("2026-10", ran_at = "2026-10-12T11:30:00Z", has_winner = false)))
        val vm = vm()
        vm.refresh(); runCurrent(); assertTrue(vm.state.value.unseenResult)
        vm.refresh(); runCurrent(); assertTrue(vm.state.value.unseenResult)
        vm.acknowledgeResult(); runCurrent(); vm.refresh(); runCurrent()
        assertFalse(vm.state.value.unseenResult)
        val otherAccount = vm(consumerId = 8)
        otherAccount.refresh(); runCurrent(); assertTrue(otherAccount.state.value.unseenResult)
    }

    @Test fun `claim marks the month for the rail and failures stay on the card`() = runTest {
        serverStatus = LuckyDrawStatus(active_enrollment = enrolled.copy(status = "won"),
            cycle = DrawCycle(1, 2, "Round One", month_key = "2026-10", month_number = 4, won = true,
                reward = DrawReward(product_name = "Groundnut Oil 1L", months_remaining = 8)))
        val vm = vm()
        vm.refresh(); runCurrent()
        vm.claimReward(); runCurrent()
        assertEquals("2026-10", vm.state.value.justClaimed)
        assertNull(vm.state.value.claimError)
        assertTrue(vm.state.value.status!!.cycle!!.reward!!.this_month_ordered)
    }

    @Test fun `dismissing a result and the entered moment are each remembered once`() = runTest {
        val cycle = DrawCycle(1, 2, "Round One", month_key = "2026-10", entered = true,
            latest_draw = DrawResult("2026-09", ran_at = "2026-10-12T11:30:00Z", winner_display_name = "Meera K."))
        serverStatus = LuckyDrawStatus(active_enrollment = enrolled, cycle = cycle)
        val vm = vm()
        vm.refresh(); runCurrent()
        assertTrue(vm.firstLookAtEntry(cycle))
        assertFalse(vm.firstLookAtEntry(cycle))
        assertFalse(vm.state.value.resultDismissed)
        vm.dismissResult(); runCurrent()
        assertTrue(vm.state.value.resultDismissed)
        vm.refresh(); runCurrent()
        assertTrue(vm.state.value.resultDismissed)
        // Dismissing the result card is not watching the reveal.
        assertTrue(vm.state.value.unseenResult)
    }

    @Test fun `failed refresh preserves last known round and exposes an error`() = runTest {
        val vm = vm()
        vm.refresh(); runCurrent()
        statusFails = true
        vm.refresh(); runCurrent()
        assertEquals(listOf(scheme), vm.state.value.status!!.open_schemes)
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }
}
