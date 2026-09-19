package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AldellaViewModel
import com.example.ui.ReportFilterCriteria
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportExportDialog(
    viewModel: AldellaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var reportFormatTab by remember { mutableIntStateOf(0) }

    // Calendar Defaults
    val cal = remember { Calendar.getInstance() }
    val currentYearStr = remember { cal.get(Calendar.YEAR).toString() }
    val currentMonthStr = remember { String.format("%02d", cal.get(Calendar.MONTH) + 1) }
    val currentDayStr = remember { String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)) }

    // Filter states (Default to ALL for full data, or selectable)
    var selectedYear by remember { mutableStateOf("ALL") }
    var selectedMonth by remember { mutableStateOf("ALL") }
    var selectedDay by remember { mutableStateOf("ALL") }
    var showFilterOptions by remember { mutableStateOf(false) }

    val filterCriteria = remember(selectedYear, selectedMonth, selectedDay) {
        ReportFilterCriteria(
            year = selectedYear,
            month = selectedMonth,
            day = selectedDay
        )
    }

    // Dynamic 30-year span (from 2024 to 2056)
    val yearsList = remember {
        listOf("ALL") + (2024..2056).map { it.toString() }
    }

    val monthsList = listOf(
        "ALL" to "All Months",
        "01" to "01 - Jan",
        "02" to "02 - Feb",
        "03" to "03 - Mar",
        "04" to "04 - Apr",
        "05" to "05 - May",
        "06" to "06 - Jun",
        "07" to "07 - Jul",
        "08" to "08 - Aug",
        "09" to "09 - Sep",
        "10" to "10 - Oct",
        "11" to "11 - Nov",
        "12" to "12 - Dec"
    )

    val daysList = remember {
        listOf("ALL") + (1..31).map { String.format("%02d", it) }
    }

    val formatName = when (reportFormatTab) {
        0 -> "PDF"
        1 -> "EXCEL"
        else -> "DOCS"
    }

    // Filtered Content Preview
    val filteredSheets = remember(filterCriteria, viewModel.allProcessSheets.collectAsState().value) {
        viewModel.getFilteredProcessSheets(filterCriteria)
    }
    val filteredComplaints = remember(filterCriteria, viewModel.allComplaintsAndDowntime.collectAsState().value) {
        viewModel.getFilteredComplaints(filterCriteria)
    }
    val filteredAttendance = remember(filterCriteria, viewModel.allAttendance.collectAsState().value) {
        viewModel.getFilteredAttendance(filterCriteria)
    }

    val reportPreview = when (reportFormatTab) {
        0 -> viewModel.getDailyReportPdfContent(filterCriteria)
        1 -> viewModel.getDailyReportExcelCsvContent(filterCriteria)
        else -> viewModel.getDailyReportDocsContent(filterCriteria)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (reportFormatTab) {
                            0 -> Icons.Default.PictureAsPdf
                            1 -> Icons.Default.TableChart
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = when (reportFormatTab) {
                            0 -> AldellaRed
                            1 -> AldellaGreen
                            else -> AldellaBluePrimary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Production Report & Export",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AldellaTextDark
                        )
                        Text(
                            text = "30-Year Archive (2024–2056) • Filter & Download",
                            fontSize = 11.sp,
                            color = AldellaTextMuted
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                // FORMAT TABS
                TabRow(
                    selectedTabIndex = reportFormatTab,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = AldellaBluePrimary
                ) {
                    Tab(
                        selected = reportFormatTab == 0,
                        onClick = { reportFormatTab = 0 },
                        text = { Text("PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = reportFormatTab == 1,
                        onClick = { reportFormatTab = 1 },
                        text = { Text("Excel (CSV)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = reportFormatTab == 2,
                        onClick = { reportFormatTab = 2 },
                        text = { Text("Word / Docs", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FILTER CONTROLS HEADER / TOGGLE
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFilterOptions = !showFilterOptions },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = AldellaBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Date Filter: [Year: $selectedYear] [Month: $selectedMonth] [Day: $selectedDay]",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AldellaBluePrimary
                                )
                            }
                            Icon(
                                if (showFilterOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = AldellaBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // EXPANDABLE FILTER SELECTORS
                        if (showFilterOptions) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // PRESET SHORTCUTS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedYear == "ALL" && selectedMonth == "ALL" && selectedDay == "ALL",
                                    onClick = {
                                        selectedYear = "ALL"
                                        selectedMonth = "ALL"
                                        selectedDay = "ALL"
                                    },
                                    label = { Text("All Data (Full Archive)", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = selectedYear == currentYearStr && selectedMonth == currentMonthStr && selectedDay == currentDayStr,
                                    onClick = {
                                        selectedYear = currentYearStr
                                        selectedMonth = currentMonthStr
                                        selectedDay = currentDayStr
                                    },
                                    label = { Text("Today ($currentDayStr/$currentMonthStr)", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = selectedYear == currentYearStr && selectedMonth == currentMonthStr && selectedDay == "ALL",
                                    onClick = {
                                        selectedYear = currentYearStr
                                        selectedMonth = currentMonthStr
                                        selectedDay = "ALL"
                                    },
                                    label = { Text("This Month ($currentMonthStr/$currentYearStr)", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = selectedYear == currentYearStr && selectedMonth == "ALL" && selectedDay == "ALL",
                                    onClick = {
                                        selectedYear = currentYearStr
                                        selectedMonth = "ALL"
                                        selectedDay = "ALL"
                                    },
                                    label = { Text("Full Year ($currentYearStr)", fontSize = 10.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // YEAR SELECTION ROW
                            Text("Filter by Year (2024 to 2056):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AldellaTextDark)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                yearsList.forEach { y ->
                                    val isSelected = selectedYear == y
                                    Surface(
                                        color = if (isSelected) AldellaBluePrimary else Color.White,
                                        shape = RoundedCornerShape(4.dp),
                                        border = CardDefaults.outlinedCardBorder(),
                                        modifier = Modifier.clickable { selectedYear = y }
                                    ) {
                                        Text(
                                            text = y,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else AldellaTextDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // MONTH SELECTION ROW
                            Text("Filter by Month:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AldellaTextDark)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                monthsList.forEach { (mVal, mLabel) ->
                                    val isSelected = selectedMonth == mVal
                                    Surface(
                                        color = if (isSelected) AldellaBluePrimary else Color.White,
                                        shape = RoundedCornerShape(4.dp),
                                        border = CardDefaults.outlinedCardBorder(),
                                        modifier = Modifier.clickable { selectedMonth = mVal }
                                    ) {
                                        Text(
                                            text = mLabel,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else AldellaTextDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // DAY SELECTION ROW
                            Text("Filter by Day of Month (01 to 31):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AldellaTextDark)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                daysList.forEach { d ->
                                    val isSelected = selectedDay == d
                                    Surface(
                                        color = if (isSelected) AldellaBluePrimary else Color.White,
                                        shape = RoundedCornerShape(4.dp),
                                        border = CardDefaults.outlinedCardBorder(),
                                        modifier = Modifier.clickable { selectedDay = d }
                                    ) {
                                        Text(
                                            text = d,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else AldellaTextDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // LIVE MATCH COUNTER BADGE
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Found: ${filteredSheets.size} Batches • ${filteredComplaints.size} Downtimes • ${filteredAttendance.size} Staff records",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filteredSheets.isNotEmpty()) AldellaGreen else AldellaRed
                            )
                            if (selectedYear != "ALL" || selectedMonth != "ALL" || selectedDay != "ALL") {
                                Text(
                                    text = "Clear Filter",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AldellaBluePrimary,
                                    modifier = Modifier.clickable {
                                        selectedYear = "ALL"
                                        selectedMonth = "ALL"
                                        selectedDay = "ALL"
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // REPORT CONTENT PREVIEW
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = reportPreview,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AldellaTextDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ACTION BUTTONS: DOWNLOAD & SHARE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // DOWNLOAD FILTERED REPORT
                    Button(
                        onClick = {
                            viewModel.downloadReportDocument(context, formatName, filterCriteria)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_daily_report_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedYear == "ALL" && selectedMonth == "ALL" && selectedDay == "ALL") "Download Full $formatName" else "Download Filtered $formatName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // SHARE FILTERED REPORT
                    OutlinedButton(
                        onClick = {
                            viewModel.shareReportDocument(context, formatName, filterCriteria)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaBluePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_daily_report_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share $formatName", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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

