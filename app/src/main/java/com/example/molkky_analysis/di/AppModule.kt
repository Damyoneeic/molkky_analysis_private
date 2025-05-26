package com.example.molkky_analysis.di

import android.content.Context
import com.example.molkky_analysis.data.local.AppDatabase
import com.example.molkky_analysis.data.repository.ThrowRepository
import com.example.molkky_analysis.ui.practice.PracticeViewModel
// import org.koin.android.ext.koin.androidContext
// import org.koin.androidx.viewmodel.dsl.viewModel
// import org.koin.dsl.module

// Koinを使用する場合のモジュール定義例 (コメントアウト)
/*
val appModule = module {
    // Database
    single { AppDatabase.getDatabase(androidContext()) }

    // DAOs
    single { get<AppDatabase>().userDao() }
    single { get<AppDatabase>().throwDao() }

    // Repositories
    single { ThrowRepository(get()) }
    // 他のRepositoryがあれば追加

    // ViewModels
    viewModel { (userId: Int) -> PracticeViewModel(get(), userId) }
    // 他のViewModelがあれば追加
}
*/

// アプリケーションクラスでKoinを初期化する必要があります
// class YourApplication : Application() {
//    override fun onCreate() {
//        super.onCreate()
//        startKoin {
//            androidContext(this@YourApplication)
//            modules(appModule)
//        }
//    }
// }