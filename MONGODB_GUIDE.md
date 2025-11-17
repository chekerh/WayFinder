# Guide MongoDB - Démarrer le serveur

## ✅ MongoDB est maintenant démarré

MongoDB écoute sur `localhost:27017` et MongoDB Compass devrait pouvoir se connecter.

## Comment démarrer MongoDB

### Méthode 1 : Via Homebrew (recommandé si l'espace disque est suffisant)

```bash
brew services start mongodb-community
```

Pour arrêter :
```bash
brew services stop mongodb-community
```

### Méthode 2 : Démarrage manuel (si Homebrew ne fonctionne pas)

```bash
# Démarrer MongoDB en arrière-plan
nohup mongod --dbpath /usr/local/var/mongodb --logpath /usr/local/var/log/mongodb/mongo.log > /dev/null 2>&1 &

# Vérifier qu'il est démarré
ps aux | grep "[m]ongod"
```

Pour arrêter :
```bash
# Trouver le processus
ps aux | grep "[m]ongod" | grep -v grep

# Arrêter (remplace PID par le numéro du processus)
kill PID
```

## Vérifier que MongoDB fonctionne

```bash
# Vérifier que le port 27017 est ouvert
nc -z localhost 27017 && echo "✅ MongoDB fonctionne" || echo "❌ MongoDB n'est pas démarré"
```

## Connexion depuis MongoDB Compass

1. Ouvre MongoDB Compass
2. Clique sur "localhost:27017" dans la liste des connexions
3. La connexion devrait maintenant fonctionner ✅

## Problèmes courants

### Erreur "ECONNREFUSED"
- **Cause** : MongoDB n'est pas démarré
- **Solution** : Utilise une des méthodes ci-dessus pour démarrer MongoDB

### Erreur "Input/output error" avec Homebrew
- **Cause** : Espace disque plein ou problème avec launchctl
- **Solution** : Utilise la méthode 2 (démarrage manuel)

### MongoDB ne démarre pas
- Vérifie les logs : `tail -f /usr/local/var/log/mongodb/mongo.log`
- Vérifie l'espace disque : `df -h`
- Vérifie que les répertoires existent :
  ```bash
  mkdir -p /usr/local/var/mongodb
  mkdir -p /usr/local/var/log/mongodb
  ```

## Configuration pour le backend NestJS

Ton backend NestJS devrait se connecter à :
```
mongodb://localhost:27017/wayfinder
```

(Remplace `wayfinder` par le nom de ta base de données)

