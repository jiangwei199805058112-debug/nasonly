package com.example.nasonly.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.nasonly.core.utils.FileUtils.FileSizeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedFilter: FilterOption?,
    onFilterSelected: (FilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var filterExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 搜索框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("搜索") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            )
        )

        // 筛选按钮
        Box {
            IconButton(onClick = { filterExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false }
            ) {
                FilterOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onFilterSelected(option)
                            filterExpanded = false
                        }
                    )
                }
            }
        }
    }
}

enum class FilterOption(val label: String) {
    ALL("全部"),
    SIZE_MB("大于 100 MB"),
    SIZE_GB("大于 1 GB"),
    TYPE_VIDEO("视频文件"),
    TYPE_AUDIO("音频文件");

    fun matches(size: Long, unit: FileSizeUnit): Boolean {
        return when (this) {
            ALL -> true
            SIZE_MB -> unit.toMB(size) > 100
            SIZE_GB -> unit.toGB(size) > 1
            TYPE_VIDEO, TYPE_AUDIO -> true // 实际项目里可以扩展类型过滤逻辑
        }
    }
}
