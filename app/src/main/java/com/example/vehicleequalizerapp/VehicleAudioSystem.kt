package com.example.vehicleequalizerapp

import android.util.Log
import kotlin.random.Random

/**
 * Sistema de áudio veicular simulado (Etapa 2).
 *
 * Representa o processador de áudio do rádio/central multimídia: armazena o estado
 * e os níveis do equalizador, "aplica" as configurações registrando no Logcat o que
 * o hardware receberia e simula a chegada de um valor de volume pela rede CAN.
 */
class VehicleAudioSystem {

    companion object {
        private const val TAG = "VehicleAudioSystem"
        const val MIN_LEVEL = 0
        const val MAX_LEVEL = 100
    }

    // Estado do equalizador
    var isEqualizerEnabled: Boolean = false
        private set
    var bassLevel: Int = 50
        private set
    var midLevel: Int = 50
        private set
    var trebleLevel: Int = 50
        private set

    // Último volume recebido pela rede CAN simulada (0..100)
    var canVolume: Int = 30
        private set

    @Synchronized
    fun setEqualizerEnabled(enabled: Boolean) {
        isEqualizerEnabled = enabled
        Log.i(TAG, "Equalizador ${if (enabled) "LIGADO" else "DESLIGADO"}")
        applySettings()
    }

    @Synchronized
    fun setBassLevel(level: Int) {
        bassLevel = clamp(level)
        Log.d(TAG, "Nível de graves definido para $bassLevel")
    }

    @Synchronized
    fun setMidLevel(level: Int) {
        midLevel = clamp(level)
        Log.d(TAG, "Nível de médios definido para $midLevel")
    }

    @Synchronized
    fun setTrebleLevel(level: Int) {
        trebleLevel = clamp(level)
        Log.d(TAG, "Nível de agudos definido para $trebleLevel")
    }

    /**
     * Simula o envio da configuração ao hardware de áudio (ex.: quadro CAN).
     * Com o equalizador desligado, o processador de áudio é bypassado (resposta plana).
     */
    @Synchronized
    fun applySettings() {
        if (isEqualizerEnabled) {
            Log.i(
                TAG,
                "[CAN TX] Aplicando EQ -> graves=$bassLevel médios=$midLevel agudos=$trebleLevel"
            )
        } else {
            Log.i(TAG, "[CAN TX] EQ desviado (resposta plana)")
        }
    }

    /**
     * Simula a atualização de volume recebida pela rede CAN.
     * Retorna o novo volume para que a interface possa ser atualizada.
     */
    @Synchronized
    fun simulateCanVolumeUpdate(): Int {
        canVolume = Random.nextInt(MIN_LEVEL, MAX_LEVEL + 1)
        Log.d(TAG, "[CAN RX] quadro de volume recebido: $canVolume")
        return canVolume
    }

    private fun clamp(value: Int): Int = value.coerceIn(MIN_LEVEL, MAX_LEVEL)
}
