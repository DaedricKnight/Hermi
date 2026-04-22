package com.khateev.hermi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khateev.hermi.shared.interfaces.VersionManager
import com.khateev.hermi.shared.model.Command
import com.khateev.hermi.shared.model.CommandAccessControl

/**
 * Data class для отображения информации о команде
 */
data class CommandUIModel(
    val name: String,
    val description: String,
    val command: Command,
    val isPro: Boolean,
    val isAvailable: Boolean
)

/**
 * Экран со списком доступных команд
 */
@Composable
fun CommandListScreen(
    versionManager: VersionManager,
    onCommandClick: (Command) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPro by versionManager.isProVersion.collectAsState(initial = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        VersionHeaderCard(isPro = isPro, onUpgradeClick = onUpgradeClick)

        Spacer(modifier = Modifier.height(16.dp))

        CommandsList(
            isPro = isPro,
            onCommandClick = onCommandClick
        )
    }
}

/**
 * Карточка с информацией о версии приложения
 */
@Composable
private fun VersionHeaderCard(
    isPro: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) Color(0xFF4CAF50) else Color(0xFF2196F3)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isPro) "PRO VERSION" else "FREE VERSION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isPro) "✓ All features unlocked" else "Limited access",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                if (!isPro) {
                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF2196F3)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Upgrade",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Список команд
 */
@Composable
private fun CommandsList(
    isPro: Boolean,
    onCommandClick: (Command) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = getCommandsUIModels(isPro)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // FREE ВЕРСИЯ
        items(
            items = commands.filter { !it.isPro },
            key = { it.name }
        ) { commandUI ->
            CommandCard(
                command = commandUI,
                onCommandClick = { onCommandClick(commandUI.command) },
                isPro = isPro
            )
        }

        // PRO ВЕРСИЯ (если есть доступ)
        if (commands.any { it.isPro }) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ProSectionHeader()
            }

            items(
                items = commands.filter { it.isPro },
                key = { it.name }
            ) { commandUI ->
                CommandCard(
                    command = commandUI,
                    onCommandClick = { onCommandClick(commandUI.command) },
                    isPro = isPro
                )
            }
        }
    }
}

/**
 * Заголовок раздела Pro команд
 */
@Composable
private fun ProSectionHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = Color(0xFFBDBDBD)
        )
        Text(
            text = " 💎 PRO FEATURES ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF666666),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = Color(0xFFBDBDBD)
        )
    }
}

/**
 * Карточка с командой
 */
@Composable
private fun CommandCard(
    command: CommandUIModel,
    onCommandClick: () -> Unit,
    isPro: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = command.isAvailable) {
                if (command.isAvailable) onCommandClick()
            },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !command.isAvailable -> Color(0xFFEEEEEE)
                command.isPro && isPro -> Color(0xFFF0F7FF)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (command.isAvailable) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = command.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (command.isAvailable) Color.Black else Color.Gray
                    )

                    if (command.isPro) {
                        Text(
                            text = " 💎",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Text(
                    text = command.description,
                    fontSize = 13.sp,
                    color = if (command.isAvailable) Color(0xFF666666) else Color.LightGray,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (!command.isAvailable) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Pro feature",
                    tint = Color.Gray
                )
            }
        }
    }
}

/**
 * Получить список команд для отображения
 */
