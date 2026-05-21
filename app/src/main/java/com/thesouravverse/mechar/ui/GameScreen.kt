package com.thesouravverse.mechar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameScreen(
    vm: GameViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {

        // The AR view fills the screen. All Filament/ARCore logic is isolated
        // in MechARScene so this Composable stays readable.
        MechARScene(
            state = state,
            onMechPlaced = vm::onMechPlaced,
            onMechTapped = { vm.shoot() },
            modifier = Modifier.fillMaxSize()
        )

        // --- Top HUD ---
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudBadge(text = "SCORE  ${state.score}")
                if (state.mechPlaced) {
                    HpBar(hp = state.mechHp, maxHp = GameUiState.MAX_HP)
                }
            }
            Spacer(Modifier.height(12.dp))
            HudBadge(text = state.hint)
        }

        // --- Bottom size controls ---
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShapeRow(
                shape = state.shape,
                onCycle = vm::cycleShape
            )
            SizeControls(
                sizeCm = state.cubeSizeCm,
                locked = state.sizeLocked,
                onMinus = vm::decreaseSize,
                onPlus = vm::increaseSize,
                onToggleLock = vm::toggleLock
            )
        }
    }
}

@Composable
private fun HudBadge(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun HpBar(hp: Int, maxHp: Int) {
    val frac = (hp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.End) {
        Text(
            "MECH  $hp/$maxHp",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .width(160.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.55f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(10.dp)
                    .background(
                        when {
                            frac > 0.5f -> Color(0xFF22D67A)
                            frac > 0.2f -> Color(0xFFFFC857)
                            else -> Color(0xFFFF5577)
                        }
                    )
            )
        }
    }
}

@Composable
private fun ShapeRow(
    shape: MechShape,
    onCycle: () -> Unit
) {
    Button(
        onClick = onCycle,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black.copy(alpha = 0.65f)
        )
    ) {
        Text(
            "SHAPE: ${shape.label}  \u21BB",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SizeControls(
    sizeCm: Int,
    locked: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleBtn(label = "-", enabled = !locked, onClick = onMinus)
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                "${sizeCm} cm",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        CircleBtn(label = "+", enabled = !locked, onClick = onPlus)
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onToggleLock,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (locked) Color(0xFF22D67A) else Color(0xFF3469FF)
            )
        ) {
            Text(if (locked) "UNLOCK" else "LOCK", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CircleBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (enabled) Color.Black.copy(alpha = 0.65f)
                else Color.Black.copy(alpha = 0.30f)
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Text(label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PermissionGate(onRequest: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B12)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "MechAR needs camera access to see the world.",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Button(onClick = onRequest) {
                Text("Grant camera access")
            }
        }
    }
}
