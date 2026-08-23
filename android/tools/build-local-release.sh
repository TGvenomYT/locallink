#!/usr/bin/env bash
# Builds a signed, installable LocalLink release APK for local testing/sideloading.
#
# This is NOT the upstream bitchat Play/GitHub release process described in
# docs/maintainer-release-guide.md (that requires credentials this fork does
# not have). It self-signs with a local-only keystore under android/keystore/,
# generated on first run and never committed to git.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -z "${JAVA_HOME:-}" ]; then
    STUDIO_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    if [ -x "$STUDIO_JBR/bin/java" ]; then
        export JAVA_HOME="$STUDIO_JBR"
    else
        echo "JAVA_HOME is not set and Android Studio's bundled JDK was not found." >&2
        echo "Set JAVA_HOME to a JDK 21 install and re-run." >&2
        exit 1
    fi
fi

KEYSTORE_DIR="keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/locallink-release.jks"
PROPS_FILE="keystore.properties"

if [ ! -f "$PROPS_FILE" ]; then
    echo "No keystore.properties found — generating a local signing key..."
    mkdir -p "$KEYSTORE_DIR"
    STORE_PASS=$(openssl rand -base64 24 | tr -d '/+=' | cut -c1-24)
    "$JAVA_HOME/bin/keytool" -genkeypair -v \
        -keystore "$KEYSTORE_FILE" \
        -alias locallink \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass "$STORE_PASS" -keypass "$STORE_PASS" \
        -dname "CN=LocalLink, OU=Dev, O=LocalLink, L=NA, S=NA, C=US"

    cat > "$PROPS_FILE" <<EOF
storeFile=$KEYSTORE_FILE
storePassword=$STORE_PASS
keyAlias=locallink
keyPassword=$STORE_PASS
EOF
    echo "Wrote $PROPS_FILE (gitignored) — back this up if you want stable"
    echo "APK update signatures across builds."
fi

./gradlew :app:assembleRelease "$@"

echo
echo "Built APKs:"
find app/build/outputs/apk/release -name "*.apk" -exec ls -la {} \;