private fun getCommandsUIModels(isPro: Boolean): List<CommandUIModel> {
    val availableCommands = CommandAccessControl.getAvailableCommands(isPro)

    return listOf(
        // ===== FREE ВЕРСИЯ (5 команд - Навигация) =====
        CommandUIModel(
            name = "Find Food",
            description = "Search for nearby restaurants and cafes",
            command = Command.FindFood,
            isPro = false,
            isAvailable = "FindFood" in availableCommands
        ),
        CommandUIModel(
            name = "Find Gas Station",
            description = "Locate nearby gas/petrol stations",
            command = Command.FindGasStation,
            isPro = false,
            isAvailable = "FindGasStation" in availableCommands
        ),
        CommandUIModel(
            name = "Find Charging Station",
            description = "Find EV charging stations nearby",
            command = Command.FindChargingStation,
            isPro = false,
            isAvailable = "FindChargingStation" in availableCommands
        ),
        CommandUIModel(
            name = "Find Parking",
            description = "Search for available parking spots",
            command = Command.FindParking,
            isPro = false,
            isAvailable = "FindParking" in availableCommands
        ),
        CommandUIModel(
            name = "Find Rest Stop",
            description = "Find roadside rest areas and travel stops",
            command = Command.FindRestStop,
            isPro = false,
            isAvailable = "FindRestStop" in availableCommands
        ),

        // ===== PRO ВЕРСИЯ: ENGINE & POWERTRAIN (6 команд) =====
        CommandUIModel(
            name = "Engine Status",
            description = "Check engine diagnostics and warnings",
            command = Command.CheckEngineStatus,
            isPro = true,
            isAvailable = "CheckEngineStatus" in availableCommands
        ),
        CommandUIModel(
            name = "Oil Level",
            description = "Monitor engine oil level and quality",
            command = Command.CheckOilLevel,
            isPro = true,
            isAvailable = "CheckOilLevel" in availableCommands
        ),
        CommandUIModel(
            name = "Transmission Status",
            description = "Monitor transmission health and performance",
            command = Command.CheckTransmissionStatus,
            isPro = true,
            isAvailable = "CheckTransmissionStatus" in availableCommands
        ),
        CommandUIModel(
            name = "Brake Pad Wear",
            description = "Check brake pad wear levels",
            command = Command.CheckBrakePadWear,
            isPro = true,
            isAvailable = "CheckBrakePadWear" in availableCommands
        ),
        CommandUIModel(
            name = "Spark Plugs",
            description = "Monitor spark plug condition",
            command = Command.CheckSparkPlugCondition,
            isPro = true,
            isAvailable = "CheckSparkPlugCondition" in availableCommands
        ),
        CommandUIModel(
            name = "RPM Monitor",
            description = "Monitor engine RPM in real-time",
            command = Command.CheckRpm,
            isPro = true,
            isAvailable = "CheckRpm" in availableCommands
        ),

        // ===== PRO ВЕРСИЯ: FUEL & BATTERY (5 команд) =====
        CommandUIModel(
            name = "Fuel Level",
            description = "Check fuel tank level and consumption",
            command = Command.CheckFuelLevel,
            isPro = true,
            isAvailable = "CheckFuelLevel" in availableCommands
        ),
        CommandUIModel(
            name = "Fuel Consumption",
            description = "Monitor fuel consumption and economy",
            command = Command.CheckFuelConsumption,
            isPro = true,
            isAvailable = "CheckFuelConsumption" in availableCommands
        ),
        CommandUIModel(
            name = "Battery Voltage",
            description = "Monitor battery voltage levels",
            command = Command.CheckBatteryVoltage,
            isPro = true,
            isAvailable = "CheckBatteryVoltage" in availableCommands
        ),
        CommandUIModel(
            name = "Battery Health",
            description = "Check battery health and temperature",
            command = Command.CheckBatteryHealth,
            isPro = true,
            isAvailable = "CheckBatteryHealth" in availableCommands
        ),
        CommandUIModel(
            name = "EV Status",
            description = "Check EV battery charge, port connection, and range",
            command = Command.CheckEvStatus,
            isPro = true,
            isAvailable = "CheckEvStatus" in availableCommands
        ),

        // ===== PRO ВЕРСИЯ: TIRES & SUSPENSION (5 команд) =====
        CommandUIModel(
            name = "Tire Status",
            description = "Monitor tire pressure and condition",
            command = Command.CheckTireStatus,
            isPro = true,
            isAvailable = "CheckTireStatus" in availableCommands
        ),
        CommandUIModel(
            name = "Tire Wear",
            description = "Check tire tread wear levels",
            command = Command.CheckTireWear,
            isPro = true,
            isAvailable = "CheckTireWear" in availableCommands
        ),
        CommandUIModel(
            name = "Suspension Health",
            description = "Monitor suspension system status",
            command = Command.CheckSuspensionHealth,
            isPro = true,
            isAvailable = "CheckSuspensionHealth" in availableCommands
        ),
        CommandUIModel(
            name = "Wheel Alignment",
            description = "Check wheel alignment status",
            command = Command.CheckWheelAlignment,
            isPro = true,
            isAvailable = "CheckWheelAlignment" in availableCommands
        ),
        CommandUIModel(
            name = "Steering Status",
            description = "Monitor steering system condition",
            command = Command.CheckSteeringStatus,
            isPro = true,
            isAvailable = "CheckSteeringStatus" in availableCommands
        ),

        // ===== PRO ВЕРСИЯ: FLUIDS & FILTERS (4 команды) =====
        CommandUIModel(
            name = "Transmission Fluid",
            description = "Monitor transmission fluid status",
            command = Command.CheckTransmissionFluid,
            isPro = true,
            isAvailable = "CheckTransmissionFluid" in availableCommands
        ),
        CommandUIModel(
            name = "Brake Fluid",
            description = "Check brake fluid level and condition",
            command = Command.CheckBrakeFluid,
            isPro = true,
            isAvailable = "CheckBrakeFluid" in availableCommands
        ),
        CommandUIModel(
            name = "Coolant Level",
            description = "Monitor engine coolant level",
            command = Command.CheckCoolantLevel,
            isPro = true,
            isAvailable = "CheckCoolantLevel" in availableCommands
        ),
        CommandUIModel(
            name = "Air Filters",
            description = "Check engine and cabin air filters",
            command = Command.CheckAirFilter,
            isPro = true,
            isAvailable = "CheckAirFilter" in availableCommands
        ),

        // ===== PRO ВЕРСИЯ: DRIVING STATS (2 команды) =====
        CommandUIModel(
            name = "Odometer",
            description = "View total vehicle mileage",
            command = Command.CheckOdometer,
            isPro = true,
            isAvailable = "CheckOdometer" in availableCommands
        ),
        CommandUIModel(
            name = "Driving Range",
            description = "Check remaining fuel/battery range",
            command = Command.CheckDrivingRange,
            isPro = true,
            isAvailable = "CheckDrivingRange" in availableCommands
        ),

        // ===== PRO ВЕРСИЯ: ENVIRONMENTAL (1 команда) =====
        CommandUIModel(
            name = "Outside Temperature",
            description = "Get current outside temperature",
            command = Command.CheckOutsideTemperature,
            isPro = true,
            isAvailable = "CheckOutsideTemperature" in availableCommands
        ),

        // ===== PRO ВЕРСИЯ: SMART FINDING (2 команды) =====
        CommandUIModel(
            name = "Find Service",
            description = "Locate auto repair and service stations",
            command = Command.FindService,
            isPro = true,
            isAvailable = "FindService" in availableCommands
        ),
        CommandUIModel(
            name = "Find Car Wash",
            description = "Search for nearby car wash facilities",
            command = Command.FindCarWash,
            isPro = true,
            isAvailable = "FindCarWash" in availableCommands
        )
    )
}

/**
 * Mock VersionManager for Preview
 */
class MockVersionManager : VersionManager {
    override val isProVersion: kotlinx.coroutines.flow.Flow<Boolean>
        get() = kotlinx.coroutines.flow.flowOf(false) // Mock as FREE version

    override fun isProNow(): Boolean = false

    override suspend fun setProVersion(isPro: Boolean) {
        // Mock implementation
    }

    override suspend fun isSubscriptionActive(): Boolean = false
}

@Preview
@Composable
fun CommandListScreenPreview() {
    CommandListScreen(
        versionManager = MockVersionManager(),
        onCommandClick = { _ -> },
        onUpgradeClick = {}
    )
}
