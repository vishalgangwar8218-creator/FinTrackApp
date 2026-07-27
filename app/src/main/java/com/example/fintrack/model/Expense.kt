package com.example.fintrack.model

import com.google.gson.annotations.SerializedName

data class Expense(
    @SerializedName("id")
    val id: String? = "",

    @SerializedName("userId")
    val userId: String? = "",

    @SerializedName("title")
    val title: String? = "Scanned Receipt",

    @SerializedName("amount")
    val amount: String? = "0.00",

    @SerializedName("category")
    val category: String? = "General",

    @SerializedName("date")
    val date: String? = "",

    @SerializedName("store")
    val store: String? = "General Store"
)