package tn.esprit.wayfinder.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Utility for generating QR codes from booking data
 */
object QRCodeGenerator {
    
    /**
     * Generate a QR code bitmap from a string
     * @param text The text to encode in the QR code
     * @param width The width of the QR code in pixels
     * @param height The height of the QR code in pixels
     * @return Bitmap of the QR code
     */
    fun generateQRCode(text: String, width: Int = 512, height: Int = 512): Bitmap? {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("QRCodeGenerator", "Error generating QR code", e)
            null
        }
    }
    
    /**
     * Generate QR code data string for flight ticket
     */
    fun generateFlightQRData(
        confirmationNumber: String,
        passengerName: String,
        flightNumber: String? = null,
        departureDate: String? = null,
        origin: String? = null,
        destination: String? = null
    ): String {
        return buildString {
            append("FLIGHT|")
            append("CN:$confirmationNumber|")
            append("PN:$passengerName")
            flightNumber?.let { append("|FN:$it") }
            departureDate?.let { append("|DD:$it") }
            origin?.let { append("|OR:$it") }
            destination?.let { append("|DT:$it") }
        }
    }
    
    /**
     * Generate QR code data string for hotel reservation
     */
    fun generateHotelQRData(
        confirmationNumber: String,
        guestName: String,
        hotelName: String,
        checkIn: String? = null,
        checkOut: String? = null
    ): String {
        return buildString {
            append("HOTEL|")
            append("CN:$confirmationNumber|")
            append("GN:$guestName|")
            append("HN:$hotelName")
            checkIn?.let { append("|CI:$it") }
            checkOut?.let { append("|CO:$it") }
        }
    }
    
    /**
     * Generate QR code data string for activity ticket
     */
    fun generateActivityQRData(
        confirmationNumber: String,
        attendeeName: String,
        activityName: String,
        activityDate: String? = null,
        quantity: Int = 1
    ): String {
        return buildString {
            append("ACTIVITY|")
            append("CN:$confirmationNumber|")
            append("AN:$attendeeName|")
            append("AC:$activityName|")
            append("QTY:$quantity")
            activityDate?.let { append("|AD:$it") }
        }
    }
}

