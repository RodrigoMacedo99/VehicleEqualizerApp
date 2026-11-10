package com.example.vehicleequalizerapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

/**
 * Tela principal do equalizador automotivo.
 *
 * - Etapa 1: monta a interface a partir de activity_main.xml e habilita os SeekBars
 *   somente quando o Switch está ligado.
 * - Etapa 2: envia cada alteração ao backend local [VehicleAudioSystem].
 * - Etapas 3 e 4: vincula-se ao [EqualizerService] e envia as mesmas alterações via AIDL.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CAN_POLL_INTERVAL_MS = 3000L
    }

    // Backend da Etapa 2 (local)
    private val audioSystem = VehicleAudioSystem()

    // Proxy AIDL (Etapa 4): definido em onServiceConnected e limpo em onServiceDisconnected
    private var equalizerService: IEqualizerService? = null
    private var isServiceBound = false

    private lateinit var equalizerSwitch: SwitchCompat
    private lateinit var bassSeekBar: SeekBar
    private lateinit var midSeekBar: SeekBar
    private lateinit var trebleSeekBar: SeekBar
    private lateinit var canVolumeText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val canPollRunnable = object : Runnable {
        override fun run() {
            // Simula a chegada de um quadro de volume pela rede CAN a cada poucos segundos.
            val volume = audioSystem.simulateCanVolumeUpdate()
            canVolumeText.text = getString(R.string.can_volume_format, volume)
            handler.postDelayed(this, CAN_POLL_INTERVAL_MS)
        }
    }

    /** Etapa 4: ciclo de vida da conexão com o serviço vinculado. */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            equalizerService = IEqualizerService.Stub.asInterface(service)
            Log.i(TAG, "Conectado ao EqualizerService")
            // Envia o estado atual da interface para que o serviço fique sincronizado.
            pushStateToService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            equalizerService = null
            Log.w(TAG, "Desconectado do EqualizerService")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupListeners()
        // Requisito da Etapa 1: os controles começam desabilitados com o equalizador desligado.
        setEqualizerControlsEnabled(equalizerSwitch.isChecked)
    }

    override fun onStart() {
        super.onStart()
        // Etapa 3: vincula o serviço. BIND_AUTO_CREATE o inicia caso ainda não esteja em execução.
        val intent = Intent(this, EqualizerService::class.java)
        isServiceBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        Log.i(TAG, "bindService solicitado: $isServiceBound")
    }

    override fun onResume() {
        super.onResume()
        handler.post(canPollRunnable)
    }

    override fun onPause() {
        handler.removeCallbacks(canPollRunnable)
        super.onPause()
    }

    override fun onStop() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
            equalizerService = null
            Log.i(TAG, "Desvinculado do EqualizerService")
        }
        super.onStop()
    }

    private fun bindViews() {
        equalizerSwitch = findViewById(R.id.equalizerSwitch)
        bassSeekBar = findViewById(R.id.bassSeekBar)
        midSeekBar = findViewById(R.id.midSeekBar)
        trebleSeekBar = findViewById(R.id.trebleSeekBar)
        canVolumeText = findViewById(R.id.canVolumeText)
    }

    private fun setupListeners() {
        // Switch: habilita/desabilita os controles e notifica o backend e o serviço.
        equalizerSwitch.setOnCheckedChangeListener { _, isChecked ->
            setEqualizerControlsEnabled(isChecked)
            Log.i(TAG, "Chave do equalizador: $isChecked")
            audioSystem.setEqualizerEnabled(isChecked)
            runOnService { it.setEqualizerEnabled(isChecked) }
        }

        bassSeekBar.setOnSeekBarChangeListener(levelListener("Graves") { level ->
            audioSystem.setBassLevel(level)
            audioSystem.applySettings()
            runOnService { it.setBassLevel(level) }
        })

        midSeekBar.setOnSeekBarChangeListener(levelListener("Médios") { level ->
            audioSystem.setMidLevel(level)
            audioSystem.applySettings()
            runOnService { it.setMidLevel(level) }
        })

        trebleSeekBar.setOnSeekBarChangeListener(levelListener("Agudos") { level ->
            audioSystem.setTrebleLevel(level)
            audioSystem.applySettings()
            runOnService { it.setTrebleLevel(level) }
        })
    }

    /**
     * Cria um listener para SeekBar que registra no Logcat cada alteração e
     * repassa ao backend somente as alterações feitas pelo usuário (fromUser).
     */
    private fun levelListener(
        label: String,
        onLevelChanged: (Int) -> Unit
    ): SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.d(TAG, "Nível de $label: $progress (fromUser=$fromUser)")
                if (fromUser) onLevelChanged(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

    /** Etapa 1: habilita ou desabilita os três SeekBars ao mesmo tempo. */
    private fun setEqualizerControlsEnabled(enabled: Boolean) {
        bassSeekBar.isEnabled = enabled
        midSeekBar.isEnabled = enabled
        trebleSeekBar.isEnabled = enabled
    }

    /** Executa uma chamada AIDL se o serviço estiver conectado; caso contrário, registra e ignora. */
    private fun runOnService(call: (IEqualizerService) -> Unit) {
        val service = equalizerService
        if (service == null) {
            Log.w(TAG, "Serviço não conectado; comando mantido apenas localmente")
            return
        }
        try {
            call(service)
        } catch (e: RemoteException) {
            Log.e(TAG, "Falha na chamada AIDL", e)
        }
    }

    /** Envia o estado completo da interface ao serviço após a (re)conexão. */
    private fun pushStateToService() {
        runOnService {
            it.setEqualizerEnabled(equalizerSwitch.isChecked)
            it.setBassLevel(bassSeekBar.progress)
            it.setMidLevel(midSeekBar.progress)
            it.setTrebleLevel(trebleSeekBar.progress)
        }
    }
}
