package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProcessSheetEntity
import com.example.ui.AldellaViewModel
import com.example.ui.ProductCatalogItem
import com.example.ui.BrandCatalogItem
import com.example.ui.BatchCatalogItem
import com.example.ui.theme.*

enum class AttributeMode {
    PRODUCT,
    BRAND,
    BATCH_NO
}

@Composable
fun AdminAttributeManagerDialog(
    mode: AttributeMode,
    currentValue: String,
    viewModel: AldellaViewModel,
    onSelect: (name: String, code: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val productCatalog by viewModel.productCatalog.collectAsState()
    val brandCatalog by viewModel.brandCatalog.collectAsState()
    val batchCatalog by viewModel.batchCatalog.collectAsState()
    val allSheets by viewModel.allProcessSheets.collectAsState()

    // Add inputs
    var newNameInput by remember { mutableStateOf("") }
    var newCodeInput by remember { mutableStateOf("") }

    // Rename state
    var itemToRename by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var renameCodeInput by remember { mutableStateOf("") }

    // Delete confirmation state
    var itemToDeletePermanently by remember { mutableStateOf<String?>(null) }
    var sheetToDeletePermanently by remember { mutableStateOf<ProcessSheetEntity?>(null) }

    // RENAME DIALOG
    itemToRename?.let { oldName ->
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = {
                Text(
                    text = "Rename ${mode.name.replace("_", " ")}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current Name: $oldName",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("New Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_attribute_input")
                    )
                    if (mode == AttributeMode.PRODUCT) {
                        OutlinedTextField(
                            value = renameCodeInput,
                            onValueChange = { renameCodeInput = it },
                            label = { Text("Product Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("rename_attribute_code_input")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            when (mode) {
                                AttributeMode.PRODUCT -> viewModel.renameProduct(oldName, renameInput, renameCodeInput)
                                AttributeMode.BRAND -> viewModel.renameBrand(oldName, renameInput)
                                AttributeMode.BATCH_NO -> viewModel.renameBatchNo(oldName, renameInput)
                            }
                            Toast.makeText(context, "Renamed to \"${renameInput.trim()}\"!", Toast.LENGTH_SHORT).show()
                            if (currentValue == oldName) {
                                onSelect(renameInput.trim(), renameCodeInput.ifBlank { null })
                            }
                            itemToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_rename_attribute_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) { Text("Cancel") }
            }
        )
    }

    // PERMANENT DELETE DIALOG (CATALOG ITEM)
    itemToDeletePermanently?.let { itemToDelete ->
        AlertDialog(
            onDismissRequest = { itemToDeletePermanently = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AldellaRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Permanently?", fontWeight = FontWeight.Bold, color = AldellaRed)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"$itemToDelete\"? This item will be removed permanently from the factory catalogue.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (mode) {
                            AttributeMode.PRODUCT -> viewModel.deleteProductPermanently(itemToDelete)
                            AttributeMode.BRAND -> viewModel.deleteBrandPermanently(itemToDelete)
                            AttributeMode.BATCH_NO -> viewModel.deleteBatchNoPermanently(itemToDelete)
                        }
                        Toast.makeText(context, "\"$itemToDelete\" permanently deleted!", Toast.LENGTH_SHORT).show()
                        itemToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaRed),
                    modifier = Modifier.testTag("confirm_permanent_delete_btn")
                ) {
                    Text("Delete Permanent", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeletePermanently = null }) { Text("Cancel") }
            }
        )
    }

    // PERMANENT DELETE DIALOG (SUBMITTED BATCH SHEET)
    sheetToDeletePermanently?.let { sheet ->
        AlertDialog(
            onDismissRequest = { sheetToDeletePermanently = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AldellaRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Batch Record Permanently?", fontWeight = FontWeight.Bold, color = AldellaRed)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete batch record #${sheet.id} (${sheet.batchNo} • ${sheet.productName}) from the database? This cannot be undone.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProcessSheetPermanently(sheet)
                        Toast.makeText(context, "Batch ${sheet.batchNo} permanently deleted from database!", Toast.LENGTH_LONG).show()
                        sheetToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaRed)
                ) {
                    Text("Delete Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sheetToDeletePermanently = null }) { Text("Cancel") }
            }
        )
    }

    // MAIN DIALOG
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (mode) {
                            AttributeMode.PRODUCT -> Icons.Default.Inventory2
                            AttributeMode.BRAND -> Icons.Default.Verified
                            AttributeMode.BATCH_NO -> Icons.Default.QrCode
                        },
                        contentDescription = null,
                        tint = AldellaBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when (mode) {
                                AttributeMode.PRODUCT -> "Product Manager (Admin)"
                                AttributeMode.BRAND -> "Brand Manager (Admin)"
                                AttributeMode.BATCH_NO -> "Batch No Manager (Admin)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AldellaTextDark
                        )
                        Text(
                            text = "Rename • Remove • Delete Permanent • Add",
                            fontSize = 11.sp,
                            color = AldellaBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // Current Selected Box
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected on sheet: ",
                            fontSize = 11.sp,
                            color = AldellaTextMuted
                        )
                        Text(
                            text = currentValue.ifBlank { "--" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AldellaBluePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ADD NEW ITEM SECTION
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "+ Add New ${mode.name.replace("_", " ")}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AldellaTextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newNameInput,
                                onValueChange = { newNameInput = it },
                                placeholder = {
                                    Text(
                                        when (mode) {
                                            AttributeMode.PRODUCT -> "Product name..."
                                            AttributeMode.BRAND -> "Brand name..."
                                            AttributeMode.BATCH_NO -> "Batch number (e.g. Batch 5)..."
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("new_attribute_name_input")
                            )

                            if (mode == AttributeMode.PRODUCT) {
                                OutlinedTextField(
                                    value = newCodeInput,
                                    onValueChange = { newCodeInput = it },
                                    placeholder = { Text("Code...", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(50.dp)
                                        .testTag("new_attribute_code_input")
                                )
                            }

                            Button(
                                onClick = {
                                    if (newNameInput.isNotBlank()) {
                                        when (mode) {
                                            AttributeMode.PRODUCT -> {
                                                viewModel.addProduct(newNameInput, newCodeInput)
                                                onSelect(newNameInput.trim(), newCodeInput.trim().ifBlank { null })
                                            }
                                            AttributeMode.BRAND -> {
                                                viewModel.addBrand(newNameInput)
                                                onSelect(newNameInput.trim(), null)
                                            }
                                            AttributeMode.BATCH_NO -> {
                                                viewModel.addBatchNo(newNameInput)
                                                onSelect(newNameInput.trim(), null)
                                            }
                                        }
                                        Toast.makeText(context, "Added \"${newNameInput.trim()}\"!", Toast.LENGTH_SHORT).show()
                                        newNameInput = ""
                                        newCodeInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(48.dp).testTag("add_attribute_submit_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Catalog Items & Records",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AldellaTextDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                // LIST OF ITEMS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (mode) {
                        AttributeMode.PRODUCT -> {
                            productCatalog.forEach { prod ->
                                val isCurrent = prod.name.equals(currentValue, ignoreCase = true)
                                ItemRow(
                                    title = prod.name,
                                    subtitle = "Code: ${prod.code}",
                                    isSelected = isCurrent,
                                    onSelect = {
                                        onSelect(prod.name, prod.code)
                                        Toast.makeText(context, "Selected: ${prod.name}", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    onRename = {
                                        itemToRename = prod.name
                                        renameInput = prod.name
                                        renameCodeInput = prod.code
                                    },
                                    onDeletePermanent = {
                                        itemToDeletePermanently = prod.name
                                    }
                                )
                            }
                        }
                        AttributeMode.BRAND -> {
                            brandCatalog.forEach { br ->
                                val isCurrent = br.name.equals(currentValue, ignoreCase = true)
                                ItemRow(
                                    title = br.name,
                                    subtitle = "Brand Item",
                                    isSelected = isCurrent,
                                    onSelect = {
                                        onSelect(br.name, null)
                                        Toast.makeText(context, "Selected: ${br.name}", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    onRename = {
                                        itemToRename = br.name
                                        renameInput = br.name
                                    },
                                    onDeletePermanent = {
                                        itemToDeletePermanently = br.name
                                    }
                                )
                            }
                        }
                        AttributeMode.BATCH_NO -> {
                            batchCatalog.forEach { b ->
                                val isCurrent = b.name.equals(currentValue, ignoreCase = true)
                                ItemRow(
                                    title = b.name,
                                    subtitle = "Batch Sequence Identifier",
                                    isSelected = isCurrent,
                                    onSelect = {
                                        onSelect(b.name, null)
                                        Toast.makeText(context, "Selected: ${b.name}", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    onRename = {
                                        itemToRename = b.name
                                        renameInput = b.name
                                    },
                                    onDeletePermanent = {
                                        itemToDeletePermanently = b.name
                                    }
                                )
                            }

                            // If there are submitted batch sheets in database, also offer option to delete record permanently
                            val submittedBatches = allSheets.filter { it.isSubmitted }
                            if (submittedBatches.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Submitted Batches (${submittedBatches.size}) — Touch to Delete Permanent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AldellaRed
                                )
                                submittedBatches.take(6).forEach { sheet ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                                        border = CardDefaults.outlinedCardBorder()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${sheet.batchNo} • ${sheet.productName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = AldellaTextDark
                                                )
                                                Text(
                                                    text = "${sheet.areaKey} / ${sheet.processKey} • ${sheet.totalKgQty} kg",
                                                    fontSize = 10.sp,
                                                    color = AldellaTextMuted
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = { sheetToDeletePermanently = sheet },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Delete Permanent", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color.White
    )
}

@Composable
private fun ItemRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDeletePermanent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AldellaTextDark
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = AldellaGreen,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = AldellaTextMuted
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SELECT
                if (!isSelected) {
                    FilledTonalButton(
                        onClick = onSelect,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AldellaBlueLight,
                            contentColor = AldellaBluePrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Select", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // RENAME
                IconButton(
                    onClick = onRename,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Rename",
                        tint = AldellaBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // DELETE PERMANENT
                IconButton(
                    onClick = onDeletePermanent,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Delete Permanent",
                        tint = AldellaRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
