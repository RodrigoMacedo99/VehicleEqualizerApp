package com.example.vehicleequalizerapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Serviço vinculado que recebe os comandos do equalizador da MainActivity (Etapas 3 e 4).
 *
 * A Activity se vincula com bindService(); o serviço permanece ativo enquanto houver
 * pelo menos um cliente vinculado. Os comandos chegam pelo stub AIDL [binder] (IPC via
 * Binder) e são repassados ao backend [VehicleAudioSystem].
 */
class EqualizerService : Service() {

    companion object {
        private const val TAG = "EqualizerService"
    }

    private lateinit var audioSystem: VehicleAudioSystem

    /** Implementação do AIDL: cada chamada chega por IPC via Binder. */
    private val binder = object : IEqualizerService.Stub() {
        override fun setEqualizerEnabled(enabled: Boolean) {
            Log.d(TAG, "Comando IPC: setEqualizerEnabled($enabled)")
            audioSystem.setEqualizerEnabled(enabled)
        }

        override fun setBassLevel(level: Int) {
            Log.d(TAG, "Comando IPC: setBassLevel($level)")
            audioSystem.setBassLevel(level)
            audioSystem.applySettings()
        }

        override fun setMidLevel(level: Int) {
            Log.d(TAG, "Comando IPC: setMidLevel($level)")
            audioSystem.setMidLevel(level)
            audioSystem.applySettings()
        }

        override fun setTrebleLevel(level: Int) {
            Log.d(TAG, "Comando IPC: setTrebleLevel($level)")
            audioSystem.setTrebleLevel(level)
            audioSystem.applySettings()
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioSystem = VehicleAudioSystem()
        Log.i(TAG, "onCreate: serviço criado")
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.i(TAG, "onBind: cliente conectado")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "onUnbind: cliente desconectado")
        // Retorna false: onRebind não é necessário; a próxima vinculação chama onBind novamente.
        return false
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: serviço destruído")
        super.onDestroy()
    }
}
