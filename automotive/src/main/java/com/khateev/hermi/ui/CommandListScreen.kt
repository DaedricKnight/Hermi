package com.khateev.hermi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
        // Header с информацией о версии
        VersionHeaderCard(isPro = isPro, onUpgradeClick = onUpgradeClick)

        Spacer(modifier = Modifier.height(16.dp))

        // Список команд
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
        // ===== FREE ВЕРСИЯ (Навигация) =====
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

        // ===== PRO ВЕРСИЯ (Статус машины) =====
        CommandUIModel(
            name = "Vehicle Status",
            description = "Get complete vehicle diagnostics",
            command = Command.VehicleStatus,
            isPro = true,
            isAvailable = "VehicleStatus" in availableCommands
        ),
        CommandUIModel(
            name = "Check Engine",
            description = "Check engine status and diagnostic codes",
            command = Command.CheckEngine,
            isPro = true,
            isAvailable = "CheckEngine" in availableCommands
        ),
        CommandUIModel(
            name = "Check Tires",
            description = "Monitor tire pressure and conditions",
            command = Command.CheckTires,
            isPro = true,
            isAvailable = "CheckTires" in availableCommands
        ),
        CommandUIModel(
            name = "Check Fuel Level",
            description = "View fuel level and fuel status",
            command = Command.CheckFuelLevel,
            isPro = true,
            isAvailable = "CheckFuelLevel" in availableCommands
        ),
        CommandUIModel(
            name = "Check Battery",
            description = "Monitor battery charge status",
            command = Command.CheckBatteryLevel,
            isPro = true,
            isAvailable = "CheckBatteryLevel" in availableCommands
        ),

        // ===== PRO ВЕРСИЯ (Управление) =====
        CommandUIModel(
            name = "Lock Doors",
            description = "Lock all vehicle doors",
            command = Command.LockDoors,
            isPro = true,
            isAvailable = "LockDoors" in availableCommands
        ),
        CommandUIModel(
            name = "Unlock Doors",
            description = "Unlock all vehicle doors",
            command = Command.UnlockDoors,
            isPro = true,
            isAvailable = "UnlockDoors" in availableCommands
        ),
        CommandUIModel(
            name = "Turn On Lights",
            description = "Turn on vehicle headlights",
            command = Command.TurnOnLights,
            isPro = true,
            isAvailable = "TurnOnLights" in availableCommands
        ),
        CommandUIModel(
            name = "Turn Off Lights",
            description = "Turn off vehicle headlights",
            command = Command.TurnOffLights,
            isPro = true,
            isAvailable = "TurnOffLights" in availableCommands
        ),

        // ===== PRO ВЕРСИЯ (Информация) =====
        CommandUIModel(
            name = "Get Range",
            description = "Check remaining driving range",
            command = Command.GetRange,
            isPro = true,
            isAvailable = "GetRange" in availableCommands
        ),
        CommandUIModel(
            name = "Get Odometer",
            description = "View total vehicle mileage",
            command = Command.GetOdometer,
            isPro = true,
            isAvailable = "GetOdometer" in availableCommands
        ),
        CommandUIModel(
            name = "Get Speed",
            description = "Check current driving speed",
            command = Command.GetSpeed,
            isPro = true,
            isAvailable = "GetSpeed" in availableCommands
        ),
        CommandUIModel(
            name = "Get Temperature",
            description = "Get outside temperature reading",
            command = Command.GetOutsideTemp,
            isPro = true,
            isAvailable = "GetOutsideTemp" in availableCommands
        )
    )
}

/**
 * Preview для разработки
 * TODO: Add proper preview implementation
 */
// @androidx.compose.ui.tooling.preview.Preview
// @Composable
// private fun CommandListScreenPreview() {
    // Здесь можно добавить preview для тестирования UI
// }
