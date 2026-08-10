package com.example.expensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SummaryState(
    val balance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    val transactions: StateFlow<List<TransactionEntity>>
    val summary: StateFlow<SummaryState>

    val incomeCategories = listOf("Salary", "Business", "Investment", "Gift", "Food", "Transport", "Education", "Shopping", "Bills", "Entertainment", "Others")
    val expenseCategories = listOf("Food", "Transport", "Education", "Shopping", "Bills", "Entertainment", "Others")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransactionRepository(database.transactionDao())

        transactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        summary = repository.allTransactions.map { list ->
            var income = 0.0
            var expense = 0.0
            for (t in list) {
                if (t.type == "income") {
                    income += t.amount
                } else {
                    expense += t.amount
                }
            }
            SummaryState(
                balance = income - expense,
                totalIncome = income,
                totalExpense = expense
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SummaryState()
        )
    }

    fun addTransaction(
        type: String,
        amount: Double,
        category: String,
        date: String,
        description: String
    ) {
        viewModelScope.launch {
            val transaction = TransactionEntity(
                type = type,
                amount = amount,
                category = category,
                date = date,
                description = description
            )
            repository.insert(transaction)
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}
