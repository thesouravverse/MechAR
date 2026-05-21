package com.thesouravverse.mechar.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
    val mechPlaced: Boolean = false,
    val mechHp: Int = MAX_HP,
    val score: Int = 0,
    val cubeSizeCm: Int = DEFAULT_SIZE_CM,
    val sizeLocked: Boolean = false,
    val hint: String = "Adjust size with - / + . Tap floor to deploy."
) {
    companion object {
        const val MAX_HP = 100
        const val DAMAGE_PER_HIT = 10
        const val DEFAULT_SIZE_CM = 15
        const val MIN_SIZE_CM = 5
        const val MAX_SIZE_CM = 200
        const val SIZE_STEP_CM = 5
    }
}

class GameViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    fun onMechPlaced() {
        _state.update {
            it.copy(
                mechPlaced = true,
                mechHp = GameUiState.MAX_HP,
                hint = if (it.sizeLocked) "TAP THE MECH TO FIRE"
                       else "Adjust size, then tap LOCK. Tap mech to fire."
            )
        }
    }

    /** Returns true if this shot killed the mech. */
    fun shoot(): Boolean {
        var killed = false
        _state.update { s ->
            if (!s.mechPlaced) return@update s
            val newHp = (s.mechHp - GameUiState.DAMAGE_PER_HIT).coerceAtLeast(0)
            if (newHp == 0) {
                killed = true
                s.copy(
                    mechPlaced = false,
                    mechHp = 0,
                    score = s.score + 1,
                    hint = "MECH DOWN. Tap floor to deploy another."
                )
            } else {
                s.copy(mechHp = newHp)
            }
        }
        return killed
    }

    fun increaseSize() {
        _state.update { s ->
            if (s.sizeLocked) return@update s
            s.copy(cubeSizeCm = (s.cubeSizeCm + GameUiState.SIZE_STEP_CM)
                .coerceAtMost(GameUiState.MAX_SIZE_CM))
        }
    }

    fun decreaseSize() {
        _state.update { s ->
            if (s.sizeLocked) return@update s
            s.copy(cubeSizeCm = (s.cubeSizeCm - GameUiState.SIZE_STEP_CM)
                .coerceAtLeast(GameUiState.MIN_SIZE_CM))
        }
    }

    fun toggleLock() {
        _state.update { s ->
            val locked = !s.sizeLocked
            s.copy(
                sizeLocked = locked,
                hint = when {
                    locked && s.mechPlaced -> "LOCKED at ${s.cubeSizeCm} cm. Tap mech to fire."
                    locked && !s.mechPlaced -> "LOCKED at ${s.cubeSizeCm} cm. Tap floor to deploy."
                    !locked && s.mechPlaced -> "UNLOCKED. Resize, then lock again."
                    else -> "Adjust size with - / + . Tap floor to deploy."
                }
            )
        }
    }

    fun reset() {
        _state.value = GameUiState()
    }
}
