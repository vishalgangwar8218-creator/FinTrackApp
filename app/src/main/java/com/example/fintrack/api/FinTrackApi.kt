package com.example.fintrack.api

import com.example.fintrack.model.ChatRequest
import com.example.fintrack.model.ChatResponse
import com.example.fintrack.model.Expense
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface FinTrackApi {

    // DynamoDB se saare saved expenses fetch karne ke liye (GET)
    @GET("process")
    suspend fun getExpenses(
        @Query("userId") userId: String
    ): Response<List<Expense>>

    @POST("process")
    suspend fun sendReceiptData(@Body expense: Expense): Response<String>

    @POST("chat") // AWS par chat ke liye route
    suspend fun sendChatMessage(@Body request: ChatRequest): Response<ChatResponse>

    // Manual expense upload karne ke liye
    @POST("process")
    suspend fun uploadExpense(
        @Query("userId") userId: String,
        @Body expense: Expense
    ): Response<Expense>

    // Receipt image upload karke OCR se details extract karne ke liye
    @Headers("Content-Type: application/json")
    @POST("ocr-scan")
    suspend fun uploadReceipt(
        @Query("userId") userId: String,
        @Body receipt: RequestBody
    ): Response<Expense>
}