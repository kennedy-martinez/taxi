package com.taximeter.ui.taximeter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TaximeterScreen() {
    val viewModel: TaximeterViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoadingConfig -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.isConfigError -> {
                ErrorState(
                    onRetry = viewModel::onRetryConfig,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                MainContent(
                    uiState = uiState,
                    onStartStopClick = viewModel::onStartStopClick,
                    onSupplementChange = viewModel::onSupplementChange
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    uiState: TaximeterUiState,
    onStartStopClick: () -> Unit,
    onSupplementChange: (String, Int) -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomBar(
                rideStatus = uiState.rideStatus,
                totalFare = uiState.totalFare,
                onStartStopClick = onStartStopClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            TimeDistanceDisplay(
                time = uiState.elapsedTime,
                distance = uiState.traveledDistance
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "SUPPLEMENTS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            uiState.supplements.forEach { supplement ->
                SupplementStepper(
                    name = supplement.name,
                    count = supplement.count,
                    onIncrement = { onSupplementChange(supplement.id, 1) },
                    onDecrement = { onSupplementChange(supplement.id, -1) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "PRICE BREAKDOWN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            uiState.priceBreakdown.forEach { item ->
                PriceRow(concept = item.concept, price = item.price)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading configuration",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}


@Composable
private fun TimeDistanceDisplay(time: String, distance: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "TIME", style = MaterialTheme.typography.labelMedium)
            Text(
                text = time,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "DISTANCE", style = MaterialTheme.typography.labelMedium)
            Text(
                text = distance,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SupplementStepper(
    name: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Icon(Icons.Default.Remove, contentDescription = "Remove $name")
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onIncrement, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Icon(Icons.Default.Add, contentDescription = "Add $name")
            }
        }
    }
}

@Composable
private fun PriceRow(concept: String, price: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = concept, style = MaterialTheme.typography.bodyLarge)
        Text(text = price, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BottomBar(
    rideStatus: RideStatus,
    totalFare: String,
    onStartStopClick: () -> Unit
) {
    val (buttonText, buttonColor) = when (rideStatus) {
        RideStatus.IDLE -> "START" to MaterialTheme.colorScheme.primary
        RideStatus.ACTIVE -> "STOP" to Color.Red
        RideStatus.FINISHED -> "NEW RIDE" to Color.Gray
    }

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.height(if (rideStatus == RideStatus.FINISHED) 120.dp else 80.dp)
    ) {
        if (rideStatus == RideStatus.FINISHED) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "FINAL FARE", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = totalFare,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onStartStopClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text(text = buttonText, fontSize = 16.sp)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "TOTAL FARE", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = totalFare,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onStartStopClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.width(120.dp)
                ) {
                    Text(text = buttonText, fontSize = 16.sp)
                }
            }
        }
    }
}