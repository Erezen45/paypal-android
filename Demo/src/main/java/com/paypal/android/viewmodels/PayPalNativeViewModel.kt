package com.paypal.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.paypal.android.BuildConfig
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutError
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutListener
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutResult
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutClient
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutClient
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutError
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutListener
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutRequest
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutResult
import com.paypal.android.paypalnativepayments.PayPalNativePaysheetActions
import com.paypal.android.paypalnativepayments.PayPalNativeShippingAddress
import com.paypal.android.paypalnativepayments.PayPalNativeShippingListener
import com.paypal.android.paypalnativepayments.PayPalNativeShippingMethod
import com.paypal.android.ui.paypal.PayPalNativeUiState
import com.paypal.android.ui.paypal.ShippingPreferenceType
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import com.paypal.android.usecase.GetOrderIdUseCase
import com.paypal.android.usecase.UpdateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import javax.inject.Inject

@HiltViewModel
class PayPalNativeViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    @Inject
    lateinit var getClientIdUseCase: GetClientIdUseCase

    @Inject
    lateinit var getOrderIdUseCase: GetOrderIdUseCase

    @Inject
    lateinit var completeOrderUseCase: CompleteOrderUseCase

    @Inject
    lateinit var updateOrderUseCase: UpdateOrderUseCase

    private var orderId: String? = null

    private val _uiState = MutableStateFlow(PayPalNativeUiState())
    val uiState = _uiState.asStateFlow()

    var intentOption: OrderIntent
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    private val payPalListener = object : PayPalNativeCheckoutListener {
        override fun onPayPalCheckoutStart() {
            internalState.postValue(NativeCheckoutViewState.CheckoutStart)
        }

        override fun onPayPalCheckoutSuccess(result: PayPalNativeCheckoutResult) {
            result.apply {
                internalState.postValue(
                    NativeCheckoutViewState.CheckoutComplete(
                        payerId,
                        orderId
                    )
                )
            }
        }

        override fun onPayPalCheckoutFailure(error: PayPalSDKError) {
            val nxoError = error.cause as? PayPalNativeCheckoutError
            val errorState = if (nxoError != null) {
                NativeCheckoutViewState.CheckoutError(error = nxoError.errorInfo)
            } else {
                NativeCheckoutViewState.CheckoutError(message = error.errorDescription)
            }
            internalState.postValue(errorState)
        }

        override fun onPayPalCheckoutCanceled() {
            internalState.postValue(NativeCheckoutViewState.CheckoutCancelled)
        }
    }

    private val shippingListener = object : PayPalNativeShippingListener {

        override fun onPayPalNativeShippingAddressChange(
            actions: PayPalNativePaysheetActions,
            shippingAddress: PayPalNativeShippingAddress
        ) {
            if (shippingAddress.adminArea1.isNullOrBlank() || shippingAddress.adminArea1 == "NV") {
                actions.reject()
            } else {
                actions.approve()
            }
        }

        override fun onPayPalNativeShippingMethodChange(
            actions: PayPalNativePaysheetActions,
            shippingMethod: PayPalNativeShippingMethod
        ) {

            viewModelScope.launch(exceptionHandler) {
                orderId?.also {
                    try {
                        updateOrderUseCase(it, shippingMethod)
                        actions.approve()
                    } catch (e: IOException) {
                        actions.reject()
                        throw e
                    }
                }
            }
        }
    }

    private val internalState =
        MutableLiveData<NativeCheckoutViewState>(NativeCheckoutViewState.Initial)
    val state: LiveData<NativeCheckoutViewState> = internalState

    lateinit var payPalClient: PayPalNativeCheckoutClient

    private var clientId = ""

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        internalState.postValue(NativeCheckoutViewState.CheckoutError(message = e.message))
    }

    fun fetchClientId() {
        internalState.postValue(NativeCheckoutViewState.FetchingClientId)
        viewModelScope.launch(exceptionHandler) {
            clientId = getClientIdUseCase()
            initPayPalClient()
            internalState.postValue(NativeCheckoutViewState.ClientIdFetched(clientId))
        }
    }

    fun orderIdCheckout(shippingPreferenceType: ShippingPreferenceType, orderIntent: OrderIntent) {
        internalState.postValue(NativeCheckoutViewState.CheckoutInit)
        viewModelScope.launch(exceptionHandler) {
            orderId = getOrderIdUseCase(shippingPreferenceType, orderIntent)
            orderId?.also {
                payPalClient.startCheckout(PayPalNativeCheckoutRequest(it))
            }
        }
    }

    fun reset() {
        clientId = ""
        internalState.postValue(NativeCheckoutViewState.Initial)
    }

    private fun initPayPalClient() {
        payPalClient = PayPalNativeCheckoutClient(
            getApplication(),
            CoreConfig(clientId),
            "${BuildConfig.APPLICATION_ID}://paypalpay"
        )
        payPalClient.listener = payPalListener
        payPalClient.shippingListener = shippingListener
    }

    fun captureOrder(orderId: String) = viewModelScope.launch {
        // TODO: capture client metadata ID
        val order = completeOrderUseCase(orderId, OrderIntent.CAPTURE, "")
        internalState.postValue(NativeCheckoutViewState.OrderCaptured(order))
    }

    fun authorizeOrder(orderId: String) = viewModelScope.launch {
        // TODO: capture client metadata ID
        val order = completeOrderUseCase(orderId, OrderIntent.AUTHORIZE, "")
        internalState.postValue(NativeCheckoutViewState.OrderAuthorized(order))
    }
}
