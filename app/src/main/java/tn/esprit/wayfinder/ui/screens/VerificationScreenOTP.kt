package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import tn.esprit.wayfinder.ui.theme.WayFinderTheme

@Composable
fun VerificationScreenOTP(navController: NavController) {
    var otpValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F8FF))
            .padding(16.dp)
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.height(32.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Header", fontSize = 24.sp, fontWeight = FontWeight.Bold) // This is a placeholder from the design
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Entrez le code",
                fontSize = 16.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(48.dp))

            OtpTextField(otpText = otpValue, onOtpTextChange = { otpValue = it })

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    navController.navigate("login_screen") {
                        popUpTo(0) { inclusive = true } // Go back to login, clearing the auth flow
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Se connecter", fontSize = 16.sp)
                // Icon(painter = painterResource(id = R.drawable.ic_login_arrow), contentDescription = null)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text("En vous inscrivant, vous acceptez nos...", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun OtpTextField(
    otpText: String,
    onOtpTextChange: (String) -> Unit,
    length: Int = 4
) {
    BasicTextField(
        value = otpText,
        onValueChange = {
            if (it.length <= length) {
                onOtpTextChange(it)
            }
        },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(length) {
                    val char = otpText.getOrNull(it)
                    val isFocused = otpText.length == it
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(horizontal = 4.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color(0xFF1976D2) else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char?.toString() ?: "-", 
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun VerificationScreenOTPPreview() {
    WayFinderTheme {
        VerificationScreenOTP(rememberNavController())
    }
}
