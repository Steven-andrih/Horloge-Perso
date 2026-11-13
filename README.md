# Horloge Perso

Un fond d'écran animé (live wallpaper) pour Android qui affiche une horloge entièrement personnalisable sur l'écran de verrouillage et l'écran d'accueil, un peu comme sur iPhone. Pensé pour les téléphones plus anciens, sans root et sans service Google.

Développé et testé sur un **Redmi Note 8 Pro (Android 11, MIUI 12)**.

## Fonctionnalités

- **5 styles d'horloge** : classique, empilé (heures au-dessus des minutes), contour, dégradé, analogique.
- **Polices** : 8 polices système, plus toutes les polices `.ttf` / `.otf` déposées dans `assets/fonts/`. Chaque nom est affiché dans sa propre police.
- **Couleurs** : 17 couleurs prédéfinies, un sélecteur libre (teinte, saturation, luminosité) et une deuxième couleur pour le dégradé et l'aiguille des minutes.
- **Opacité, taille, position verticale**, format 24 h / 12 h, ombre sous le texte.
- **Date** : format long, court ou jour seul, activable ou non.
- **Fond** : une photo de ta galerie (assombrissable) ou une couleur unie.
- **Photo d'accueil séparée** : une autre photo pour l'écran d'accueil, avec ou sans horloge.
- **Où afficher l'horloge** : accueil + verrouillage, verrouillage seulement, ou accueil seulement.
- **Profils** : enregistre réglages **et** photos sous un nom (Travail, Nuit…) et recharge-les en un tap.
- **Réinitialiser** les réglages (les photos et les profils sont conservés).
- Interface sombre aux couleurs `#222831` / `#393E46` / `#7BC74D` / `#EEEEEE`.

## Compatibilité

| | |
|---|---|
| Android minimum | 7.0 (API 24) |
| Cible | Android 14 (API 34) |
| Dépendances externes | aucune (Kotlin et API Android seulement) |
| Droits spéciaux | aucun (pas de root, pas d'Internet) |

## Installer l'APK

1. Copie `app-release.apk` sur le téléphone.
2. Autorise l'installation depuis cette source si Android le demande.
3. Ouvre **Horloge Perso**, règle l'horloge, puis touche **Appliquer comme fond d'écran**.
4. Dans la fenêtre du système, choisis l'option qui inclut l'écran de verrouillage **et** l'accueil. Le réglage « Afficher l'horloge sur » décide ensuite où elle apparaît.

## Compiler depuis les sources

### Prérequis

- JDK 17 ou plus (testé avec JDK 21)
- Android SDK en ligne de commande, avec `platforms;android-34` et `build-tools;34.0.0`
- La variable `ANDROID_HOME` qui pointe vers le SDK (par exemple `~/Android/Sdk`)
- `adb` pour installer sur le téléphone, avec le débogage USB activé

### Étapes

```bash
git clone <url-du-depot>
cd HorlogePerso

# Télécharge les polices Google Fonts (licence OFL)
bash get_fonts.sh

# Version de test
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Si `gradlew` est absent, génère-le une fois avec Gradle 8.7 installé : `gradle wrapper`.

### Version finale signée

```bash
bash make_release.sh
```

Au premier lancement, le script crée ta clé de signature (`horloge-release.jks`) et le fichier `keystore.properties`, puis compile `app/build/outputs/apk/release/app-release.apk`.

> **Sauvegarde ta clé** (`horloge-release.jks` et `keystore.properties`) dans un endroit privé. Sans elle, tu ne pourras plus mettre l'app à jour avec la même signature. Ces fichiers sont dans le `.gitignore` : ne les publie jamais.

Une version de test et une version finale ont des signatures différentes. Pour passer de l'une à l'autre : `adb uninstall com.horloge.perso`, puis installe l'autre.

## Structure du projet

```
app/src/main/java/com/horloge/perso/
├── MainActivity.kt          écran de réglages
├── Ui.kt                    composants d'interface (cartes, pastilles, curseurs…)
├── ClockWallpaperService.kt le live wallpaper
├── ClockRenderer.kt         dessin du fond et des 5 styles d'horloge
├── Prefs.kt                 réglages et photos de fond
├── Profiles.kt              profils enregistrés
└── Fonts.kt                 polices système et polices ajoutées
app/src/main/res/            icône, thèmes, métadonnées du wallpaper
get_fonts.sh                 télécharge les polices
make_release.sh              crée la clé et compile l'APK final
```

## Comment ça marche

Android interdit à une app normale de modifier l'écran de verrouillage. Horloge Perso est donc un **live wallpaper** : elle dessine l'horloge directement dans le fond d'écran, ce qui s'affiche aussi sur l'écran de verrouillage.

Sur Android 11, un live wallpaper s'applique aux deux écrans. Pour « verrouillage seulement » ou « accueil seulement », le service détecte si le téléphone est verrouillé (`KeyguardManager`) et redessine le fond avec ou sans l'horloge au déverrouillage.

## Limites connues

- **L'horloge native du système** (MIUI et autres) reste affichée par-dessus le fond d'écran. Une app normale ne peut pas la masquer. Les solutions sont de choisir un style discret dans les réglages du système, ou, sur un appareil rooté, d'utiliser un module comme LSPosed.
- La fenêtre de choix du système (accueil / verrouillage / les deux) est fournie par Android : l'app ne peut pas y ajouter d'option.
- La photo de fond doit être choisie dans l'app. Elle ne reprend pas automatiquement le fond d'écran précédent.
- Le changement d'écran (verrouillage → accueil) peut avoir un très léger délai.

## Idées pour la suite

- Effet de profondeur : l'heure derrière le sujet de la photo, avec un masque dessiné à la main.
- Plus de styles d'horloge et de formats de date.

## Licence

À choisir avant la publication (par exemple MIT). Aucune licence n'est définie pour l'instant.

## Crédits

- Polices : [Google Fonts](https://fonts.google.com), sous licence SIL Open Font License. Elles ne sont pas incluses dans le dépôt : `get_fonts.sh` les télécharge.
- Inspiration : l'effet d'horloge de l'écran de verrouillage d'iOS, et le projet open source Atmo Engine.
