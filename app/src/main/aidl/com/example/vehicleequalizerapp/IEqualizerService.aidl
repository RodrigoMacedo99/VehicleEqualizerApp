// Contrato AIDL entre a MainActivity (cliente) e o EqualizerService (servidor).
// Cada método é uma transação Binder; o build do Android gera
// IEqualizerService.Stub (lado servidor) e IEqualizerService.Stub.asInterface (lado cliente).
package com.example.vehicleequalizerapp;

interface IEqualizerService {
    /** Liga ou desliga o equalizador. */
    void setEqualizerEnabled(boolean enabled);

    /** Define o nível de graves, de 0 a 100. */
    void setBassLevel(int level);

    /** Define o nível de médios, de 0 a 100. */
    void setMidLevel(int level);

    /** Define o nível de agudos, de 0 a 100. */
    void setTrebleLevel(int level);
}
