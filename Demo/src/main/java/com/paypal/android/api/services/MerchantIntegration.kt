package com.paypal.android.api.services

enum class MerchantIntegration(val baseUrl: String) {
    LOCAL("http://10.0.2.2:8080/"),
    DEFAULT("https://sdk-sample-merchant-server.herokuapp.com/"),
    DIRECT("https://sdk-sample-merchant-server.herokuapp.com/direct/"),
    CONNECTED_PATH("https://sdk-sample-merchant-server.herokuapp.com/connected_path/"),
    MANAGED_PATH("https://sdk-sample-merchant-server.herokuapp.com/managed_path/"),
}
