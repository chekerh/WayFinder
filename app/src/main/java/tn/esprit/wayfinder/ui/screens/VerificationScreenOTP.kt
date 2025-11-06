package tn.esprit.wayfinder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

@Composable
fun VerificationScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val code = remember { List(4) { mutableStateOf("") } }
    val focusRequesters = remember { List(4) { FocusRequester() } }
    val isError = remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEAF2FF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Vous devez saisir le code de vérification qui a été envoyé par e-mail ou SMS",
            style = MaterialTheme.typography.headlineSmall, // Corrected: M3 typography
            modifier = Modifier.padding(bottom = 20.dp),
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        // Code Fields (4 fields)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            code.forEachIndexed { index, state ->
                OutlinedTextField(
                    value = state.value,
                    onValueChange = {
                        isError.value = false
                        if (it.length <= 1) {
                            state.value = it
                            if (it.isNotEmpty()) {
                                if (index < code.size - 1) {
                                    focusRequesters[index + 1].requestFocus()
                                } else {
                                    focusManager.clearFocus()
                                }
                            }
                        }
                    },
                    label = { Text("•") },
                    modifier = Modifier
                        .width(60.dp)
                        .focusRequester(focusRequesters[index])
                        .onKeyEvent {
                            if (it.key == Key.Backspace && state.value.isEmpty()) {
                                if (index > 0) {
                                    focusRequesters[index - 1].requestFocus()
                                }
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (index == code.size - 1) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                        onDone = { focusManager.clearFocus() }
                    ),
                    isError = isError.value
                )
            }
        }

        // Verify Button
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val codeEntered = code.joinToString("") { it.value }
                if (codeEntered.length == 4) {
                    if (codeEntered == "1234") { // Simulate verification
                        navController.navigate("NextScreen") // TODO: Replace with actual route
                    } else {
                        isError.value = true
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3682E3)) // Corrected: M3 parameter
        ) {
            Text(text = "Verify", color = Color.White)
        }

        // Resend code link
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "t'as pas recue un code de verification? ",
                color = Color.Gray
            )
            Text(
                text = "renvoyee le code",
                color = Color(0xFFFC4C52),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    // TODO: Handle resend code action
                }
            )
        }

        // Error message (for incorrect code)
        if (isError.value) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "code incorrecte!",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge, // Corrected: M3 typography
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red)
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
    
    LaunchedEffect(Unit) {
        delay(300)
        focusRequesters[0].requestFocus()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVerificationScreen() {
    VerificationScreen(navController = rememberNavController())
}
