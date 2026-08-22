package com.codex.quota.ui.feature.accountdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.ui.components.CircularQuotaGauge
import com.codex.quota.ui.components.RelativeTimeText
import com.codex.quota.ui.components.StatusBadge
import com.codex.quota.ui.theme.Amber500
import com.codex.quota.ui.theme.Red500
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: AccountDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountWithUsage by viewModel.accountState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val accountDeleted by viewModel.accountDeleted.collectAsState()
    val appLocale = LocalConfiguration.current.locales[0]

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isBannerDismissed by viewModel.isBannerDismissed.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(accountDeleted) {
        if (accountDeleted) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                androidx.compose.material3.Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = accountWithUsage?.account?.nickname ?: "Account Details",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh usage data",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val data = accountWithUsage
        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val account = data.account
            val usage = data.usage
            val status = usage?.status ?: account.authStatus
            val remainingPercent = usage?.remainingPercent
            val usedPercent = usage?.usedPercent ?: (remainingPercent?.let { (100.0 - it).coerceIn(0.0, 100.0) })
            val effectiveRenewalEpochMs = account.customRenewalDateEpochMs ?: usage?.subscriptionRenewalEpochMs

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Subscription Renewal Setup Banner (if not yet configured and not dismissed)
                if (effectiveRenewalEpochMs == null && !isBannerDismissed) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Event,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Track Subscription Renewal",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.dismissRenewalBanner()
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("You can set your renewal date anytime by tapping the ✏️ Edit button.")
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Set when your subscription renews each month to track days remaining and billing cycles.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { showDatePickerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Set Renewal Date", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    // Main Quota Metric Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StatusBadge(status = status)
                            Spacer(modifier = Modifier.height(16.dp))

                            CircularQuotaGauge(
                                remainingPercent = remainingPercent,
                                size = 160.dp,
                                strokeWidth = 14.dp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // 3-Metric Summary Box
                            val resetStr = usage?.rateLimitInfo?.resetRequestsDuration
                                ?: usage?.rateLimitInfo?.resetTokensDuration
                                ?: "Active"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "REMAINING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (remainingPercent != null) "${remainingPercent.toInt()}%" else "--%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "USED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (usedPercent != null) "${usedPercent.toInt()}%" else "--%",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "RESET IN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = resetStr,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            if (usage?.bankedResets != null) {
                                val expiry = usage.bankedResetExpiresAtEpochMs
                                    ?.takeIf { usage.bankedResets > 0 }
                                    ?.let { epochMs ->
                                        SimpleDateFormat("HH:mm 'on' dd MMM yyyy", appLocale)
                                            .apply { timeZone = TimeZone.getDefault() }
                                            .format(Date(epochMs))
                                    }
                                val expiryPrefix = if (usage.bankedResets > 1) {
                                    "Next expiry"
                                } else {
                                    "Expires"
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                DetailMetricRow(
                                    label = "Banked usage resets",
                                    value = buildString {
                                        append("${usage.bankedResets} available")
                                        if (expiry != null) append("\n$expiryPrefix $expiry")
                                    }
                                )
                            }
                        }
                    }
                }

                // Subscription & Renewal Card (shown if configured)
                if (effectiveRenewalEpochMs != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Subscription & Renewal",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    IconButton(
                                        onClick = { showDatePickerDialog = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Renewal Date",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val renewalDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                                    .format(Date(effectiveRenewalEpochMs))

                                val daysLeft = ((effectiveRenewalEpochMs - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)

                                DetailMetricRow(
                                    label = "Plan Type",
                                    value = "${account.planType.displayName} (${usage?.billingPeriod ?: "Monthly"})"
                                )

                                DetailMetricRow(
                                    label = "Renewal / Expiration Date",
                                    value = "$renewalDate (in $daysLeft days)"
                                )

                                DetailMetricRow(
                                    label = "Auto-Renewal Status",
                                    value = if (usage?.willAutoRenew == false) {
                                        "Manual renewal / Cancels at period end"
                                    } else {
                                        "Active Subscription (Will auto-renew on next cycle)"
                                    }
                                )
                            }
                        }
                    }
                }

                // Rate Limit Dimensions Card
                item {
                    val rateLimits = usage?.rateLimitInfo
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Rate Limit Dimensions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

                            DetailMetricRow(
                                label = "Requests Per Minute (RPM)",
                                value = if (rateLimits?.limitRequests != null && rateLimits.remainingRequests != null) {
                                    "${numberFormat.format(rateLimits.remainingRequests)} / ${numberFormat.format(rateLimits.limitRequests)}"
                                } else "Available / Standard"
                            )

                            DetailMetricRow(
                                label = "Tokens Per Minute (TPM)",
                                value = if (rateLimits?.limitTokens != null && rateLimits.remainingTokens != null) {
                                    "${numberFormat.format(rateLimits.remainingTokens)} / ${numberFormat.format(rateLimits.limitTokens)}"
                                } else "Available / Standard"
                            )

                            if (usage?.remainingCredits != null) {
                                DetailMetricRow(
                                    label = "Remaining Balance / Credits",
                                    value = "$${String.format(Locale.US, "%.2f", usage.remainingCredits)}"
                                )
                            }
                        }
                    }
                }

                // Account Metadata & Security Card (Omits "Added to Codex Quota")
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Account Details",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                IconButton(onClick = { showEditDialog = true }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Account")
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            DetailMetricRow(label = "Nickname", value = account.nickname)
                            if (account.email != null) {
                                DetailMetricRow(label = "Email Address", value = account.email)
                            }
                            if (account.organizationId != null) {
                                DetailMetricRow(label = "Organization ID", value = account.organizationId)
                            }
                            if (usage?.accountCreatedEpochMs != null) {
                                DetailMetricRow(
                                    label = "OpenAI Account Created",
                                    value = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(usage.accountCreatedEpochMs))
                                )
                            }
                            DetailMetricRow(
                                label = "Key Encryption & Storage",
                                value = "Hardware-backed Android Keystore (AES-256-GCM)"
                            )
                        }
                    }
                }

                // Sync Log Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Sync Status",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Successful Sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                RelativeTimeText(epochMs = account.lastSuccessfulSyncEpochMs)
                            }

                            if (usage?.errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Diagnostic: ${usage.errorMessage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (status == AuthStatus.AUTHENTICATION_REQUIRED) Red500 else Amber500
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showReauthDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Re-Authenticate Credentials")
                        }

                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red500)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove Account")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Material 3 Date Picker Dialog for Subscription Renewal
            if (showDatePickerDialog) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = effectiveRenewalEpochMs ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
                )

                DatePickerDialog(
                    onDismissRequest = { showDatePickerDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val selectedDate = datePickerState.selectedDateMillis
                                if (selectedDate != null) {
                                    viewModel.updateRenewalDate(selectedDate)
                                }
                                showDatePickerDialog = false
                            }
                        ) {
                            Text("Save Date")
                        }
                    },
                    dismissButton = {
                        Row {
                            if (effectiveRenewalEpochMs != null) {
                                TextButton(
                                    onClick = {
                                        viewModel.updateRenewalDate(null)
                                        showDatePickerDialog = false
                                    }
                                ) {
                                    Text("Clear", color = Red500)
                                }
                            }
                            TextButton(onClick = { showDatePickerDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Edit Nickname & Color Dialog
            if (showEditDialog) {
                EditAccountDialog(
                    currentNickname = account.nickname,
                    currentColorHex = account.colorHex,
                    currentRenewalEpochMs = effectiveRenewalEpochMs,
                    onOpenDatePicker = {
                        showEditDialog = false
                        showDatePickerDialog = true
                    },
                    onDismiss = { showEditDialog = false },
                    onConfirm = { name, color ->
                        viewModel.updateAccountDetails(name, color, account.customRenewalDateEpochMs)
                        showEditDialog = false
                    }
                )
            }

            // Re-authenticate Dialog
            if (showReauthDialog) {
                ReauthDialog(
                    onDismiss = { showReauthDialog = false },
                    onConfirm = { newKey ->
                        viewModel.reauthenticate(newKey)
                        showReauthDialog = false
                    }
                )
            }

            // Delete Account Confirmation Dialog
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Remove Account?") },
                    text = { Text("This will remove '${account.nickname}' and securely delete its stored API keys from this device.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.deleteAccount()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Red500)
                        ) {
                            Text("Remove", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EditAccountDialog(
    currentNickname: String,
    currentColorHex: String,
    currentRenewalEpochMs: Long?,
    onOpenDatePicker: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }
    var selectedColor by remember { mutableStateOf(currentColorHex) }

    val colorOptions = listOf(
        "#10B981", "#3B82F6", "#8B5CF6", "#EC4899",
        "#F59E0B", "#06B6D4", "#6366F1", "#84CC16"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Theme Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subscription Renewal", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = if (currentRenewalEpochMs != null) {
                                SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(currentRenewalEpochMs))
                            } else "Not set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onOpenDatePicker) {
                        Text(if (currentRenewalEpochMs != null) "Change" else "Set Date")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nickname.isNotBlank()) onConfirm(nickname.trim(), selectedColor) },
                enabled = nickname.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReauthDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var keyInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Re-Authenticate Credentials") },
        text = {
            Column {
                Text(
                    text = "Enter a fresh OpenAI API key (sk-...) or Session JWT token for this account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("API Key or Token") },
                    placeholder = { Text("sk-...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (keyInput.isNotBlank()) onConfirm(keyInput.trim()) },
                enabled = keyInput.isNotBlank()
            ) {
                Text("Update Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
