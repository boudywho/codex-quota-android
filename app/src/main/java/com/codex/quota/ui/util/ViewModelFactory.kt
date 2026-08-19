package com.codex.quota.ui.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Suppress("UNCHECKED_CAST")
@Composable
inline fun <reified VM : ViewModel> scopedViewModel(
    key: String? = null,
    crossinline creator: () -> VM
): VM {
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return creator() as T
            }
        }
    )
}
