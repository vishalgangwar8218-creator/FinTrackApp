package com.example.fintrack.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fintrack.api.RetrofitClient
import com.example.fintrack.model.Expense
import com.example.fintrack.model.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class ExpenseViewModel(application: Application): AndroidViewModel(application) {
    val isLoadingExpense = mutableStateOf(false)
    private val sessionManager = SessionManager(application)

    private val _expenseList = mutableStateListOf<Expense>()
    val expenseList: List<Expense> get() = _expenseList

    init {
        fetchExpenses()
    }

    fun getCurrentUserId(): String {
        return sessionManager.fetchUserEmail() ?: ""
    }

    // 🔹 Session Switch Fix: Login / Logout par isko hamesha call karein
    fun onUserSessionChanged() {
        _expenseList.clear()
        fetchExpenses()
    }

    fun fetchExpenses() {
        val userId = getCurrentUserId()

        // Safety: Agar user email null/empty hai toh AWS call na karein aur screen clear rakhein
        if (userId.isEmpty() || userId == "default_user") {
            _expenseList.clear()
            Log.w("AWS_FETCH", "User email empty/invalid, list cleared.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getExpenses(userId)
                if (response.isSuccessful && response.body() != null) {
                    withContext(Dispatchers.Main) {
                        _expenseList.clear() // Previous items wipe out
                        _expenseList.addAll(response.body()!!)
                        Log.d("AWS_FETCH", "Successfully fetched ${response.body()!!.size} items for $userId")
                    }
                } else {
                    Log.e("AWS_FETCH", "Failed to fetch expenses: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AWS_FETCH", "Network Exception while fetching: ${e.message}")
            }
        }
    }

    fun uploadExpense(expense: Expense) {
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return

        val expenseWithUser = expense.copy(userId = userId)

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isLoadingExpense.value = true }
            try {
                val response = RetrofitClient.instance.uploadExpense(userId, expenseWithUser)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d("AWS_UPLOAD", "Success! DynamoDB mein save ho gaya.")
                        _expenseList.add(0, expenseWithUser)
                    } else {
                        Log.e("AWS_UPLOAD", "AWS Server Error Code: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AWS_UPLOAD", "Network Exception: ${e.message}")
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingExpense.value = false
                }
            }
        }
    }

    fun uploadReceiptBitmap(bitmap: Bitmap, onResult: (Expense?) -> Unit) {
        if (isLoadingExpense.value) return
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return

        isLoadingExpense.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val scaledBitmap = resizeBitmapStrict(bitmap, 1024)
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)

                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }

                val byteArray = outputStream.toByteArray()
                outputStream.close()

                val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                val jsonString = "{\"image\": \"$base64Image\", \"userId\": \"$userId\"}"
                val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val response = RetrofitClient.instance.uploadReceipt(userId, requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val raw = response.body()!!
                        val safeExpense = Expense(
                            id = raw.id ?: "",
                            userId = userId,
                            title = if (!raw.title.isNullOrEmpty()) raw.title else "Receipt Expense",
                            amount = if (!raw.amount.isNullOrEmpty()) raw.amount else "0.00",
                            category = if (!raw.category.isNullOrEmpty()) raw.category else "General",
                            date = raw.date ?: ""
                        )
                        _expenseList.add(0, safeExpense)
                        onResult(safeExpense)
                    } else {
                        Log.e("OCR_UPLOAD", "Server returned: ${response.code()}")
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(null) }
            } finally {
                withContext(Dispatchers.Main) { isLoadingExpense.value = false }
            }
        }
    }

    fun uploadReceiptUri(imageUri: Uri, context: Context, onResult: (Expense?) -> Unit) {
        if (isLoadingExpense.value) return
        val userId = getCurrentUserId()
        if (userId.isEmpty()) return

        isLoadingExpense.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = loadAndScaleUriStrict(context, imageUri, 1024)
                if (bitmap == null) {
                    Log.e("OCR_UPLOAD", "Failed to load bitmap safely from Uri")
                    withContext(Dispatchers.Main) { onResult(null) }
                    return@launch
                }

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                bitmap.recycle()

                val byteArray = outputStream.toByteArray()
                outputStream.close()

                val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                val jsonString = "{\"image\": \"$base64Image\", \"userId\": \"$userId\"}"
                val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val response = RetrofitClient.instance.uploadReceipt(userId, requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val raw = response.body()!!

                        val safeExpense = Expense(
                            id = raw.id ?: "",
                            userId = userId,
                            title = if (!raw.title.isNullOrEmpty()) raw.title else "Receipt Expense",
                            amount = if (!raw.amount.isNullOrEmpty()) raw.amount else "0.00",
                            category = if (!raw.category.isNullOrEmpty()) raw.category else "General",
                            date = raw.date ?: ""
                        )

                        _expenseList.add(0, safeExpense)
                        onResult(safeExpense)
                    } else {
                        Log.e("OCR_UPLOAD", "Server returned: ${response.code()}")
                        onResult(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(null) }
            } finally {
                withContext(Dispatchers.Main) { isLoadingExpense.value = false }
            }
        }
    }

    private fun resizeBitmapStrict(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun loadAndScaleUriStrict(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension || options.outHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}