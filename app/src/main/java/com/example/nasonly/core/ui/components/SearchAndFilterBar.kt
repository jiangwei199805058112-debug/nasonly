package nasonly.ui.components

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
import nasonly.core.utils.FileUtils.FileSizeUnit

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
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索"
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除搜索"
                        )
                    }
                }
            },
            placeholder = { Text("搜索视频...") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            )
        )

        // 筛选按钮
        Box {
            IconButton(onClick = { filterExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "筛选"
                )
            }

            // 筛选下拉菜单
            DropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false },
                modifier = Modifier.width(200.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("无筛选") },
                    onClick = {
                        onFilterSelected(FilterOption.NONE)
                        filterExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("按名称排序 (A-Z)") },
                    onClick = {
                        onFilterSelected(FilterOption.NAME_ASC)
                        filterExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("按名称排序 (Z-A)") },
                    onClick = {
                        onFilterSelected(FilterOption.NAME_DESC)
                        filterExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("按大小排序 (从小到大)") },
                    onClick = {
                        onFilterSelected(FilterOption.SIZE_ASC)
                        filterExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("按大小排序 (从大到小)") },
                    onClick = {
                        onFilterSelected(FilterOption.SIZE_DESC)
                        filterExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("仅显示大文件 (>1GB)") },
                    onClick = {
                        onFilterSelected(FilterOption.LARGE_FILES)
                        filterExpanded = false
                    }
                )
            }
        }
    }
}

// 筛选选项
enum class FilterOption {
    NONE,          // 无筛选
    NAME_ASC,      // 名称升序
    NAME_DESC,     // 名称降序
    SIZE_ASC,      // 大小升序
    SIZE_DESC,     // 大小降序
    LARGE_FILES    // 仅大文件 (>1GB)
}