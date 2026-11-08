package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FilterAction
import com.example.data.model.FilterRule
import com.example.data.model.FilterType
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun FiltersScreen(
    rules: List<FilterRule>,
    onAddRule: (FilterRule) -> Unit,
    onUpdateRule: (FilterRule) -> Unit,
    onToggleRule: (FilterRule) -> Unit,
    onDeleteRule: (FilterRule) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<FilterRule?>(null) }
    var ruleToDelete by remember { mutableStateOf<FilterRule?>(null) }

    val filteredRules = remember(rules, searchQuery) {
        if (searchQuery.isBlank()) rules else {
            rules.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.filterValue.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("filters_screen")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Description
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SMS Filter Rules",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize forwarding rules based on contacts, keywords, or regex expressions. Rules are evaluated in order.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search rules by name or keywords...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_rules_input")
                )
            }

            // Quick Preset Suggestions (if few rules)
            if (rules.size <= 2) {
                item {
                    Text(
                        text = "Quick Templates",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onAddRule(
                                    FilterRule(
                                        name = "Urgent / Alert SMS",
                                        filterType = FilterType.KEYWORD,
                                        filterValue = "urgent, alert, immediate, emergency",
                                        action = FilterAction.FORWARD
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚨 Urgent Alert", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onAddRule(
                                    FilterRule(
                                        name = "Specific Phone / Family",
                                        filterType = FilterType.SENDER,
                                        filterValue = "+1",
                                        action = FilterAction.FORWARD
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📱 Contact Filter", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Rules List
            if (filteredRules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching rules found" else "No filter rules yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Create a new rule using the button below to filter specific contacts or keywords.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredRules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        onToggle = { onToggleRule(rule) },
                        onEdit = {
                            editingRule = rule
                            showEditDialog = true
                        },
                        onDelete = { ruleToDelete = rule }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Add Rule Floating Action Button
        FloatingActionButton(
            onClick = {
                editingRule = null
                showEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_rule_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Filter Rule")
        }
    }

    // Edit/Add Rule Dialog
    if (showEditDialog) {
        RuleEditDialog(
            existingRule = editingRule,
            onDismiss = {
                showEditDialog = false
                editingRule = null
            },
            onSave = { rule ->
                if (editingRule != null) {
                    onUpdateRule(rule.copy(id = editingRule!!.id, createdAt = editingRule!!.createdAt))
                } else {
                    onAddRule(rule)
                }
                showEditDialog = false
                editingRule = null
            }
        )
    }

    // Delete Confirmation
    if (ruleToDelete != null) {
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text("Delete Filter Rule") },
            text = { Text("Are you sure you want to delete '${ruleToDelete?.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        ruleToDelete?.let { onDeleteRule(it) }
                        ruleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RuleCard(
    rule: FilterRule,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rule_card_${rule.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (rule.isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("rule_switch_${rule.id}")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = when (rule.filterType) {
                            FilterType.ALL -> "ALL SMS"
                            FilterType.KEYWORD -> "KEYWORD"
                            FilterType.SENDER -> "CONTACT/PHONE"
                            FilterType.REGEX -> "REGEX"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (rule.action == FilterAction.FORWARD) {
                        SuccessGreen.copy(alpha = 0.12f)
                    } else {
                        ErrorRed.copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = if (rule.action == FilterAction.FORWARD) "FORWARD" else "BLOCK",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rule.action == FilterAction.FORWARD) SuccessGreen else ErrorRed
                    )
                }

                if (rule.caseSensitive) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "CASE SENSITIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (rule.filterType != FilterType.ALL && rule.filterValue.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pattern: \"${rule.filterValue}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!rule.targetEmailOverride.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mail,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Override: ${rule.targetEmailOverride}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp).testTag("edit_rule_${rule.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_rule_${rule.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleEditDialog(
    existingRule: FilterRule?,
    onDismiss: () -> Unit,
    onSave: (FilterRule) -> Unit
) {
    var name by remember { mutableStateOf(existingRule?.name ?: "") }
    var filterType by remember { mutableStateOf(existingRule?.filterType ?: FilterType.KEYWORD) }
    var filterValue by remember { mutableStateOf(existingRule?.filterValue ?: "") }
    var caseSensitive by remember { mutableStateOf(existingRule?.caseSensitive ?: false) }
    var action by remember { mutableStateOf(existingRule?.action ?: FilterAction.FORWARD) }
    var targetEmailOverride by remember { mutableStateOf(existingRule?.targetEmailOverride ?: "") }

    var testInput by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<Boolean?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (existingRule == null) "New Filter Rule" else "Edit Filter Rule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name *") },
                    placeholder = { Text("e.g. Bank 2FA, Boss SMS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rule_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Filter Condition Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterType == FilterType.KEYWORD,
                        onClick = { filterType = FilterType.KEYWORD },
                        label = { Text("Keyword", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterType == FilterType.SENDER,
                        onClick = { filterType = FilterType.SENDER },
                        label = { Text("Sender", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterType == FilterType.REGEX,
                        onClick = { filterType = FilterType.REGEX },
                        label = { Text("Regex", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterType == FilterType.ALL,
                        onClick = { filterType = FilterType.ALL },
                        label = { Text("All SMS", fontSize = 11.sp) }
                    )
                }

                if (filterType != FilterType.ALL) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = filterValue,
                        onValueChange = {
                            filterValue = it
                            testResult = null
                        },
                        label = {
                            Text(
                                when (filterType) {
                                    FilterType.KEYWORD -> "Keywords (comma-separated)"
                                    FilterType.SENDER -> "Sender Number or Name"
                                    FilterType.REGEX -> "Regular Expression"
                                    FilterType.ALL -> ""
                                }
                            )
                        },
                        placeholder = {
                            Text(
                                when (filterType) {
                                    FilterType.KEYWORD -> "e.g. OTP, verification, code, secure"
                                    FilterType.SENDER -> "e.g. +14155552671 or BankAlert"
                                    FilterType.REGEX -> "e.g. \\b\\d{6}\\b"
                                    FilterType.ALL -> ""
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("rule_value_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Case sensitive match")
                        Switch(
                            checked = caseSensitive,
                            onCheckedChange = { caseSensitive = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Action",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = action == FilterAction.FORWARD,
                        onClick = { action = FilterAction.FORWARD },
                        label = { Text("Forward to Email") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )

                    FilterChip(
                        selected = action == FilterAction.EXCLUDE,
                        onClick = { action = FilterAction.EXCLUDE },
                        label = { Text("Ignore / Block") },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                if (action == FilterAction.FORWARD) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = targetEmailOverride,
                        onValueChange = { targetEmailOverride = it },
                        label = { Text("Custom Recipient (Optional)") },
                        placeholder = { Text("Leave blank to use default email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Interactive Pattern Tester inside dialog
                if (filterType != FilterType.ALL && filterValue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Rule Tester",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = testInput,
                                    onValueChange = {
                                        testInput = it
                                        testResult = testRuleMatch(filterType, filterValue, caseSensitive, it)
                                    },
                                    placeholder = { Text("Type test text or number...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (testResult != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (testResult == true) "✓ Matches this rule!" else "✗ No match",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (testResult == true) SuccessGreen else ErrorRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    FilterRule(
                                        name = name.trim(),
                                        filterType = filterType,
                                        filterValue = filterValue.trim(),
                                        caseSensitive = caseSensitive,
                                        action = action,
                                        targetEmailOverride = targetEmailOverride.trim().ifEmpty { null }
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("save_rule_btn")
                    ) {
                        Text("Save Rule")
                    }
                }
            }
        }
    }
}

private fun testRuleMatch(
    type: FilterType,
    filterValue: String,
    caseSensitive: Boolean,
    testInput: String
): Boolean {
    if (testInput.isBlank() || filterValue.isBlank()) return false
    return when (type) {
        FilterType.ALL -> true
        FilterType.KEYWORD -> {
            val tokens = filterValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            tokens.any { token ->
                testInput.contains(token, ignoreCase = !caseSensitive)
            }
        }
        FilterType.SENDER -> {
            val cleanFilter = filterValue.replace(" ", "").trim()
            val cleanInput = testInput.replace(" ", "").trim()
            val tokens = cleanFilter.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            tokens.any { token ->
                cleanInput.contains(token, ignoreCase = !caseSensitive) ||
                        cleanInput.endsWith(token)
            }
        }
        FilterType.REGEX -> {
            try {
                val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                Regex(filterValue.trim(), options).containsMatchIn(testInput)
            } catch (_: Exception) {
                false
            }
        }
    }
}
