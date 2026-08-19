package com.codex.quota.ui.feature.addaccount

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.quota.domain.model.PlanType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    viewModel: AddAccountViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val accountAdded by viewModel.accountAdded.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = API Key, 1 = Demo Profiles

    // Form fields
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf(PlanType.PLUS) }
    var orgId by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#10B981") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var planDropdownExpanded by remember { mutableStateOf(false) }

    val colorOptions = listOf(
        "#10B981", "#38BDF8", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#64748B"
    )

    LaunchedEffect(accountAdded) {
        if (accountAdded != null) {
            onNavigateBack()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add Codex Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("OpenAI API Key") },
                    icon = { Icon(Icons.Default.Key, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Demo Simulation") },
                    icon = { Icon(Icons.Default.Science, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Real API Key Flow
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Credentials are encrypted on-device via Android Keystore hardware security. Passwords are never collected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("OpenAI API Key (sk-...)") },
                        placeholder = { Text("sk-proj-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle key visibility"
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Account Nickname") },
                        placeholder = { Text("e.g. Work Codex, Personal Plus") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = planDropdownExpanded,
                        onExpandedChange = { planDropdownExpanded = !planDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPlan.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Plan / Tier") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = planDropdownExpanded,
                            onDismissRequest = { planDropdownExpanded = false }
                        ) {
                            PlanType.entries.filter { it != PlanType.MOCK_DEMO }.forEach { plan ->
                                DropdownMenuItem(
                                    text = { Text(plan.displayName) },
                                    onClick = {
                                        selectedPlan = plan
                                        planDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = orgId,
                        onValueChange = { orgId = it },
                        label = { Text("Organization ID (Optional)") },
                        placeholder = { Text("org-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Color Identifier", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colorOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.addAccount(
                                nickname = nickname.ifBlank { "OpenAI Account" },
                                email = email.ifBlank { null },
                                apiKey = apiKey,
                                planType = selectedPlan,
                                organizationId = orgId.ifBlank { null },
                                colorHex = selectedColor,
                                isDemoAccount = false
                            )
                        },
                        enabled = !isLoading && apiKey.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Validate & Save Account", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Demo Profiles Section
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Add realistic test profiles to test multi-account views, rate limits, and signed-out alert flows without needing real API keys:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DemoProfileButton(
                        title = "Personal Plus Account",
                        subtitle = "78% remaining • 2h 15m reset",
                        colorHex = "#10B981"
                    ) {
                        viewModel.addAccount(
                            nickname = "Personal Plus",
                            email = "personal@example.com",
                            apiKey = "mock_personal_plus",
                            planType = PlanType.PLUS,
                            organizationId = null,
                            colorHex = "#10B981",
                            isDemoAccount = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DemoProfileButton(
                        title = "Work Team Account",
                        subtitle = "31% remaining • 16h reset",
                        colorHex = "#38BDF8"
                    ) {
                        viewModel.addAccount(
                            nickname = "Work Team",
                            email = "work@company.org",
                            apiKey = "mock_work_team",
                            planType = PlanType.TEAM,
                            organizationId = "org-workcorp",
                            colorHex = "#38BDF8",
                            isDemoAccount = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DemoProfileButton(
                        title = "Enterprise / Tier 5",
                        subtitle = "94% remaining • $1,250 balance",
                        colorHex = "#8B5CF6"
                    ) {
                        viewModel.addAccount(
                            nickname = "Enterprise Suite",
                            email = "lead@enterprise.ai",
                            apiKey = "mock_enterprise",
                            planType = PlanType.ENTERPRISE,
                            organizationId = "org-enterprise",
                            colorHex = "#8B5CF6",
                            isDemoAccount = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DemoProfileButton(
                        title = "Signed-Out Alert Simulator",
                        subtitle = "Triggers 401 signed-out notification",
                        colorHex = "#EF4444"
                    ) {
                        viewModel.addAccount(
                            nickname = "Signed Out Demo",
                            email = "expired@example.com",
                            apiKey = "mock_signed_out_key",
                            planType = PlanType.PLUS,
                            organizationId = null,
                            colorHex = "#EF4444",
                            isDemoAccount = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DemoProfileButton(
                        title = "Rate Limit 429 Simulator",
                        subtitle = "0% remaining • 12m cooldown",
                        colorHex = "#F59E0B"
                    ) {
                        viewModel.addAccount(
                            nickname = "Rate Limited Key",
                            email = "ratelimit@example.com",
                            apiKey = "mock_rate_limit_key",
                            planType = PlanType.API_TIER_1,
                            organizationId = null,
                            colorHex = "#F59E0B",
                            isDemoAccount = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DemoProfileButton(
    title: String,
    subtitle: String,
    colorHex: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(colorHex)))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("+ Add", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
