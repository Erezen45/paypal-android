package com.paypal.android.ui.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.paypal.android.R
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.databinding.FragmentCardBinding
import com.paypal.android.utils.SharedPreferenceUtil
import com.paypal.checkout.createorder.OrderIntent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    companion object {
        const val TAG = "CardFragment"
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    @Inject
    lateinit var dataCollectorHandler: DataCollectorHandler

    private lateinit var cardClient: CardClient
    private lateinit var binding: FragmentCardBinding
    private val orderIntent: OrderIntent
        get() = when (binding.radioGroupIntent.checkedRadioButtonId) {
            R.id.intent_authorize -> OrderIntent.AUTHORIZE
            else -> OrderIntent.CAPTURE
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CardView()
            }
        }
//
//        binding = FragmentCardBinding.inflate(inflater, container, false)
//        return binding.root
    }

    @Composable
    fun CardView() {
        Text("Hello")
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.run {
//            cardNumberInput.onValueChange = ::onCardNumberChange
//            cardExpirationInput.onValueChange = ::onCardExpirationDateChange
//
//            useTestCardButton.setOnClickListener {
//                findNavController().navigate(R.id.action_cardFragment_to_testCardFragment)
//            }
//            submitButton.setOnClickListener {
//                viewLifecycleOwner.lifecycleScope.launch {
//                    createOrder()
//                }
//            }
//        }
//
//        setFragmentResultListener(TestCardsFragment.REQUEST_KEY) { _, bundle ->
//            handleTestCardSelected(bundle)
//        }
//    }
//
//    private fun handleTestCardSelected(bundle: Bundle) {
//        val cardNumber = bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_NUMBER)
//        val securityCode = bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_SECURITY_CODE)
//
//        val expirationMonth =
//            bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_EXPIRATION_MONTH)
//        val expirationYear =
//            bundle.getString(TestCardsFragment.RESULT_EXTRA_CARD_EXPIRATION_YEAR)
//
//        binding.run {
//            cardNumberInput.setText("")
//            val formattedCardNumber =
//                CardFormatter.formatCardNumber(cardNumber ?: "")
//            cardNumberInput.setText(formattedCardNumber)
//
//            val expirationDate = "$expirationMonth/$expirationYear"
//            cardExpirationInput.setText(expirationDate)
//            cardSecurityCodeInput.setText(securityCode)
//        }
//    }
//
//    private fun onCardNumberChange(oldValue: String, newValue: String) {
//        val formattedCardNumber = CardFormatter.formatCardNumber(newValue, oldValue)
//        binding.cardNumberInput.setText(formattedCardNumber)
//        binding.cardNumberInput.setSelection(formattedCardNumber.length)
//    }
//
//    private fun onCardExpirationDateChange(oldValue: String, newValue: String) {
//        val formattedExpirationDate = DateFormatter.formatExpirationDate(newValue, oldValue)
//        binding.cardExpirationInput.setText(formattedExpirationDate)
//        binding.cardExpirationInput.setSelection(formattedExpirationDate.length)
//    }
//
//    private suspend fun createOrder() {
//        val clientId = sdkSampleServerAPI.fetchClientId()
//        val configuration = CoreConfig(clientId = clientId)
//        cardClient = CardClient(requireActivity(), configuration)
//
//        cardClient.approveOrderListener = object : ApproveOrderListener {
//            override fun onApproveOrderSuccess(result: CardResult) {
//                viewLifecycleOwner.lifecycleScope.launch {
//                    when (orderIntent) {
//                        OrderIntent.CAPTURE -> captureOrder(result)
//                        OrderIntent.AUTHORIZE -> authorizeOrder(result)
//                    }
//                }
//            }
//
//            override fun onApproveOrderFailure(error: PayPalSDKError) {
//                updateStatusText("CAPTURE fail: ${error.errorDescription}")
//            }
//
//            override fun onApproveOrderCanceled() {
//                updateStatusText("USER CANCELED")
//            }
//
//            override fun onApproveOrderThreeDSecureWillLaunch() {
//                updateStatusText("3DS launched")
//            }
//
//            override fun onApproveOrderThreeDSecureDidFinish() {
//                updateStatusText("3DS finished")
//            }
//        }
//
//        dataCollectorHandler.setLogging(true)
//        updateStatusText("Creating order...")
//
//        val orderRequest = CreateOrderRequest(
//            intent = orderIntent.name,
//            purchaseUnit = listOf(
//                com.paypal.android.api.model.PurchaseUnit(
//                    amount = com.paypal.android.api.model.Amount(
//                        currencyCode = "USD",
//                        value = "10.99"
//                    )
//                )
//            ),
//            payee = Payee(emailAddress = "anpelaez@paypal.com")
//        )
//
//        val order = sdkSampleServerAPI.createOrder(orderRequest = orderRequest)
//
//        val clientMetadataId = dataCollectorHandler.getClientMetadataId(order.id)
//        Log.i(TAG, "MetadataId: $clientMetadataId")
//
//        updateStatusText("Authorizing order...")
//
//        // build card request
//        val cardRequest = binding.run {
//            val cardNumber = cardNumberInput.text.toString().replace(" ", "")
//            val expirationDate = cardExpirationInput.text.toString()
//            val securityCode = cardSecurityCodeInput.text.toString()
//
//            val (monthString, yearString) = expirationDate.split("/")
//
//            val billingAddress = Address(
//                countryCode = "US",
//                streetAddress = "3272 Gateway Road",
//                locality = "Aloha",
//                postalCode = "97007"
//            )
//
//            val card = Card(
//                cardNumber,
//                monthString,
//                yearString,
//                securityCode,
//                billingAddress = billingAddress
//            )
//            val sca = when (radioGroup3DS.checkedRadioButtonId) {
//                R.id.sca_when_required -> SCA.SCA_WHEN_REQUIRED
//                else -> SCA.SCA_ALWAYS
//            }
//            CardRequest(order.id!!, card, APP_RETURN_URL, sca)
//        }
//
//        cardClient.approveOrder(requireActivity(), cardRequest)
//    }
//
//    private suspend fun captureOrder(cardResult: CardResult) {
//        updateStatusText("Capturing order with ID: ${cardResult.orderId}...")
//        val result = sdkSampleServerAPI.captureOrder(cardResult.orderId)
//        updateStatusTextWithCardResult(cardResult, result.status)
//    }
//
//    private suspend fun authorizeOrder(cardResult: CardResult) {
//        updateStatusText("Authorizing order with ID: ${cardResult.orderId}...")
//        val result = sdkSampleServerAPI.authorizeOrder(cardResult.orderId)
//        updateStatusTextWithCardResult(cardResult, result.status)
//    }
//
//    private fun updateStatusTextWithCardResult(result: CardResult, orderStatus: String?) {
//        val statusText = "Confirmed Order: ${result.orderId} Status: $orderStatus"
//        val deepLink = result.deepLinkUrl?.toString().orEmpty()
//        val joinedText = listOf(statusText, deepLink).joinToString("\n")
//        updateStatusText(joinedText)
//    }
//
//    private fun updateStatusText(text: String) {
//        requireActivity().runOnUiThread {
//            if (!isDetached) {
//                binding.statusText.text = text
//            }
//        }
//    }
}
