package tn.esprit.wayfinder.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.esprit.wayfinder.models.Accommodation
import tn.esprit.wayfinder.models.SelectedUpsell
import tn.esprit.wayfinder.models.TripDetails
import tn.esprit.wayfinder.models.BookingPassenger
import tn.esprit.wayfinder.ui.components.CustomBottomNavigationBar
import tn.esprit.wayfinder.utils.StringTranslator
import tn.esprit.wayfinder.utils.QRCodeGenerator
import androidx.compose.material3.HorizontalDivider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsScreen(navController: NavController, bookingId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentEntry = navController.currentBackStackEntry
    val ticketType = currentEntry?.savedStateHandle?.get<String>("ticket_type") ?: "flight"
    val confirmationNumber = currentEntry?.savedStateHandle?.get<String>("booking_confirmation_number") ?: bookingId
    
    // Get booking data
    val accommodation = currentEntry?.savedStateHandle?.get<Accommodation>("booking_accommodation")
    val upsells = currentEntry?.savedStateHandle?.get<List<SelectedUpsell>>("booking_upsells") ?: emptyList()
    val tripDetails = currentEntry?.savedStateHandle?.get<TripDetails>("booking_trip_details")
    val passengers = currentEntry?.savedStateHandle?.get<List<BookingPassenger>>("booking_passengers")
    
    var isDownloading by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (ticketType) {
                            "hotel" -> StringTranslator.translate(context, "Réservation hôtel")
                            "activities" -> StringTranslator.translate(context, "Billets d'activités")
                            else -> StringTranslator.translate(context, "Billet d'avion")
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Download PDF button
                    IconButton(
                        onClick = {
                            if (!isDownloading) {
                                isDownloading = true
                                scope.launch {
                                    val success = downloadTicketAsPdf(
                                        context = context,
                                        ticketType = ticketType,
                                        confirmationNumber = confirmationNumber,
                                        tripDetails = tripDetails,
                                        accommodation = accommodation,
                                        passengers = passengers
                                    )
                                    isDownloading = false
                                    val message = if (success) {
                                        StringTranslator.translate(context, "Billet téléchargé avec succès!")
                                    } else {
                                        StringTranslator.translate(context, "Erreur lors du téléchargement")
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isDownloading
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = "Download PDF",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            CustomBottomNavigationBar(navController = navController)
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (ticketType) {
                "flight" -> FlightTicketCard(
                    confirmationNumber = confirmationNumber,
                    tripDetails = tripDetails,
                    passengers = passengers
                )
                "hotel" -> {
                    if (accommodation != null) {
                        HotelTicketCard(
                            confirmationNumber = confirmationNumber,
                            accommodation = accommodation,
                            passengers = passengers
                        )
                    } else {
                        Text(StringTranslator.translate(context, "Aucune réservation hôtel trouvée"))
                    }
                }
                "activities" -> {
                    if (upsells.isNotEmpty()) {
                        ActivitiesTicketsCard(
                            confirmationNumber = confirmationNumber,
                            upsells = upsells,
                            passengers = passengers
                        )
                    } else {
                        Text(StringTranslator.translate(context, "Aucun billet d'activité trouvé"))
                    }
                }
            }
            
            // Download button at bottom
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (!isDownloading) {
                        isDownloading = true
                        scope.launch {
                            val success = downloadTicketAsPdf(
                                context = context,
                                ticketType = ticketType,
                                confirmationNumber = confirmationNumber,
                                tripDetails = tripDetails,
                                accommodation = accommodation,
                                passengers = passengers
                            )
                            isDownloading = false
                            val message = if (success) {
                                StringTranslator.translate(context, "Billet téléchargé avec succès!")
                            } else {
                                StringTranslator.translate(context, "Erreur lors du téléchargement")
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (ticketType) {
                        "hotel" -> Color(0xFF4CAF50)
                        "activities" -> Color(0xFFFF9800)
                        else -> Color(0xFF1976D2)
                    }
                ),
                enabled = !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = StringTranslator.translate(context, "Télécharger le billet PDF"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Generates and saves a PDF ticket to Downloads folder
 */
suspend fun downloadTicketAsPdf(
    context: Context,
    ticketType: String,
    confirmationNumber: String,
    tripDetails: TripDetails?,
    accommodation: Accommodation?,
    passengers: List<BookingPassenger>?
): Boolean = withContext(Dispatchers.IO) {
    try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        
        val titlePaint = Paint().apply {
            textSize = 28f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#1976D2")
        }
        
        val headerPaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        
        val textPaint = Paint().apply {
            textSize = 14f
            color = android.graphics.Color.DKGRAY
        }
        
        val labelPaint = Paint().apply {
            textSize = 12f
            color = android.graphics.Color.GRAY
        }
        
        var yPos = 60f
        val leftMargin = 40f
        
        // Header
        canvas.drawText("WayFinder", leftMargin, yPos, titlePaint)
        yPos += 40f
        
        // Ticket type
        val ticketTitle = when (ticketType) {
            "hotel" -> "HOTEL RESERVATION"
            "activities" -> "ACTIVITY TICKET"
            else -> "BOARDING PASS"
        }
        canvas.drawText(ticketTitle, leftMargin, yPos, headerPaint)
        yPos += 40f
        
        // Divider line
        canvas.drawLine(leftMargin, yPos, 555f, yPos, textPaint)
        yPos += 30f
        
        // Confirmation number
        canvas.drawText("Confirmation Number", leftMargin, yPos, labelPaint)
        yPos += 20f
        canvas.drawText(confirmationNumber, leftMargin, yPos, headerPaint)
        yPos += 40f
        
        // Passenger name
        val passengerName = passengers?.firstOrNull()?.fullName ?: "Passenger"
        canvas.drawText("Passenger Name", leftMargin, yPos, labelPaint)
        yPos += 20f
        canvas.drawText(passengerName, leftMargin, yPos, textPaint)
        yPos += 40f
        
        when (ticketType) {
            "flight" -> {
                tripDetails?.let { trip ->
                    // Origin
                    trip.origin?.let {
                        canvas.drawText("From", leftMargin, yPos, labelPaint)
                        yPos += 20f
                        canvas.drawText(it, leftMargin, yPos, textPaint)
                        yPos += 30f
                    }
                    
                    // Destination
                    trip.destination?.let {
                        canvas.drawText("To", leftMargin, yPos, labelPaint)
                        yPos += 20f
                        canvas.drawText(it, leftMargin, yPos, textPaint)
                        yPos += 30f
                    }
                    
                    // Date
                    trip.departureDate?.let {
                        canvas.drawText("Departure Date", leftMargin, yPos, labelPaint)
                        yPos += 20f
                        canvas.drawText(it, leftMargin, yPos, textPaint)
                        yPos += 30f
                    }
                    
                    // Class
                    trip.travelClass?.let {
                        canvas.drawText("Class", leftMargin, yPos, labelPaint)
                        yPos += 20f
                        canvas.drawText(it, leftMargin, yPos, textPaint)
                        yPos += 30f
                    }
                }
            }
            "hotel" -> {
                accommodation?.let { hotel ->
                    // Hotel name
                    canvas.drawText("Hotel", leftMargin, yPos, labelPaint)
                    yPos += 20f
                    canvas.drawText(hotel.name, leftMargin, yPos, textPaint)
                    yPos += 30f
                    
                    // Location
                    canvas.drawText("Location", leftMargin, yPos, labelPaint)
                    yPos += 20f
                    canvas.drawText(hotel.location, leftMargin, yPos, textPaint)
                    yPos += 30f
                    
                    // Rating
                    if (hotel.rating > 0) {
                        canvas.drawText("Rating", leftMargin, yPos, labelPaint)
                        yPos += 20f
                        canvas.drawText("${hotel.rating}/5 stars", leftMargin, yPos, textPaint)
                        yPos += 30f
                    }
                }
            }
        }
        
        // QR Code
        yPos += 20f
        val qrData = when (ticketType) {
            "hotel" -> QRCodeGenerator.generateHotelQRData(
                confirmationNumber, passengerName, accommodation?.name ?: ""
            )
            else -> QRCodeGenerator.generateFlightQRData(
                confirmationNumber, passengerName, null,
                tripDetails?.departureDate, tripDetails?.origin, tripDetails?.destination
            )
        }
        
        val qrBitmap = QRCodeGenerator.generateQRCode(qrData, 200, 200)
        qrBitmap?.let {
            canvas.drawBitmap(it, leftMargin, yPos, null)
            yPos += 220f
        }
        
        canvas.drawText("Scan this QR code at check-in", leftMargin, yPos, labelPaint)
        
        // Footer
        yPos = 800f
        canvas.drawLine(leftMargin, yPos, 555f, yPos, textPaint)
        yPos += 20f
        canvas.drawText("Powered by WayFinder - Your Travel Companion", leftMargin, yPos, labelPaint)
        
        document.finishPage(page)
        
        // Save to Downloads
        val fileName = "WayFinder_${ticketType}_$confirmationNumber.pdf"
        
        val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )
            uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file)
        }
        
        outputStream?.use { stream ->
            document.writeTo(stream)
        }
        
        document.close()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
fun FlightTicketCard(
    confirmationNumber: String,
    tripDetails: TripDetails?,
    passengers: List<BookingPassenger>?
) {
    val context = LocalContext.current
    val passengerName = passengers?.firstOrNull()?.fullName ?: StringTranslator.translate(context, "Passager")
    
    // Generate QR code
    val qrData = QRCodeGenerator.generateFlightQRData(
        confirmationNumber = confirmationNumber,
        passengerName = passengerName,
        flightNumber = null,
        departureDate = tripDetails?.departureDate,
        origin = tripDetails?.origin,
        destination = tripDetails?.destination
    )
    
    val qrBitmap = remember(qrData) {
        QRCodeGenerator.generateQRCode(qrData, 400, 400)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = confirmationNumber,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TicketInfo(
                    title = StringTranslator.translate(context, "Passager"),
                    value = passengerName,
                    textColor = Color.White
                )
                tripDetails?.departureDate?.let {
                    TicketInfo(
                        title = StringTranslator.translate(context, "Date"),
                        value = it,
                        textColor = Color.White
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tripDetails?.origin?.let {
                    TicketInfo(
                        title = StringTranslator.translate(context, "Départ"),
                        value = it,
                        textColor = Color.White
                    )
                }
                tripDetails?.destination?.let {
                    TicketInfo(
                        title = StringTranslator.translate(context, "Destination"),
                        value = it,
                        textColor = Color.White
                    )
                }
            }
            
            tripDetails?.travelClass?.let {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TicketInfo(
                        title = StringTranslator.translate(context, "Classe"),
                        value = it,
                        textColor = Color.White
                    )
                    tripDetails.seats?.let { seats ->
                        TicketInfo(
                            title = StringTranslator.translate(context, "Siège"),
                            value = seats,
                            textColor = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // QR Code
            qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(200.dp)
                )
            }
            
            Text(
                text = StringTranslator.translate(context, "Scannez ce code QR à l'aéroport"),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun HotelTicketCard(
    confirmationNumber: String,
    accommodation: Accommodation,
    passengers: List<BookingPassenger>?
) {
    val context = LocalContext.current
    val guestName = passengers?.firstOrNull()?.fullName ?: StringTranslator.translate(context, "Invité")
    
    // Generate QR code
    val qrData = QRCodeGenerator.generateHotelQRData(
        confirmationNumber = confirmationNumber,
        guestName = guestName,
        hotelName = accommodation.name
    )
    
    val qrBitmap = remember(qrData) {
        QRCodeGenerator.generateQRCode(qrData, 400, 400)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = confirmationNumber,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Icon(
                imageVector = Icons.Default.Hotel,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TicketInfo(
                    title = StringTranslator.translate(context, "Invité"),
                    value = guestName,
                    textColor = Color.White
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TicketInfo(
                    title = StringTranslator.translate(context, "Hôtel"),
                    value = accommodation.name,
                    textColor = Color.White
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TicketInfo(
                    title = StringTranslator.translate(context, "Localisation"),
                    value = accommodation.location,
                    textColor = Color.White
                )
            }
            
            if (accommodation.rating > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TicketInfo(
                        title = StringTranslator.translate(context, "Note"),
                        value = String.format("%.1f ⭐", accommodation.rating),
                        textColor = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // QR Code
            qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(200.dp)
                )
            }
            
            Text(
                text = StringTranslator.translate(context, "Présentez ce code QR à la réception"),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ActivitiesTicketsCard(
    confirmationNumber: String,
    upsells: List<SelectedUpsell>,
    passengers: List<BookingPassenger>?
) {
    val context = LocalContext.current
    val attendeeName = passengers?.firstOrNull()?.fullName ?: StringTranslator.translate(context, "Participant")
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        upsells.forEachIndexed { index, upsell ->
            // Generate QR code for each activity
            val activityQrData = QRCodeGenerator.generateActivityQRData(
                confirmationNumber = "$confirmationNumber-ACT${index + 1}",
                attendeeName = attendeeName,
                activityName = "Activity ${index + 1}",
                quantity = upsell.quantity
            )
            
            val qrBitmap = remember(activityQrData) {
                QRCodeGenerator.generateQRCode(activityQrData, 300, 300)
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "$confirmationNumber-ACT${index + 1}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Icon(
                        imageVector = Icons.Default.LocalActivity,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TicketInfo(
                            title = StringTranslator.translate(context, "Participant"),
                            value = attendeeName,
                            textColor = Color.White
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TicketInfo(
                            title = StringTranslator.translate(context, "Quantité"),
                            value = "${upsell.quantity}",
                            textColor = Color.White
                        )
                        TicketInfo(
                            title = StringTranslator.translate(context, "Prix"),
                            value = String.format("%.2f %s", upsell.price * upsell.quantity, upsell.currency),
                            textColor = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // QR Code
                    qrBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(180.dp)
                        )
                    }
                    
                    Text(
                        text = StringTranslator.translate(context, "Scannez ce code QR à l'entrée"),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TicketInfo(
    title: String,
    value: String,
    textColor: Color = Color.White
) {
    Column {
        Text(
            text = title.uppercase(),
            color = textColor.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

