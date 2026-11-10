#!/usr/bin/env bash
# Prepara o ambiente e compila o VehicleEqualizerApp.
#
# Uso:
#   ./scripts/setup.sh           -> instala SDK, gera o Gradle Wrapper e compila o APK de debug
#   ./scripts/setup.sh --run     -> o mesmo, e instala + abre o app em um emulador/dispositivo conectado
#
# Pré-requisitos: JDK 17 ou superior, curl e unzip.
# Variáveis opcionais: ANDROID_HOME (padrão: $HOME/Android/Sdk)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GRADLE_VERSION="8.7"
SDK_DIR="${ANDROID_HOME:-$HOME/Android/Sdk}"
# Ferramentas de linha de comando do Android (sdkmanager). Se o link mudar, atualize-o
# a partir de https://developer.android.com/studio#command-tools
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
APP_ID="com.example.vehicleequalizerapp"

RUN_AFTER_BUILD=false
[ "${1:-}" = "--run" ] && RUN_AFTER_BUILD=true

need() {
  command -v "$1" >/dev/null 2>&1 || { echo "ERRO: '$1' não encontrado. Instale-o e rode novamente."; exit 1; }
}
need java
need curl
need unzip

JAVA_MAJOR="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
if [ "${JAVA_MAJOR:-0}" -lt 17 ]; then
  echo "ERRO: é necessário JDK 17 ou superior (encontrado: ${JAVA_MAJOR:-nenhum})."
  exit 1
fi
echo "==> JDK $JAVA_MAJOR encontrado"

# 1. Android SDK (plataforma 34, build-tools e platform-tools)
echo "==> Verificando Android SDK em $SDK_DIR"
if [ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "    Baixando command-line tools..."
  mkdir -p "$SDK_DIR/cmdline-tools"
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/cmdline-tools.zip" "$CMDLINE_TOOLS_URL"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  rm -rf "$SDK_DIR/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
  rm -rf "$tmp"
fi

SDKMANAGER="$SDK_DIR/cmdline-tools/latest/bin/sdkmanager"
echo "    Aceitando licenças..."
yes | "$SDKMANAGER" --sdk_root="$SDK_DIR" --licenses >/dev/null || true
echo "    Instalando platform-tools, platforms;android-34 e build-tools;34.0.0..."
"$SDKMANAGER" --sdk_root="$SDK_DIR" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "sdk.dir=$SDK_DIR" > "$ROOT/local.properties"

# 2. Gradle Wrapper (gera gradlew apenas se ainda não existir)
if [ ! -x "$ROOT/gradlew" ]; then
  echo "==> Gerando Gradle Wrapper ($GRADLE_VERSION)"
  GRADLE_BIN="$HOME/.cache/gradle-$GRADLE_VERSION/bin/gradle"
  if [ ! -x "$GRADLE_BIN" ]; then
    echo "    Baixando Gradle $GRADLE_VERSION..."
    mkdir -p "$HOME/.cache"
    tmp="$(mktemp -d)"
    curl -fsSL -o "$tmp/gradle.zip" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    unzip -q "$tmp/gradle.zip" -d "$HOME/.cache"
    rm -rf "$tmp"
  fi
  (cd "$ROOT" && "$GRADLE_BIN" wrapper --gradle-version "$GRADLE_VERSION")
fi

# 3. Compilação
cd "$ROOT"
echo "==> Compilando APK de debug"
./gradlew --no-daemon assembleDebug
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
echo "==> APK gerado: $APK"

# 4. Instalação e execução (opcional)
if [ "$RUN_AFTER_BUILD" = true ]; then
  ADB="$SDK_DIR/platform-tools/adb"
  echo "==> Dispositivos conectados:"
  "$ADB" devices
  echo "==> Instalando e abrindo o app"
  "$ADB" install -r "$APK"
  "$ADB" shell am start -n "$APP_ID/.MainActivity"
  echo "==> Para ver os logs:"
  echo "    $ADB logcat -s MainActivity EqualizerService VehicleAudioSystem"
fi
