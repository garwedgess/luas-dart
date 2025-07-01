package com.wedgess.luas.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import com.wedgess.luas.presentation.model.DropdownItem
import com.wedgess.luas.presentation.model.DropdownTrailingContent
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownTextField(
    selectedValue: DropdownItem,
    options: ImmutableList<DropdownItem>,
    label: String,
    onValueChange: (DropdownItem) -> Unit,
    valueFormatter: (DropdownItem) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = valueFormatter(selectedValue),
            onValueChange = { value ->
                onValueChange(options.first { valueFormatter(it) == value })
            },
            label = { Text(text = label) },
            suffix = if (selectedValue is DropdownTrailingContent) {
                @Composable {
                    selectedValue.RenderTrailingContent()
                }
            } else {
                null
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = valueFormatter(option)) },
                    leadingIcon = if (option == selectedValue) {
                        @Composable {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF119955)
                            )
                        }
                    } else {
                        null
                    },
                    trailingIcon = {
                        (option as? DropdownTrailingContent)?.RenderTrailingContent()
                    },
                    onClick = {
                        focusManager.clearFocus()
                        expanded = false
                        onValueChange(option)
                    }
                )
            }
        }
    }
}
