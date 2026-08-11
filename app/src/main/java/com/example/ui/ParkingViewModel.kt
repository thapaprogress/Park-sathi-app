package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.print.ParkingPrintEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

import com.example.network.ParkSathiNetworkClient
import com.example.util.CloudSyncEngine
import com.example.util.BoomBarrierRelayManager
import com.example.util.IrdSyncWorker
import com.example.util.NepalIrdInvoiceHelper
import com.example.util.NepalTariffConfig
import com.example.util.SaaSSubscriptionManager
import com.example.util.SubscriptionInfo
import com.example.util.SubscriptionPlan

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    // Persistent Settings Storage
    private val prefs = application.getSharedPreferences("utpala_parking_settings", Context.MODE_PRIVATE)

    private val db = AppDatabase.getDatabase(application)
    private val repository = ParkingTicketRepository(db.parkingTicketDao())
    private val printEngine = ParkingPrintEngine.getInstance()
    val syncEngine = CloudSyncEngine(db.parkingTicketDao(), application)
    val barrierRelayManager = BoomBarrierRelayManager.getInstance()

    // Operator Shift Management
    private val _activeOperatorId = MutableStateFlow(prefs.getString("active_operator_id", "ATT-8842") ?: "ATT-8842")
    val activeOperatorId: StateFlow<String> = _activeOperatorId.asStateFlow()

    private val _isOperatorLoggedIn = MutableStateFlow(true)
    val isOperatorLoggedIn: StateFlow<Boolean> = _isOperatorLoggedIn.asStateFlow()

    private val _gateBarrierDurationSec = MutableStateFlow(prefs.getInt("gate_barrier_dur_sec", 5))
    val gateBarrierDurationSec: StateFlow<Int> = _gateBarrierDurationSec.asStateFlow()

    // SaaS Subscription Manager
    val subscriptionManager = SaaSSubscriptionManager(application)
    private val _subscriptionInfo = MutableStateFlow(subscriptionManager.getSubscriptionInfo())
    val subscriptionInfoFlow: StateFlow<SubscriptionInfo> = _subscriptionInfo.asStateFlow()

    // Room Persistent Activation Records
    val activationRecordsFlow: StateFlow<List<ActivationRecord>> = db.activationDao().getAllActivations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Bind the SUNMI SDK printer service lifecycle initially
        printEngine.bindService(application)

        // Schedule 15-minute background WorkManager IRD CBMS sync
        try {
            IrdSyncWorker.schedulePeriodicSync(application)
        } catch (e: Exception) {
            Log.e("ParkingViewModel", "Failed to schedule WorkManager", e)
        }

        // Heartbeat check and MDM config sync on launch
        viewModelScope.launch {
            try {
                subscriptionManager.performHeartbeatCheck()
                refreshSubscriptionInfo()
                fetchMdmConfigFromCloud()
            } catch (e: Exception) {
                Log.e("ParkingViewModel", "Startup cloud sync error", e)
            }
        }
    }

    fun loginOperator(pin: String): Boolean {
        if (pin == "1234" || pin == "8888" || pin == "9999" || pin.length >= 4) {
            val opId = "OP-" + pin.takeLast(4)
            _activeOperatorId.value = opId
            _isOperatorLoggedIn.value = true
            prefs.edit().putString("active_operator_id", opId).apply()
            _printStatusMessage.value = "Operator $opId Logged In Successfully"
            return true
        }
        _printStatusMessage.value = "Invalid Operator PIN! Use 1234 or 8888."
        return false
    }

    fun logoutOperator() {
        _isOperatorLoggedIn.value = false
        _printStatusMessage.value = "Operator Logged Out"
    }

    fun triggerEmergencyBarrierOpen(reason: String) {
        val opId = _activeOperatorId.value
        barrierRelayManager.manualEmergencyBarrierOpen(
            operatorId = opId,
            reason = reason,
            durationSec = _gateBarrierDurationSec.value
        )
        _printStatusMessage.value = "EMERGENCY: Gate Barrier Pulsed Open for ${_gateBarrierDurationSec.value}s by $opId ($reason)"
    }

    fun checkPrinterStatusMessage(): String {
        return printEngine.checkPrinterStatus()
    }

    fun refreshSubscriptionInfo() {
        _subscriptionInfo.value = subscriptionManager.getSubscriptionInfo()
    }

    suspend fun fetchMdmConfigFromCloud() {
        val deviceId = ParkSathiNetworkClient.getDeviceId(getApplication())
        val config = ParkSathiNetworkClient.safeGetMdmConfig(deviceId)
        if (config.success) {
            config.tariffs?.bike?.hourlyRateNpr?.let { _bikeRate.value = it.toFloat() } ?: config.bikeRate?.let { _bikeRate.value = it.toFloat() }
            config.tariffs?.car?.hourlyRateNpr?.let { _carRate.value = it.toFloat() } ?: config.carRate?.let { _carRate.value = it.toFloat() }
            config.tariffs?.truck?.hourlyRateNpr?.let { _truckRate.value = it.toFloat() } ?: config.truckRate?.let { _truckRate.value = it.toFloat() }
            config.freeMinutes?.let { _freeMinutes.value = it }
            val grace = config.gracePeriodMinutes ?: config.graceMinutes
            grace?.let { _graceMinutes.value = it }
            config.tariffs?.car?.dailyCapNpr?.let { _dailyMaxCap.value = it.toFloat() } ?: config.dailyMaxCap?.let { _dailyMaxCap.value = it.toFloat() }
        }
    }

    fun activateSaaSKey(key: String): Pair<Boolean, String> {
        val result = subscriptionManager.activateLicenseKey(key)
        refreshSubscriptionInfo()
        return result
    }

    suspend fun activateSaaSKeyOnline(key: String): Pair<Boolean, String> {
        val result = subscriptionManager.verifyLicenseOnline(key)
        refreshSubscriptionInfo()
        return result
    }

    fun renewSaaSPlan(plan: SubscriptionPlan): Pair<Boolean, String> {
        val key = when (plan) {
            SubscriptionPlan.MONTHLY -> "PARKSATHI-MONTHLY-${System.currentTimeMillis().toString().takeLast(4)}"
            SubscriptionPlan.YEARLY -> "PARKSATHI-YEARLY-${System.currentTimeMillis().toString().takeLast(4)}"
            else -> "PARKSATHI-TRIAL-14D"
        }
        val result = subscriptionManager.applySubscriptionPlan(plan, key)
        refreshSubscriptionInfo()
        return result
    }

    // Language switch state: English / नेपाली (Nepali)
    private val _isNepaliLanguage = MutableStateFlow(prefs.getBoolean("is_nepali_lang", false))
    val isNepaliLanguage: StateFlow<Boolean> = _isNepaliLanguage.asStateFlow()

    fun setLanguageNepali(isNepali: Boolean) {
        _isNepaliLanguage.value = isNepali
        prefs.edit().putBoolean("is_nepali_lang", isNepali).apply()
    }

    // Vehicle Hourly Rates
    private val _bikeRate = MutableStateFlow(prefs.getFloat("rate_bike", 20f))
    val bikeRate: StateFlow<Float> = _bikeRate.asStateFlow()

    private val _carRate = MutableStateFlow(prefs.getFloat("rate_car", 50f))
    val carRate: StateFlow<Float> = _carRate.asStateFlow()

    private val _truckRate = MutableStateFlow(prefs.getFloat("rate_truck", 100f))
    val truckRate: StateFlow<Float> = _truckRate.asStateFlow()

    // Tariff Tier Configurations
    private val _freeMinutes = MutableStateFlow(prefs.getInt("free_mins", 15))
    val freeMinutes: StateFlow<Int> = _freeMinutes.asStateFlow()

    private val _graceMinutes = MutableStateFlow(prefs.getInt("grace_mins", 5))
    val graceMinutes: StateFlow<Int> = _graceMinutes.asStateFlow()

    private val _dailyMaxCap = MutableStateFlow(prefs.getFloat("daily_cap", 300f))
    val dailyMaxCap: StateFlow<Float> = _dailyMaxCap.asStateFlow()

    // IRD Nepal Company Details
    private val _companyName = MutableStateFlow(
        prefs.getString("company_name", NepalIrdInvoiceHelper.DEFAULT_COMPANY_NAME) ?: NepalIrdInvoiceHelper.DEFAULT_COMPANY_NAME
    )
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    private val _companyPan = MutableStateFlow(
        prefs.getString("company_pan", NepalIrdInvoiceHelper.DEFAULT_PAN_NUMBER) ?: NepalIrdInvoiceHelper.DEFAULT_PAN_NUMBER
    )
    val companyPan: StateFlow<String> = _companyPan.asStateFlow()

    // SaaS Multi-Gate POS Config
    private val _gateId = MutableStateFlow(prefs.getString("gate_id", "GATE-01") ?: "GATE-01")
    val gateId: StateFlow<String> = _gateId.asStateFlow()

    private val _qrCodePrefix = MutableStateFlow(prefs.getString("qr_prefix", "") ?: "")
    val qrCodePrefix: StateFlow<String> = _qrCodePrefix.asStateFlow()

    private val _printFooterText = MutableStateFlow(
        prefs.getString("print_footer", "Scan to Checkout\nKeep ticket safe!") ?: "Scan to Checkout\nKeep ticket safe!"
    )
    val printFooterText: StateFlow<String> = _printFooterText.asStateFlow()

    val unsyncedCountFlow: StateFlow<Int> = db.parkingTicketDao().getUnsyncedCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Form inputs for Check-In
    private val _vehicleNumber = MutableStateFlow("")
    val vehicleNumber: StateFlow<String> = _vehicleNumber.asStateFlow()

    private val _selectedVehicleType = MutableStateFlow(VehicleType.CAR)
    val selectedVehicleType: StateFlow<VehicleType> = _selectedVehicleType.asStateFlow()

    private val _attendantId = MutableStateFlow("ATT-8842")
    val attendantId: StateFlow<String> = _attendantId.asStateFlow()

    // Form inputs for Check-Out
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCheckoutTicket = MutableStateFlow<ParkingTicket?>(null)
    val selectedCheckoutTicket: StateFlow<ParkingTicket?> = _selectedCheckoutTicket.asStateFlow()

    // Settings for Hardware Connections
    private val _useBluetooth = MutableStateFlow(false)
    val useBluetooth: StateFlow<Boolean> = _useBluetooth.asStateFlow()

    private val _bluetoothMacAddress = MutableStateFlow("00:11:22:33:44:55")
    val bluetoothMacAddress: StateFlow<String> = _bluetoothMacAddress.asStateFlow()

    // Interactive States
    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    private val _printStatusMessage = MutableStateFlow<String?>(null)
    val printStatusMessage: StateFlow<String?> = _printStatusMessage.asStateFlow()

    private val _isScanActive = MutableStateFlow(false)
    val isScanActive: StateFlow<Boolean> = _isScanActive.asStateFlow()

    init {
        // Bind the SUNMI SDK printer service lifecycle initially
        printEngine.bindService(application)
    }

    override fun onCleared() {
        super.onCleared()
        // Graceful unbind
        printEngine.unbindService(getApplication())
    }

    // Daily Summary flow relative to midnight today
    val startOfDayMillis: Long
        get() {
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

    val dailySummaryFlow: StateFlow<DailySummary> = repository.getDailySummary(startOfDayMillis)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DailySummary(0.0, 0, 0)
        )

    // Active tickets based on search query
    val searchResultsFlow: StateFlow<List<ParkingTicket>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            repository.getActiveTicketsFlow(query)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showOnlyCurrentSession = MutableStateFlow(true)
    val showOnlyCurrentSession: StateFlow<Boolean> = _showOnlyCurrentSession.asStateFlow()

    fun setShowOnlyCurrentSession(show: Boolean) {
        _showOnlyCurrentSession.value = show
    }

    // All tickets (detailed history list)
    val allTicketsFlow: StateFlow<List<ParkingTicket>> = combine(
        repository.getAllTickets(),
        _showOnlyCurrentSession
    ) { tickets, showOnlyCurrent ->
        if (showOnlyCurrent) {
            val minTime = startOfDayMillis
            tickets.filter { it.checkInTime >= minTime }
        } else {
            tickets
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Completed tickets (payment records list)
    val completedTicketsFlow: StateFlow<List<ParkingTicket>> = combine(
        repository.getCompletedTickets(),
        _showOnlyCurrentSession
    ) { tickets, showOnlyCurrent ->
        if (showOnlyCurrent) {
            val minTime = startOfDayMillis
            tickets.filter { (it.checkOutTime ?: 0L) >= minTime }
        } else {
            tickets
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateRates(bike: Float, car: Float, truck: Float) {
        _bikeRate.value = bike
        _carRate.value = car
        _truckRate.value = truck
        prefs.edit()
            .putFloat("rate_bike", bike)
            .putFloat("rate_car", car)
            .putFloat("rate_truck", truck)
            .apply()
    }

    fun updateQrPrefix(prefix: String) {
        _qrCodePrefix.value = prefix
        prefs.edit().putString("qr_prefix", prefix).apply()
    }

    fun updatePrintFooterText(text: String) {
        _printFooterText.value = text
        prefs.edit().putString("print_footer", text).apply()
    }

    fun updateVehicleNumber(number: String) {
        val formatted = number.uppercase().filter { it.isLetterOrDigit() }.take(12)
        _vehicleNumber.value = formatted
    }

    fun updateVehicleNumberFromScan(plate: String) {
        val cleanPlate = plate.uppercase().trim()
        _vehicleNumber.value = cleanPlate
        _printStatusMessage.value = "Vehicle Plate Auto-Detected: $cleanPlate"
    }

    fun handleScannedQrCode(scannedData: String) {
        val cleanCode = scannedData.trim()
        if (cleanCode.isBlank()) return
        
        viewModelScope.launch {
            _searchQuery.value = cleanCode
            val activeTickets = searchResultsFlow.value
            val match = activeTickets.firstOrNull { 
                it.ticketId.equals(cleanCode, ignoreCase = true) || 
                it.irdInvoiceNo?.equals(cleanCode, ignoreCase = true) == true ||
                it.vehicleNumber.equals(cleanCode, ignoreCase = true) ||
                cleanCode.contains(it.ticketId, ignoreCase = true)
            } ?: repository.getAllTickets().firstOrNull()?.firstOrNull {
                it.ticketId.equals(cleanCode, ignoreCase = true) || 
                it.irdInvoiceNo?.equals(cleanCode, ignoreCase = true) == true ||
                it.vehicleNumber.equals(cleanCode, ignoreCase = true)
            }

            if (match != null) {
                selectCheckoutTicket(match)
                _printStatusMessage.value = "Ticket Matched: ${match.vehicleNumber} (${match.ticketId.take(6)})"
            } else {
                _printStatusMessage.value = "No active ticket found for code: $cleanCode"
            }
        }
    }

    fun updateVehicleType(type: VehicleType) {
        _selectedVehicleType.value = type
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCheckoutTicket(ticket: ParkingTicket?) {
        _selectedCheckoutTicket.value = ticket
    }

    fun setUseBluetooth(use: Boolean) {
        _useBluetooth.value = use
    }

    fun updateBluetoothMacAddress(mac: String) {
        _bluetoothMacAddress.value = mac
    }

    fun clearStatusMessage() {
        _printStatusMessage.value = null
    }

    fun startScanning() {
        _isScanActive.value = true
    }

    fun stopScanning() {
        _isScanActive.value = false
    }

    fun onQrCodeScanned(ticketId: String) {
        _isScanActive.value = false
        viewModelScope.launch {
            val ticket = repository.getTicketById(ticketId)
            if (ticket != null) {
                if (ticket.status == TicketStatus.ACTIVE) {
                    _selectedCheckoutTicket.value = ticket
                    _printStatusMessage.value = "Ticket ${ticketId.take(8)} identified!"
                } else {
                    _printStatusMessage.value = "Ticket ${ticketId.take(8)} is already completed."
                }
            } else {
                _printStatusMessage.value = "Ticket ID not found."
            }
        }
    }

    fun issueTicket() {
        val currentSub = _subscriptionInfo.value
        if (currentSub.isExpired) {
            _printStatusMessage.value = "Subscription Expired! Renew Monthly/Yearly SaaS plan in SaaS Portal to issue tickets."
            return
        }

        val number = _vehicleNumber.value
        if (number.length < 3) {
            _printStatusMessage.value = "Error: Vehicle Number too short"
            return
        }

        val type = _selectedVehicleType.value
        val rate = getRateForType(type)
        val shortId = UUID.randomUUID().toString().replace("-", "").take(10).uppercase()

        val ticket = ParkingTicket(
            ticketId = shortId,
            vehicleNumber = number,
            vehicleType = type,
            checkInTime = System.currentTimeMillis(),
            ratePerHour = rate,
            attendantId = _attendantId.value
        )

        _isPrinting.value = true
        _printStatusMessage.value = "Issuing & Printing Ticket..."

        viewModelScope.launch {
            try {
                // Insert into local DB
                repository.insert(ticket)

                // Dispatch printing
                val result = printEngine.printCheckInTicket(
                    context = getApplication(),
                    ticket = ticket,
                    useBluetooth = _useBluetooth.value,
                    bluetoothMacAddress = _bluetoothMacAddress.value,
                    qrPrefix = _qrCodePrefix.value,
                    footerText = _printFooterText.value
                )

                if (result) {
                    _printStatusMessage.value = "Ticket #${shortId} Printed Successfully"
                } else {
                    _printStatusMessage.value = "Saved to Database (Printer offline)"
                }

                // Reset forms
                _vehicleNumber.value = ""
            } catch (e: Throwable) {
                Log.e("ParkingViewModel", "Failed to issue ticket", e)
                _printStatusMessage.value = "Database Error: ${e.localizedMessage}"
            } finally {
                _isPrinting.value = false
            }
        }
    }

    fun updateTariffConfig(freeMins: Int, graceMins: Int, dailyCap: Float) {
        _freeMinutes.value = freeMins
        _graceMinutes.value = graceMins
        _dailyMaxCap.value = dailyCap
        prefs.edit()
            .putInt("free_mins", freeMins)
            .putInt("grace_mins", graceMins)
            .putFloat("daily_cap", dailyCap)
            .apply()
    }

    fun updateIrdDetails(companyName: String, panNumber: String, gateId: String) {
        _companyName.value = companyName
        _companyPan.value = panNumber
        _gateId.value = gateId
        prefs.edit()
            .putString("company_name", companyName)
            .putString("company_pan", panNumber)
            .putString("gate_id", gateId)
            .apply()
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            syncEngine.performSync()
        }
    }

    fun checkoutTicket(hasCafeSeal: Boolean = false, paymentMethod: String = "CASH") {
        val ticket = _selectedCheckoutTicket.value ?: return
        val checkOutTime = System.currentTimeMillis()

        _printStatusMessage.value = "Processing $paymentMethod Checkout Payment..."
        viewModelScope.launch {
            try {
                val tariffConfig = NepalTariffConfig(
                    freeMinutes = _freeMinutes.value,
                    gracePeriodMinutes = _graceMinutes.value,
                    dailyMaxCap = _dailyMaxCap.value.toDouble()
                )

                val updatedTicket = repository.checkoutTicket(
                    ticketId = ticket.ticketId,
                    checkOutTime = checkOutTime,
                    hasCafeSeal = hasCafeSeal,
                    paymentMethod = paymentMethod,
                    tariffConfig = tariffConfig
                )

                if (updatedTicket != null) {
                    val msgSuff = if (hasCafeSeal) " (Cafe Free Pass)" else ""
                    _printStatusMessage.value = "Checkout completed [$paymentMethod]! Total: NPR ${"%.2f".format(updatedTicket.totalAmount)}$msgSuff"
                    
                    // 1. Trigger automated Boom Barrier Relay Signal
                    barrierRelayManager.triggerBarrierOpen(
                        actionType = if (paymentMethod.contains("FONEPAY")) "AUTOMATED_FONEPAY" else "AUTOMATED_CASH",
                        operatorId = _activeOperatorId.value,
                        ticketId = updatedTicket.ticketId,
                        openDurationSec = _gateBarrierDurationSec.value
                    )

                    // 2. Print IRD Fiscal Invoice
                    printEngine.printIrdCheckoutReceipt(
                        context = getApplication(),
                        ticket = updatedTicket,
                        useBluetooth = _useBluetooth.value,
                        bluetoothMacAddress = _bluetoothMacAddress.value,
                        companyName = _companyName.value,
                        companyPan = _companyPan.value
                    )

                    // 3. IRD CBMS Fiscal Tax Sync to Park Sathi Cloud API
                    val deviceId = ParkSathiNetworkClient.getDeviceId(getApplication())
                    val cbmsResponse = ParkSathiNetworkClient.safeSyncIrdCbms(
                        fiscalYear = "2082/83",
                        billNo = "PS-8283-${updatedTicket.ticketId.take(5)}",
                        customerPan = _companyPan.value,
                        totalAmountNpr = updatedTicket.totalAmount ?: 0.0,
                        taxableAmountNpr = updatedTicket.netAmount ?: 0.0,
                        vatAmountNpr = updatedTicket.vatAmount ?: 0.0,
                        paymentMethod = paymentMethod,
                        deviceId = deviceId
                    )
                    Log.d("ParkingViewModel", "IRD CBMS Sync Result: ${cbmsResponse.message}")

                    _selectedCheckoutTicket.value = null
                    _searchQuery.value = ""
                } else {
                    _printStatusMessage.value = "Error processing checkout."
                }
            } catch (e: Throwable) {
                _printStatusMessage.value = "Payment Save Error: ${e.localizedMessage}"
            }
        }
    }

    fun getRateForType(type: VehicleType): Double {
        return when (type) {
            VehicleType.BIKE -> _bikeRate.value.toDouble()
            VehicleType.CAR -> _carRate.value.toDouble()
            VehicleType.TRUCK -> _truckRate.value.toDouble()
        }
    }

    fun printZReport() {
        viewModelScope.launch {
            _isPrinting.value = true
            _printStatusMessage.value = "Generating End-of-Day Z-Report..."
            try {
                val completed = completedTicketsFlow.value
                val allTickets = allTicketsFlow.value
                val activeCount = allTickets.count { it.status == TicketStatus.ACTIVE }
                val completedCount = completed.size
                
                var cashTotal = 0.0
                var fonepayTotal = 0.0
                var totalVat = 0.0

                for (ticket in completed) {
                    val amt = ticket.totalAmount ?: 0.0
                    val vat = ticket.vatAmount ?: (amt * 0.13 / 1.13)
                    totalVat += vat
                    if (ticket.paymentMethod?.uppercase()?.contains("FONEPAY") == true) {
                        fonepayTotal += amt
                    } else {
                        cashTotal += amt
                    }
                }

                val grandTotal = cashTotal + fonepayTotal
                val unsynced = unsyncedCountFlow.value

                val result = printEngine.printZReport(
                    context = getApplication(),
                    operatorId = _activeOperatorId.value,
                    merchantName = _companyName.value,
                    panNumber = _companyPan.value,
                    totalTickets = allTickets.size,
                    activeTickets = activeCount,
                    completedTickets = completedCount,
                    cashCollections = cashTotal,
                    fonepayCollections = fonepayTotal,
                    totalVatCollected = totalVat,
                    grandTotalNpr = grandTotal,
                    unsyncedCount = unsynced
                )

                if (result) {
                    _printStatusMessage.value = "Z-Report Thermal Print Completed Successfully!"
                } else {
                    _printStatusMessage.value = "Printed to Log (SUNMI Printer offline/disconnected)"
                }
            } catch (e: Exception) {
                Log.e("ParkingViewModel", "Failed to print Z-Report", e)
                _printStatusMessage.value = "Z-Report Print Error: ${e.localizedMessage}"
            } finally {
                _isPrinting.value = false
            }
        }
    }
}
