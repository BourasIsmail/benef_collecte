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
| `ADMIN` (un seul) | `admin@entraide.ma` | Tout : bénéficiaires, **associations et établissements de toutes les provinces (ajout, modification, suppression, archivage)**, utilisateurs, journal d'audit |
| `PROVINCE` (82) | `<province>@entraide.ma`, ex. `taza@entraide.ma`, `agadir-ida-outanane@entraide.ma` | **CRUD complet sur les bénéficiaires** de sa province. **Consultation seule** des associations et des établissements |
| `REGIONAL` (12) | `region.<région>@entraide.ma`, ex. `region.fes-meknes@entraide.ma` | **Lecture seule** et statistiques de sa région |

Tous les comptes reçoivent le mot de passe `APP_DEFAULT_PASSWORD` et **doivent le changer à la première connexion** (l'API refuse tout autre appel tant que ce n'est pas fait). Règle : 10 caractères minimum, majuscule, minuscule, chiffre, différent du mot de passe par défaut.
L'administrateur peut réinitialiser un mot de passe (retour au défaut + changement obligatoire) ou désactiver un compte depuis l'écran *Utilisateurs*.

La liste exacte des comptes est visible dans l'écran *Utilisateurs* (admin).

### Données de référence (depuis les Excel)
Chargées au premier démarrage (`backend/src/main/resources/seed/*.json`) :
- **12 régions, 82 provinces** (fichier des bénéficiaires 2025-2026, + Aousserd) ;
- **7 catégories, 1 003 associations et 1 052 établissements** (fichier `base_EPS_2026`) : chaque feuille Excel est une catégorie (le nom de la feuille sans « 2026 »), chaque ligne un établissement rattaché à une association ; la province de l'établissement est celle de son association. Les lignes de total sont ignorées. RIB, comptes bancaires, montants et noms des présidents ne sont **pas** repris.

Les bénéficiaires ne sont **pas** importés : l'application démarre sans bénéficiaire.
À relire : noms arabes des provinces et rattachements aux régions (proposés par nous). Aousserd est confirmée (82 provinces).

> **Mise à jour depuis une version précédente** : le schéma a changé (établissements). Pour repartir d'une base propre : `docker compose down -v` (⚠ efface la base) puis `docker compose up -d --build`.

## Fonctionnement

### Anti-doublons (sans CIN)
Clé = `tri(nom FR, prénom FR normalisés) | date de naissance`. La normalisation ignore accents, ponctuation, `EL/AL` initial, lettres doublées et variantes courantes (MOHAMMED / MOUHAMED / MHAMED…). Le tri rend la clé insensible à l'inversion nom/prénom.
- **Pendant la saisie** : contrôle en direct (`POST /api/beneficiaries/check`) : doublon exact, jour/mois inversés, année voisine, nom très proche à la même date.
- **Doublon exact** : l'enregistrement est refusé (`409 DUPLICATE`) sauf confirmation explicite (homonyme, jumeau), tracée dans le journal.
- Un doublon situé **hors de votre périmètre** est signalé sans révéler son contenu.
- Le CIN est facultatif, mais unique et au format `AB123456` quand il est saisi.

### Associations, établissements et catégories
- Une **association** appartient à **une province** ; le même nom dans une autre province est **une autre association** (unicité = province + nom). Elle peut avoir **plusieurs établissements**.
- Un **établissement** appartient à une association (donc à sa province) et porte **la catégorie (la cible)**, à choix unique, parmi les 7 feuilles de l'Excel : femmes en situation difficile, personnes âgées, complexes, personnes en situation difficile, personnes en situation de handicap, « دور الطالب و الطالبة » (scolarisation), enfants en situation difficile. Il a aussi un type et un numéro de licence (facultatifs).
- Un **bénéficiaire** appartient à **un établissement** : à la saisie on choisit d'abord l'association (liste filtrable), puis l'établissement de cette association.
- Seul l'administrateur modifie associations et établissements. Une association qui a des établissements, ou un établissement qui a des bénéficiaires, ne peut pas être supprimé : l'établissement s'archive et se réactive.

### Statistiques
Tableau de bord filtré par périmètre : totaux, filles/garçons, par province ou par établissement, tranches d'âge (0–5, 6–12, 13–17, 18–59, 60+), établissements par catégorie, dernières fiches.

### Tableaux : filtres et pagination
Tous les tableaux (bénéficiaires, associations, établissements, utilisateurs, journal d'audit) ont une pagination (10, 20, 50 ou 100 lignes par page, première / précédente / suivante / dernière) et des filtres avec un bouton « Réinitialiser les filtres » : recherche, province, catégorie, association, établissement, sexe, rôle, état, action, utilisateur et période selon l'écran. Bénéficiaires et journal sont paginés côté serveur ; les autres listes, bornées (~1 000 lignes), côté navigateur.

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
`POST /api/auth/login|logout|change-password`, `GET /api/auth/me`, `GET /api/ref`, `GET /api/associations` + `POST|PUT|DELETE` (admin), `GET /api/etablissements` + `POST|PUT|DELETE` + `POST /{id}/archive|unarchive` (admin), `GET|POST|PUT|DELETE /api/beneficiaries`, `POST /api/beneficiaries/check`, `GET /api/beneficiaries/export.csv`, `GET /api/stats`, `GET /api/users` + `POST /{id}/reset-password` + `PUT /{id}/enabled` (admin), `GET /api/audit` (admin).

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
Fusion des doublons par l'admin, import de bénéficiaires existants, création de comptes depuis l'interface, gestion de la liste des catégories depuis l'interface (table prête), fusion d'associations homonymes, notifications.
