package tn.esprit.wayfinder.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PaypalOrderRequest(
    @SerialName("amount")
    val amount: Double,
    @SerialName("currency")
    val currency: String,
    @SerialName("returnUrl")
    val returnUrl: String,
    @SerialName("cancelUrl")
    val cancelUrl: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("referenceId")
    val referenceId: String? = null,
    @SerialName("items")
    val items: List<PaypalOrderItem>? = null
)

@Serializable
data class PaypalOrderItem(
    @SerialName("name")
    val name: String,
    @SerialName("unit_amount")
    val unitAmount: Double,
    @SerialName("quantity")
    val quantity: Int,
    @SerialName("currency_code")
    val currencyCode: String? = null
)

@Serializable
data class PaypalOrderResponse(
    @SerialName("orderId")
    val orderId: String,
    @SerialName("status")
    val status: String,
    @SerialName("approvalUrl")
    val approvalUrl: String? = null,
    @SerialName("purchaseUnits")
    val purchaseUnits: JsonElement? = null
)

@Serializable
data class PaypalCaptureResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("raw")
    val raw: JsonElement? = null
)

@Serializable
data class PaypalOrderStatusResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("links")
    val links: JsonElement? = null
)


