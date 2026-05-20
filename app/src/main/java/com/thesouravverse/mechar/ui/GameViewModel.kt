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
    val hint: String = "Move phone around. Tap a detected floor to deploy mech."
) {
    companion object {
        const val MAX_HP = 100
        const val DAMAGE_PER_HIT = 10
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
                hint = "TAP THE MECH TO FIRE"
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

    fun reset() {
        _state.value = GameUiState()
    }
}
