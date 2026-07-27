package com.example.fintrack.ui.view

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fintrack.model.Expense
import com.example.fintrack.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesTabScreen(
    viewModel: ExpenseViewModel
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    val expenseList = viewModel.expenseList
    val context = LocalContext.current

    val currentUserId = viewModel.getCurrentUserId()
    // Screen load hote hi DynamoDB se saved expenses fetch karo
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            viewModel.fetchExpenses()
        }
    }

    var showImageSourcesDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            Toast.makeText(context, "Receipt process ho rahi hai...", Toast.LENGTH_SHORT).show()
            viewModel.uploadReceiptBitmap(bitmap) { extractedExpense ->
                extractedExpense?.let {
                    amount = it.amount ?: ""
                    category = it.category ?: ""
                    storeName = it.store ?: it.title ?: ""
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            Toast.makeText(context, "Receipt selected! Extracting details...", Toast.LENGTH_SHORT).show()
            viewModel.uploadReceiptUri(selectedUri, context) { extractedExpense ->
                extractedExpense?.let {
                    amount = it.amount ?: ""
                    category = it.category ?: ""
                    storeName = it.store ?: it.title ?: ""
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 2.dp,
                    start = 14.dp,
                    end = 14.dp,
                    bottom = 8.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 🔹 CLEAN TOP HEADER: Title + Scan Button (Paas ka Avatar Hata Diya Hai)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expenses Manager",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                // Scan Bill Button
                OutlinedButton(
                    onClick = { showImageSourcesDialog = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF80CBC4).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFF80CBC4))
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan Receipt",
                        tint = Color(0xFF80CBC4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Bill", fontSize = 12.sp, color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold)
                }
            }

            // 🔹 COMPACT LOG EXPENSE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                border = BorderStroke(1.dp, Color(0xFF2B2B2B))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF80CBC4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "Log New Expense",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Side-by-Side Compact Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount (₹)", fontSize = 11.sp, color = Color.Gray) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF80CBC4),
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedContainerColor = Color(0xFF252525),
                                unfocusedContainerColor = Color(0xFF252525)
                            )
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category", fontSize = 11.sp, color = Color.Gray) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF80CBC4),
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedContainerColor = Color(0xFF252525),
                                unfocusedContainerColor = Color(0xFF252525)
                            )
                        )
                    }

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store / Merchant Name", fontSize = 11.sp, color = Color.Gray) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF80CBC4),
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedContainerColor = Color(0xFF252525),
                            unfocusedContainerColor = Color(0xFF252525)
                        )
                    )

                    Button(
                        onClick = {
                            if (amount.isNotBlank() && category.isNotBlank()) {
                                val store = if (storeName.isBlank()) "General Store" else storeName
                                val expense = Expense(
                                    amount = amount,
                                    category = category,
                                    date = "Recent",
                                    store = store,
                                    title = store
                                )
                                viewModel.uploadExpense(expense)
                                amount = ""
                                category = ""
                                storeName = ""
                                Toast.makeText(context, "Saved to DynamoDB!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter amount & category", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                    ) {
                        Text("Upload Expense to AWS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // 🔹 RECENT UPLOADS HEADER
            Text(
                text = "Recent Uploads",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )

            // 🔹 RECENT UPLOADS LIST
            LazyColumn(
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenseList) { item ->
                    val displayCategory = item.category?.takeIf { it.isNotBlank() } ?: "General"
                    val displayStore = item.store?.takeIf { it.isNotBlank() }
                        ?: item.title?.takeIf { it.isNotBlank() }
                        ?: "General Store"
                    val displayDate = item.date?.takeIf { it.isNotBlank() } ?: "Recent"
                    val displayAmount = item.amount?.takeIf { it.isNotBlank() } ?: "0.00"

                    val categoryIcon = getCategoryIcon(displayCategory)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        border = BorderStroke(1.dp, Color(0xFF282828)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF00796B).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = null,
                                        tint = Color(0xFF80CBC4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = displayCategory,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "$displayStore • $displayDate",
                                        color = Color(0xFFA0A0A0),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                color = Color(0xFF004D40).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF00796B).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "₹$displayAmount",
                                    color = Color(0xFF80CBC4),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showImageSourcesDialog) {
            AlertDialog(
                onDismissRequest = { showImageSourcesDialog = false },
                title = { Text("Select Receipt Source") },
                text = { Text("Choose Camera or Gallery to scan bill.") },
                confirmButton = {
                    TextButton(onClick = {
                        showImageSourcesDialog = false
                        cameraLauncher.launch(null)
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showImageSourcesDialog = false
                        galleryLauncher.launch("image/*")
                    }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery")
                    }
                }
            )
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    val cat = category.lowercase()
    return when {
        cat.contains("food") || cat.contains("cafe") || cat.contains("dine") || cat.contains("restaurant") -> Icons.Default.Fastfood
        cat.contains("shop") || cat.contains("mart") || cat.contains("store") || cat.contains("cloth") -> Icons.Default.ShoppingBag
        cat.contains("travel") || cat.contains("cab") || cat.contains("fuel") || cat.contains("bus") -> Icons.Default.DirectionsCar
        else -> Icons.Default.Receipt
    }
}