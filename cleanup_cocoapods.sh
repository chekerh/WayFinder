#!/bin/bash

# Script de nettoyage de CocoaPods après migration vers Swift Package Manager
# ⚠️ UTILISEZ CE SCRIPT SEULEMENT APRÈS AVOIR VÉRIFIÉ QUE SPM FONCTIONNE CORRECTEMENT !

echo "🧹 Nettoyage des fichiers CocoaPods..."
echo ""
echo "⚠️  ATTENTION : Ce script va supprimer :"
echo "   - Le dossier Pods/"
echo "   - Le fichier Podfile.lock"
echo "   - Le workspace WayFinder.xcworkspace"
echo ""
read -p "Êtes-vous sûr de vouloir continuer ? (oui/non) " -n 3 -r
echo ""

if [[ ! $REPLY =~ ^[Oo][Uu][Ii]$ ]]; then
    echo "❌ Nettoyage annulé."
    exit 1
fi

# Aller dans le dossier ios
cd "$(dirname "$0")"

# Supprimer les fichiers CocoaPods
echo "🗑️  Suppression du dossier Pods/..."
rm -rf Pods/

echo "🗑️  Suppression du fichier Podfile.lock..."
rm -f Podfile.lock

echo "🗑️  Suppression du workspace WayFinder.xcworkspace..."
rm -rf WayFinder.xcworkspace

echo ""
echo "✅ Nettoyage terminé !"
echo ""
echo "📝 Note : Le fichier Podfile a été conservé comme référence."
echo "   Vous pouvez le supprimer manuellement si vous le souhaitez."
echo ""
echo "🎉 Vous pouvez maintenant utiliser WayFinder.xcodeproj directement !"

