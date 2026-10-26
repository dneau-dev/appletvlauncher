Mi TV (MiTV-MSSP3) : nettoyage et lanceur Apple TV

TV : Xiaomi, Android 11 (SDK 30), IP locale 192.168.1.22. Sauvegarde initiale dans C:\platform-tools\backup\.

État actuel
Désactivés (pm disable-user --user 0) : com.google.android.tvlauncher (lanceur Google), com.miui.tv.analytics, com.xiaomi.statistic.
Accueil par défaut : com.example.appletvlauncher (lanceur Apple TV maison).
FLauncher (me.efesser.flauncher) reste installé comme accueil de secours.
Rien d'autre n'a été touché : assistant vocal, Chromecast, Miracast, mises à jour, PatchWall et les apps de streaming sont intacts.
Bouton Input de la télécommande : sans action (il envoie KEY_RECORD). Pour les sources, utilise le bouton Sources du lanceur ou Paramètres.
Tout remettre comme avant
restore_all.bat réactive tous les paquets listés dans disabled_by_us.txt (y compris le lanceur Google).
restore_launcher.bat remet le lanceur Google comme accueil.
Pour revenir à FLauncher : adb shell cmd package set-home-activity me.efesser.flauncher/me.efesser.flauncher.MainActivity
Réactiver une seule appli

adb shell pm enable <nom.du.paquet> (la liste des désactivés : adb shell pm list packages -d)

Changer l'ordre ou le contenu du dock
Dans ton dépôt GitHub, ouvre app/src/main/java/com/example/appletvlauncher/MainActivity.java.
Modifie le tableau DOCK (noms de paquets, de gauche à droite) : YouTube com.google.android.youtube.tv, Netflix com.netflix.ninja, Spotify com.spotify.tv.android, Prime Video com.amazon.amazonvideo.livingroom.
Commit : l'onglet Actions compile un nouvel APK. Télécharge l'artefact apk, puis adb install -r app-debug.apk.
Fond d'écran

Réglages > Fond d'écran sur l'accueil : choisis avec la télécommande (aperçu en direct, OK pour valider, Retour pour annuler). Le choix est mémorisé.
Fonds fournis : Aurore (défaut), Côte amalfitaine, Nuit bleue, Crépuscule, Océan, Braise, Noir pur.
Ajouter ton propre fond : copie une image (1920x1080, .jpg ou .png, de préférence < 500 Ko) dans app/src/main/res/drawable-nodpi/ en la nommant bg_monfond.jpg (minuscules, chiffres, _). Elle apparaît seule dans le sélecteur après recompilation. Un voile sombre est appliqué par-dessus pour garder le texte lisible.

Fermer le débogage réseau (à faire à la fin)

Le port 5555 est ouvert en clair tant que le débogage est actif.

Sur la TV : Options pour les développeurs, désactive Débogage USB et Débogage réseau/ADB, puis Révoquer les autorisations de débogage USB.
Redémarre la TV.
Vérifie que le port est fermé : Test-NetConnection 192.168.1.22 -Port 5555 dans PowerShell doit afficher TcpTestSucceeded : False.

Pour modifier la TV plus tard, il faudra réactiver le débogage.


Style Liquid Glass : dock, boutons du haut, surbrillance de sélection et panneau des fonds utilisent un verre translucide avec reflet spéculaire et liseré lumineux (classe Glass dans MainActivity.java : lightGlass, strongGlass, darkGlass). Pas de flou réel, pour rester léger sur Android 11.
