package com.taximeter.ui.taximeter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taximeter.R
import com.taximeter.ui.theme.TaximeterTheme
import java.util.concurrent.TimeUnit

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
            if (uiState.rideStatus == RideStatus.FINISHED) {
                FinishedBottomBar(
                    totalFare = uiState.totalFare,
                    onStartStopClick = onStartStopClick
                )
            } else {
                ActiveBottomBar(
                    rideStatus = uiState.rideStatus,
                    totalFare = uiState.totalFare,
                    onStartStopClick = onStartStopClick
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            TimeDistanceDisplay(
                timeSeconds = uiState.elapsedTimeSeconds,
                distanceKm = uiState.traveledDistanceKm,
                isIdle = uiState.rideStatus == RideStatus.IDLE
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(id = R.string.ui_supplements),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            uiState.supplements.forEach { supplement ->
                SupplementStepper(
                    name = supplement.id.toSupplementName(),
                    count = supplement.count,
                    onIncrement = { onSupplementChange(supplement.id, 1) },
                    onDecrement = { onSupplementChange(supplement.id, -1) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(id = R.string.ui_price_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            uiState.priceBreakdown.forEach { item ->
                PriceRow(
                    concept = item.concept.toBreakdownName(),
                    price = item.price
                )
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
            text = stringResource(id = R.string.error_loading_config),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(id = R.string.button_retry))
        }
    }
}


@Composable
private fun TimeDistanceDisplay(timeSeconds: Long, distanceKm: Double, isIdle: Boolean) {
    val timeText = if (isIdle && timeSeconds == 0L) {
        stringResource(id = R.string.format_time_idle)
    } else {
        String.format(
            stringResource(id = R.string.format_time_active),
            TimeUnit.SECONDS.toHours(timeSeconds),
            TimeUnit.SECONDS.toMinutes(timeSeconds) % 60,
            timeSeconds % 60
        )
    }

    val distanceText = if (isIdle && distanceKm == 0.0) {
        stringResource(id = R.string.format_distance_idle)
    } else {
        String.format(stringResource(id = R.string.format_distance_active), distanceKm)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.ui_time), style = MaterialTheme.typography.labelMedium)
            Text(
                text = timeText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.ui_distance), style = MaterialTheme.typography.labelMedium)
            Text(
                text = distanceText,
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
                Icon(Icons.Default.Remove, contentDescription = stringResource(id = R.string.content_desc_remove_supplement, name))
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = onIncrement, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.content_desc_add_supplement, name))
            }
        }
    }
}

@Composable
private fun PriceRow(concept: String, price: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = concept, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(id = R.string.format_price_eur, price),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ActiveBottomBar(
    rideStatus: RideStatus,
    totalFare: Double,
    onStartStopClick: () -> Unit
) {
    val (buttonTextRes, buttonColor) = when (rideStatus) {
        RideStatus.IDLE -> R.string.button_start to MaterialTheme.colorScheme.primary
        RideStatus.ACTIVE -> R.string.button_stop to Color.Red
        else -> R.string.button_start to MaterialTheme.colorScheme.primary
    }

    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.ui_total_fare),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = stringResource(id = R.string.format_price_eur, totalFare),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onStartStopClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = stringResource(id = buttonTextRes),
                    fontSize = 16.sp
                )
            }
        }
    }
}


@Composable
private fun FinishedBottomBar(
    totalFare: Double,
    onStartStopClick: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = stringResource(id = R.string.ui_final_fare), style = MaterialTheme.typography.labelMedium)
                Text(
                    text = stringResource(id = R.string.format_price_eur, totalFare),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onStartStopClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text(text = stringResource(id = R.string.button_new_ride), fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun String.toSupplementName(): String {
    return when (this) {
        SupplementIds.LUGGAGE -> stringResource(id = R.string.supplement_name_luggage)
        else -> this
    }
}

@Composable
private fun PriceBreakdownConcept.toBreakdownName(): String {
    return when (this) {
        PriceBreakdownConcept.DISTANCE -> stringResource(id = R.string.breakdown_concept_distance)
        PriceBreakdownConcept.TIME -> stringResource(id = R.string.breakdown_concept_time)
        PriceBreakdownConcept.SUPPLEMENTS -> stringResource(id = R.string.breakdown_concept_supplements)
    }
}


@Preview(showBackground = true, name = "Main Screen (Idle)")
@Composable
fun MainContentIdlePreview() {
    TaximeterTheme {
        MainContent(
            uiState = TaximeterUiState(
                supplements = listOf(
                    SupplementUiModel(SupplementIds.LUGGAGE, 0)
                ),
                totalFare = 0.0,
                rideStatus = RideStatus.IDLE,
                isLoadingConfig = false,
                isConfigError = false
            ),
            onStartStopClick = {},
            onSupplementChange = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Main Screen (Finished)")
@Composable
fun MainContentFinishedPreview() {
    TaximeterTheme {
        MainContent(
            uiState = TaximeterUiState(
                elapsedTimeSeconds = 1800,
                traveledDistanceKm = 11.0,
                supplements = listOf(SupplementUiModel(SupplementIds.LUGGAGE, 1)),
                priceBreakdown = listOf(
                    PriceBreakdownItem(PriceBreakdownConcept.DISTANCE, 2.20),
                    PriceBreakdownItem(PriceBreakdownConcept.TIME, 18.00),
                    PriceBreakdownItem(PriceBreakdownConcept.SUPPLEMENTS, 5.00)
                ),
                totalFare = 25.20,
                rideStatus = RideStatus.FINISHED,
                isLoadingConfig = false,
                isConfigError = false
            ),
            onStartStopClick = {},
            onSupplementChange = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun ErrorStatePreview() {
    TaximeterTheme {
        ErrorState(onRetry = {})
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun LoadingStatePreview() {
    TaximeterTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}