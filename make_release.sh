#!/bin/bash
# Crée (une seule fois) ta clé de signature, puis construit l'APK final.
set -e
cd "$(dirname "$0")"

if [ ! -f horloge-release.jks ]; then
  echo "Première fois : création de ta clé de signature."
  echo "Choisis un mot de passe SANS espace ni \\ . Garde-le précieusement."
  read -s -p "Mot de passe : " PW; echo
  read -s -p "Confirme     : " PW2; echo
  [ "$PW" = "$PW2" ] || { echo "Les mots de passe sont différents."; exit 1; }
  keytool -genkeypair -keystore horloge-release.jks -alias horloge \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$PW" -keypass "$PW" -dname "CN=Horloge Perso, O=Perso, C=MG"
  cat > keystore.properties <<EOF2
storeFile=horloge-release.jks
storePassword=$PW
keyAlias=horloge
keyPassword=$PW
EOF2
  chmod 600 keystore.properties
fi

./gradlew assembleRelease
echo
echo "APK final : app/build/outputs/apk/release/app-release.apk"
echo "SAUVEGARDE horloge-release.jks et keystore.properties ailleurs (clé USB, cloud privé)."
