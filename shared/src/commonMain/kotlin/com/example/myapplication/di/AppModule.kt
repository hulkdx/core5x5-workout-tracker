package com.example.myapplication.di

import com.example.myapplication.shell.ShellViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val appModule = module {
    viewModel { ShellViewModel() }
}
