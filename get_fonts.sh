#!/bin/bash
# Télécharge des polices Google Fonts (licence OFL, libres) dans l'app.
cd "$(dirname "$0")"
mkdir -p app/src/main/assets/fonts
cd app/src/main/assets/fonts
B=https://github.com/google/fonts/raw/main/ofl
get() {
  if wget -q -O "$2" "$B/$1/$2"; then echo "OK      $2"; else echo "ÉCHEC   $2"; rm -f "$2"; fi
}
get poppins Poppins-Thin.ttf
get poppins Poppins-Light.ttf
get poppins Poppins-Medium.ttf
get poppins Poppins-Bold.ttf
get poppins Poppins-Black.ttf
get bebasneue BebasNeue-Regular.ttf
get righteous Righteous-Regular.ttf
get pacifico Pacifico-Regular.ttf
get lobster Lobster-Regular.ttf
get audiowide Audiowide-Regular.ttf
get michroma Michroma-Regular.ttf
get archivoblack ArchivoBlack-Regular.ttf
get anton Anton-Regular.ttf
get syncopate Syncopate-Bold.ttf
get monoton Monoton-Regular.ttf
get bungee Bungee-Regular.ttf
get staatliches Staatliches-Regular.ttf
get spacemono SpaceMono-Regular.ttf
echo "Terminé. Recompile l'app pour les voir apparaître dans la liste."
