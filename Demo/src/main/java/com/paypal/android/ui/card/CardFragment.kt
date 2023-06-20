package com.paypal.android.ui.card

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.paypal.android.api.model.Amount
import com.paypal.android.api.model.CreateOrderRequest
import com.paypal.android.api.model.Payee
import com.paypal.android.api.model.PurchaseUnit
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.ui.card.validation.CardViewUiState
import com.paypal.android.utils.SharedPreferenceUtil
import com.paypal.checkout.createorder.OrderIntent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment : Fragment() {

    private val args: CardFragmentArgs by navArgs()
    private val viewModel by viewModels<CardViewModel>()

    companion object {
        const val TAG = "CardFragment"
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    @Inject
    lateinit var dataCollectorHandler: DataCollectorHandler

    private lateinit var cardClient: CardClient

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        args.prefillCard?.card?.let {
            viewModel.applyCardToCardFields(it)
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        CardView(uiState = uiState, onFormSubmit = { approveOrder() })
                    }
                }
            }
        }
    }

    private fun approveOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            createOrder(viewModel.uiState.value)
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun CardView(
        uiState: CardViewUiState,
        onFormSubmit: () -> Unit = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = "Card Details",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(2.dp))
            CardInputView(
                cardNumber = uiState.cardNumber,
                expirationDate = uiState.cardExpirationDate,
                securityCode = uiState.cardSecurityCode
            )
            Spacer(modifier = Modifier.size(24.dp))
            Text(
                text = "Approve Order Options",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(2.dp))
            OptionDropDown(
                hint = "SCA",
                value = uiState.scaOption,
                expanded = uiState.scaOptionExpanded,
                options = listOf("ALWAYS", "WHEN REQUIRED"),
                modifier = Modifier.fillMaxWidth(),
                onExpandedChange = { expanded ->
                    if (expanded) {
                        viewModel.onFocusChange(CardOption.SCA)
                    } else {
                        viewModel.clearFocus()
                    }
                },
                onValueChange = { value ->
                    viewModel.updateSCA(value)
                    viewModel.clearFocus()
                }
            )
            Spacer(modifier = Modifier.size(8.dp))
            OptionDropDown(
                hint = "INTENT",
                value = uiState.intentOption,
                expanded = uiState.intentOptionExpanded,
                options = listOf("AUTHORIZE", "CAPTURE"),
                modifier = Modifier.fillMaxWidth(),
                onExpandedChange = { expanded ->
                    if (expanded) {
                        viewModel.onFocusChange(CardOption.INTENT)
                    } else {
                        viewModel.clearFocus()
                    }
                },
                onValueChange = { value ->
                    viewModel.updateIntent(value)
                    viewModel.clearFocus()
                }
            )
            Spacer(modifier = Modifier.size(8.dp))
            OptionDropDown(
                hint = "SHOULD VAULT",
                value = uiState.shouldVaultOption,
                options = listOf("YES", "NO"),
                expanded = uiState.shouldVaultOptionExpanded,
                modifier = Modifier.fillMaxWidth(),
                onExpandedChange = { expanded ->
                    if (expanded) {
                        viewModel.onFocusChange(CardOption.SHOULD_VAULT)
                    } else {
                        viewModel.clearFocus()
                    }
                },
                onValueChange = { value ->
                    viewModel.updateShouldVault(value)
                    viewModel.clearFocus()
                }
            )
            Spacer(modifier = Modifier.size(8.dp))
            OutlinedTextField(
                value = uiState.customerId,
                label = { Text("CUSTOMER ID FOR VAULT") },
                onValueChange = { value -> viewModel.updateVaultCustomerId(value) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused) {
                            viewModel.onFocusChange(CardOption.VAULT_CUSTOMER_ID)
                        }
                    }
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = uiState.statusText,
                modifier = Modifier.weight(1.0f)
            )
            OutlinedButton(
                onClick = { onFormSubmit() },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text("CREATE & APPROVE ORDER")
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun CardViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                CardView(uiState = CardViewUiState())
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun OptionDropDown(
        hint: String,
        value: String,
        options: List<String>,
        expanded: Boolean,
        modifier: Modifier,
        onExpandedChange: (Boolean) -> Unit,
        onValueChange: (String) -> Unit
    ) {
        // Ref: https://alexzh.com/jetpack-compose-dropdownmenu/
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = modifier
        ) {
            OutlinedTextField(
                value = value,
                label = { Text(hint) },
                readOnly = true,
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = {}) {
                options.forEach { item ->
                    DropdownMenuItem(text = { Text(text = item) }, onClick = {
                        onValueChange(item)
                    })
                }
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun CardInputView(cardNumber: String, expirationDate: String, securityCode: String) {
        OutlinedTextField(
            value = cardNumber,
            label = { Text("CARD NUMBER") },
            onValueChange = { value -> viewModel.updateCardNumber(value) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            visualTransformation = CardNumberVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused) {
                        viewModel.onFocusChange(CardOption.CARD_NUMBER)
                    }
                }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = expirationDate,
                label = { Text("EXP. DATE") },
                onValueChange = { value -> viewModel.updateCardExpirationDate(value) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                visualTransformation = DateVisualTransformation(),
                modifier = Modifier
                    .weight(1.5f)
                    .onFocusChanged {
                        if (it.isFocused) {
                            viewModel.onFocusChange(CardOption.CARD_EXPIRATION_DATE)
                        }
                    }
            )
            OutlinedTextField(
                value = securityCode,
                label = { Text("SEC. CODE") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { value -> viewModel.updateCardSecurityCode(value) },
                modifier = Modifier
                    .weight(1.0f)
                    .onFocusChanged {
                        if (it.isFocused) {
                            viewModel.onFocusChange(CardOption.CARD_SECURITY_CODE)
                        }
                    }
            )
        }
    }

    private suspend fun createOrder(uiState: CardViewUiState) {
        val orderIntent = when (uiState.intentOption) {
            "AUTHORIZE" -> OrderIntent.AUTHORIZE
            else -> OrderIntent.CAPTURE
        }

        val clientId = sdkSampleServerAPI.fetchClientId()
        val configuration = CoreConfig(clientId = clientId)
        cardClient = CardClient(requireActivity(), configuration)

        cardClient.approveOrderListener = object : ApproveOrderListener {
            override fun onApproveOrderSuccess(result: CardResult) {
                viewLifecycleOwner.lifecycleScope.launch {
                    when (orderIntent) {
                        OrderIntent.CAPTURE -> captureOrder(result)
                        OrderIntent.AUTHORIZE -> authorizeOrder(result)
                    }
                }
            }

            override fun onApproveOrderFailure(error: PayPalSDKError) {
                viewModel.updateStatusText("CAPTURE fail: ${error.errorDescription}")
            }

            override fun onApproveOrderCanceled() {
                viewModel.updateStatusText("USER CANCELED")
            }

            override fun onApproveOrderThreeDSecureWillLaunch() {
                viewModel.updateStatusText("3DS launched")
            }

            override fun onApproveOrderThreeDSecureDidFinish() {
                viewModel.updateStatusText("3DS finished")
            }
        }

        dataCollectorHandler.setLogging(true)
        viewModel.updateStatusText("Creating order...")

        val orderRequest = CreateOrderRequest(
            intent = orderIntent.name,
            purchaseUnit = listOf(
                PurchaseUnit(
                    amount = Amount(
                        currencyCode = "USD",
                        value = "10.99"
                    )
                )
            ),
            payee = Payee(emailAddress = "anpelaez@paypal.com")
        )

        val order = sdkSampleServerAPI.createOrder(orderRequest = orderRequest)

        val clientMetadataId = dataCollectorHandler.getClientMetadataId(order.id)
        Log.i(TAG, "MetadataId: $clientMetadataId")

        viewModel.updateStatusText("Authorizing order...")

        // build card request
        val card = parseCard(uiState)
        val sca = when (uiState.scaOption) {
            "ALWAYS" -> SCA.SCA_ALWAYS
            else -> SCA.SCA_WHEN_REQUIRED
        }

        val cardRequest = CardRequest(order.id!!, card, APP_RETURN_URL, sca)
        cardClient.approveOrder(requireActivity(), cardRequest)
    }

    private fun parseCard(uiState: CardViewUiState): Card {
        var expirationMonth = ""
        var expirationYear = ""

        val rawExpirationDate = uiState.cardExpirationDate
        if (rawExpirationDate.length >= 5) {
            // at least two digits for month and four for year
            expirationYear = rawExpirationDate.takeLast(4)
            expirationMonth = rawExpirationDate.substring(0, rawExpirationDate.length - 4)
        } else {
            // TODO: handle invalid date
        }
        return Card(
            number = uiState.cardNumber,
            expirationMonth = expirationMonth,
            expirationYear = expirationYear,
            securityCode = uiState.cardSecurityCode
        )
    }

    private suspend fun captureOrder(cardResult: CardResult) {
        viewModel.updateStatusText("Capturing order with ID: ${cardResult.orderId}...")
        val result = sdkSampleServerAPI.captureOrder(cardResult.orderId)
        updateStatusTextWithCardResult(cardResult, result.status)
    }

    private suspend fun authorizeOrder(cardResult: CardResult) {
        viewModel.updateStatusText("Authorizing order with ID: ${cardResult.orderId}...")
        val result = sdkSampleServerAPI.authorizeOrder(cardResult.orderId)
        updateStatusTextWithCardResult(cardResult, result.status)
    }

    private fun updateStatusTextWithCardResult(result: CardResult, orderStatus: String?) {
        val statusText = "Confirmed Order: ${result.orderId} Status: $orderStatus"
        val deepLink = result.deepLinkUrl?.toString().orEmpty()
        val joinedText = listOf(statusText, deepLink).joinToString("\n")
        viewModel.updateStatusText(joinedText)
    }
}
