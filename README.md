# VehicleEqualizerApp

Aplicação Android automotiva que simula um equalizador de áudio (Kotlin, Android Studio).

## Dependências

Todas as dependências são declaradas em `app/build.gradle.kts` e baixadas automaticamente pelo Gradle na primeira compilação. Não é preciso instalar nenhuma biblioteca manualmente.

| Biblioteca | Versão | Uso |
|---|---|---|
| androidx.core:core-ktx | 1.12.0 | Extensões Kotlin do Android |
| androidx.appcompat:appcompat | 1.6.1 | Base da `MainActivity` |
| com.google.android.material:material | 1.11.0 | Tema Material |
| androidx.constraintlayout:constraintlayout | 2.1.4 | `ConstraintLayout` da interface |

Plugins (em `build.gradle.kts` e `app/build.gradle.kts`): Android Gradle Plugin 8.2.2 e Kotlin 1.9.22.

## Pré-requisitos

- JDK 17 ou superior
- `curl` e `unzip`
- Android SDK (o script de setup instala o necessário)

## Instalação e execução

Na raiz do projeto:

```bash
# 1. Prepara o ambiente (SDK, Gradle Wrapper) e compila o APK de debug
./scripts/setup.sh

# 2. Compila, instala e abre o app em um emulador ou dispositivo conectado
./scripts/setup.sh --run
```

Depois da primeira vez, os comandos do dia a dia são:

```bash
./gradlew assembleDebug      # compila o APK
./gradlew installDebug       # instala no dispositivo conectado
```

Para ver os logs da comunicação (Logcat, pelo terminal):

```bash
adb logcat -s MainActivity EqualizerService VehicleAudioSystem
```

## Estrutura

```
app/src/main/
├── aidl/.../IEqualizerService.aidl    # Contrato AIDL (Etapa 4)
├── java/.../MainActivity.kt           # Interface, Switch, SeekBars, ServiceConnection
├── java/.../VehicleAudioSystem.kt     # Backend simulado (Etapa 2)
├── java/.../EqualizerService.kt       # Serviço vinculado (Etapas 3 e 4)
└── res/layout/activity_main.xml       # Interface (Etapa 1)
```
