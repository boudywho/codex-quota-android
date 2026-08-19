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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.quota.domain.model.PlanType
import com.codex.quota.ui.theme.Red500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    viewModel: AddAccountViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var passwordVisible by remember { mutableStateOf(false) }
    var planDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Add Account", fontWeight = FontWeight.Bold) },
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
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("ChatGPT OAuth / Token", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("API Key", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Demo Simulator", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    // ChatGPT OAuth & Session Token Tab
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sign In with OpenAI (OAuth 2.0 PKCE)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sign in directly with your ChatGPT Plus, Team, or Enterprise subscription account using PKCE browser authentication.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.startOAuthSignIn(context) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Authenticate via OpenAI OAuth", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                        Text(
                            text = " OR PASTE TOKEN ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                    }

                    // Account Nickname
                    OutlinedTextField(
                        value = state.nickname,
                        onValueChange = { viewModel.onNicknameChange(it) },
                        label = { Text("Account Nickname") },
                        placeholder = { Text("e.g. My ChatGPT Plus") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Token input
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = { viewModel.onApiKeyChange(it) },
                        label = { Text("OAuth / Session / Bearer Token") },
                        placeholder = { Text("sess-... or eyJ...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Subscription Plan Selector
                    ExposedDropdownMenuBox(
                        expanded = planDropdownExpanded,
                        onExpandedChange = { planDropdownExpanded = !planDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = state.planType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subscription Plan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = planDropdownExpanded,
                            onDismissRequest = { planDropdownExpanded = false }
                        ) {
                            listOf(PlanType.PLUS, PlanType.TEAM, PlanType.ENTERPRISE).forEach { plan ->
                                DropdownMenuItem(
                                    text = { Text(plan.displayName) },
                                    onClick = {
                                        viewModel.onPlanTypeChange(plan)
                                        planDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ColorPickerRow(
                        selectedColor = state.selectedColorHex,
                        availableColors = viewModel.availableColors,
                        onColorSelected = { viewModel.onColorChange(it) }
                    )

                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage!!,
                            color = Red500,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = { viewModel.submitAccount(isDemo = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Account & Track Quota", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (selectedTab == 1) {
                    // API Key Tab
                    OutlinedTextField(
                        value = state.nickname,
                        onValueChange = { viewModel.onNicknameChange(it) },
                        label = { Text("Account Nickname") },
                        placeholder = { Text("e.g. Work API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = { viewModel.onApiKeyChange(it) },
                        label = { Text("OpenAI API Key") },
                        placeholder = { Text("sk-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = state.organizationId,
                        onValueChange = { viewModel.onOrganizationIdChange(it) },
                        label = { Text("Organization ID (Optional)") },
                        placeholder = { Text("org-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Plan Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = planDropdownExpanded,
                        onExpandedChange = { planDropdownExpanded = !planDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = state.planType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tier / Plan Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = planDropdownExpanded,
                            onDismissRequest = { planDropdownExpanded = false }
                        ) {
                            listOf(
                                PlanType.API_TIER_1,
                                PlanType.API_TIER_2,
                                PlanType.API_TIER_5
                            ).forEach { plan ->
                                DropdownMenuItem(
                                    text = { Text(plan.displayName) },
                                    onClick = {
                                        viewModel.onPlanTypeChange(plan)
                                        planDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ColorPickerRow(
                        selectedColor = state.selectedColorHex,
                        availableColors = viewModel.availableColors,
                        onColorSelected = { viewModel.onColorChange(it) }
                    )

                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage!!,
                            color = Red500,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = { viewModel.submitAccount(isDemo = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Account & Validate", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Demo Simulator Tab
                    Text(
                        text = "Instantly add realistic simulation profiles to test gauges, multi-account widgets, and signed-out alert notifications without requiring live OpenAI credentials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    listOf(
                        DemoProfile("Personal Account (Plus)", "78% remaining quota (50 msgs / 3 hrs)", PlanType.PLUS, "#10B981"),
                        DemoProfile("Work Account (Team)", "31% remaining quota with rolling window", PlanType.TEAM, "#38BDF8"),
                        DemoProfile("Enterprise Production", "94% remaining high-throughput capacity", PlanType.ENTERPRISE, "#818CF8"),
                        DemoProfile("Signed-Out Account", "Simulates expired token & triggers notification", PlanType.PLUS, "#EF4444", isExpired = true),
                        DemoProfile("Rate-Limited Account (429)", "0% remaining quota with countdown reset", PlanType.API_TIER_1, "#F59E0B")
                    ).forEach { profile ->
                        DemoPresetCard(
                            profile = profile,
                            onClick = {
                                viewModel.onNicknameChange(profile.name)
                                viewModel.onPlanTypeChange(profile.planType)
                                viewModel.onColorChange(profile.color)
                                viewModel.submitAccount(isDemo = true)
                            }
                        )
                    }
                }

                // Security Callout
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Credentials are encrypted with AES-256-GCM hardware keys and isolated per account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

data class DemoProfile(
    val name: String,
    val description: String,
    val planType: PlanType,
    val color: String,
    val isExpired: Boolean = false
)

@Composable
private fun DemoPresetCard(
    profile: DemoProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .background(Color(android.graphics.Color.parseColor(profile.color)))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ColorPickerRow(
    selectedColor: String,
    availableColors: List<String>,
    onColorSelected: (String) -> Unit
) {
    Column {
        Text("Account Accent Color", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            availableColors.forEach { colorHex ->
                val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                val color = Color(android.graphics.Color.parseColor(colorHex))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorSelected(colorHex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
