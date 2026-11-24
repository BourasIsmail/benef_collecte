# Suivi des bénéficiaires – Entraide Nationale

Application web de saisie contrôlée et d'analyse des bénéficiaires, hébergée en local avec Docker.

| Couche | Technologie |
|---|---|
| Interface | Next.js 14 (React, TypeScript), FR / AR (RTL), polices embarquées (fonctionne hors ligne) |
| API | Java 21, Spring Boot 3.3, Spring Security (JWT en cookie HttpOnly), JPA, Flyway |
| Base | MySQL 8.4 (utf8mb4) |
| Déploiement | Docker Compose : `db`, `backend`, `frontend`, `backup` |

## Démarrage

```bash
cp .env.example .env
```

Éditer `.env` : remplacer toutes les valeurs `CHANGER-…` (mots de passe MySQL, `APP_JWT_SECRET` d'au moins 32 caractères : `openssl rand -base64 48`).

```bash
docker compose up -d --build
```

L'application est disponible sur http://localhost:3000 (port réglable avec `WEB_PORT`).
Seul le frontend est exposé : l'API et la base ne sont joignables que depuis le réseau Docker.

### Comptes créés au premier démarrage (une seule fois)

| Rôle | E-mail | Droits |
|---|---|---|
| `ADMIN` (un seul) | `admin@entraide.ma` | Tout : saisie, associations de toutes les provinces, utilisateurs, journal d'audit |
| `PROVINCE` (82) | `<province>@entraide.ma`, ex. `taza@entraide.ma`, `agadir-ida-outanane@entraide.ma` | Saisie des bénéficiaires et gestion (ajout, modification, suppression) des associations **de sa province** |
| `REGIONAL` (12) | `region.<région>@entraide.ma`, ex. `region.fes-meknes@entraide.ma` | **Lecture seule** et statistiques de sa région |

Tous les comptes reçoivent le mot de passe `APP_DEFAULT_PASSWORD` et **doivent le changer à la première connexion** (l'API refuse tout autre appel tant que ce n'est pas fait). Règle : 10 caractères minimum, majuscule, minuscule, chiffre, différent du mot de passe par défaut.
L'administrateur peut réinitialiser un mot de passe (retour au défaut + changement obligatoire) ou désactiver un compte depuis l'écran *Utilisateurs*.

La liste exacte des comptes est visible dans l'écran *Utilisateurs* (admin).

### Données de référence (depuis l'Excel)
Chargées au premier démarrage : **12 régions, 82 provinces, 1 210 associations** (`backend/src/main/resources/seed/*.json`). Les bénéficiaires de l'Excel ne sont **pas** importés : l'application démarre sans bénéficiaire.
- Les 81 provinces du fichier + **Aousserd** (absente du fichier, ajoutée sans association) : à confirmer.
- Les noms arabes des provinces et les rattachements aux régions sont proposés par nous : à relire.
- Les associations n'ont pas de catégorie : à renseigner par chaque province.

## Fonctionnement

### Anti-doublons (sans CIN)
Clé = `tri(nom FR, prénom FR normalisés) | date de naissance`. La normalisation ignore accents, ponctuation, `EL/AL` initial, lettres doublées et variantes courantes (MOHAMMED / MOUHAMED / MHAMED…). Le tri rend la clé insensible à l'inversion nom/prénom.
- **Pendant la saisie** : contrôle en direct (`POST /api/beneficiaries/check`) : doublon exact, jour/mois inversés, année voisine, nom très proche à la même date.
- **Doublon exact** : l'enregistrement est refusé (`409 DUPLICATE`) sauf confirmation explicite (homonyme, jumeau), tracée dans le journal.
- Un doublon situé **hors de votre périmètre** est signalé sans révéler son contenu.
- Le CIN est facultatif, mais unique et au format `AB123456` quand il est saisi.

### Catégorie des associations
7 catégories (table `categories`), **choix unique**, obligatoire quand on crée ou modifie une association (les associations chargées depuis l'Excel n'en ont pas encore). Filtre dans la liste des associations et statistiques par catégorie.
Une association qui a des bénéficiaires ne peut pas être supprimée : elle s'archive (`POST /api/associations/{id}/archive`) et se réactive (`/unarchive`).

### Statistiques
Tableau de bord filtré par périmètre : totaux, filles/garçons, par province ou association, tranches d'âge (0–5, 6–12, 13–17, 18–59, 60+), associations par catégorie, dernières fiches.

### Export
`Exporter (CSV)` : UTF-8 avec BOM (ouvre l'arabe correctement dans Excel), limité au périmètre, journalisé, protégé contre l'injection de formules.

## Sécurité
- Cookie de session `HttpOnly`, `SameSite=Strict` (`Secure` si `APP_COOKIE_SECURE=true`, à activer dès que le site est servi en HTTPS). Pas de CSRF classique : API JSON uniquement, cookie `SameSite=Strict`.
- Chaque requête est filtrée **côté serveur** par périmètre (province / région) ; un compte désactivé ou réinitialisé est effectif immédiatement.
- Verrouillage de 15 minutes après 5 échecs de connexion ; message d'erreur identique pour tous les cas.
- Mots de passe hachés avec BCrypt (coût 11). Aucun secret dans le code : tout vient de `.env`.
- Journal d'audit (connexions, saisies, modifications, exports, gestion des comptes) consultable par l'admin.
- Suppression logique des bénéficiaires (statut `ARCHIVE`) : rien n'est effacé physiquement.
- **À faire avant la mise en production** : changer tous les secrets de `.env`, servir le site en HTTPS (reverse proxy du réseau local), restreindre l'accès réseau au serveur.

## Sauvegardes
Le service `backup` produit chaque nuit à 02:00 un `mysqldump` compressé dans `./backups` (14 jours conservés, `BACKUP_KEEP_DAYS`). Sauvegarde immédiate :

```bash
docker compose exec backup bash /backup.sh once
```

Restauration :

```bash
gunzip -c backups/entraide-AAAAMMJJ-HHMMSS.sql.gz | docker compose exec -T db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" entraide'
```

Copiez régulièrement `./backups` hors du serveur.

## API (résumé)
`POST /api/auth/login|logout|change-password`, `GET /api/auth/me`, `GET /api/ref`, `GET|POST|PUT|DELETE /api/associations` (+ `POST /{id}/archive|unarchive`), `GET|POST|PUT|DELETE /api/beneficiaries`, `POST /api/beneficiaries/check`, `GET /api/beneficiaries/export.csv`, `GET /api/stats`, `GET /api/users` + `POST /{id}/reset-password` + `PUT /{id}/enabled` (admin), `GET /api/audit` (admin).

## Développement local
```bash
# base de données de test
docker run -d --name edb -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=entraide -e MYSQL_USER=entraide -e MYSQL_PASSWORD=dbpass -p 3307:3306 mysql:8.4 --character-set-server=utf8mb4

# API
cd backend
DB_URL='jdbc:mysql://127.0.0.1:3307/entraide?useUnicode=true&characterEncoding=utf8&serverTimezone=Africa/Casablanca' DB_PASSWORD=dbpass \
APP_JWT_SECRET=$(openssl rand -base64 48) APP_DEFAULT_PASSWORD='Entraide@2026' mvn spring-boot:run

# interface (proxifie /api vers localhost:8080)
cd frontend && npm install && npm run dev
```
Tests unitaires : `cd backend && mvn test`.

## Hors périmètre de cette version
Fusion des doublons par l'admin, import de bénéficiaires existants, création de comptes depuis l'interface, gestion de la liste des catégories depuis l'interface (table prête), notifications.
