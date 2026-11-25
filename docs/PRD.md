# PRD – Suivi des bénéficiaires (Entraide Nationale)

## 1. Objectif
Remplacer le fichier Excel (≈112 000 lignes, saisie libre, doublons, CIN absents ou factices) par une application qui **contrôle la saisie** des bénéficiaires et permet d'analyser les données par région, province, association et catégorie.

## 2. Utilisateurs et droits
| Rôle | Périmètre | Bénéficiaires | Associations et établissements | Utilisateurs / audit |
|---|---|---|---|---|
| ADMIN (1 compte) | National | CRUD complet | CRUD complet, toutes provinces | Gestion des comptes, journal |
| PROVINCE (1 par province) | Sa province | CRUD complet | **Consultation seule** | – |
| REGIONAL (1 par région) | Sa région | Lecture seule | Lecture seule | – |

Comptes créés au premier démarrage (`<province>@entraide.ma`), mot de passe par défaut, changement obligatoire à la première connexion.

## 3. Données
- **Référence (issue des Excel)** : 12 régions, 82 provinces ; 7 catégories, 1 011 associations, 1 061 établissements (feuille = catégorie ; 9 établissements ajoutés à la main). Aucun bénéficiaire importé.
- **Association** : province + nom (AR). Le même nom dans une autre province est une autre association. Elle peut avoir plusieurs établissements.
- **Établissement** : association (donc province), **catégorie (cible, choix unique)**, nom (AR), type et numéro de licence (facultatifs), actif/archivé. Catégories (noms des feuilles Excel) : النساء في وضعية صعبة ; المسنين ; المركبات ; الأشخاص في وضعية صعبة ; الأشخاص في وضعية إعاقة ; دور الطالب و الطالبة ; الأطفال في وضعية صعبة.
- **Bénéficiaire** : **établissement** (choisi après l'association), nom/prénom (AR) et (FR) obligatoires, date de naissance, sexe, CIN facultatif. Pas de champ « année de saisie/mise à jour » : `created_at` / `updated_at` suffisent.

## 4. Règles de gestion
1. Le nom et le prénom français sont obligatoires pour permettre le contrôle des doublons.
2. Clé de dédoublonnage unique : `tri(nom FR, prénom FR normalisés) | date de naissance`.
3. Doublon exact : blocage, déblocable par confirmation explicite tracée (homonymes, jumeaux).
4. Avertissements non bloquants : jour/mois inversés, année voisine, nom très proche.
5. Doublon hors périmètre : signalé sans détail.
6. CIN : unique, format `AB123456`, vide autorisé.
7. Date de naissance : entre 1900 et aujourd'hui (le public inclut enfants, adultes et personnes âgées).
8. Une association qui a des établissements ne peut pas être supprimée. Un établissement qui a des bénéficiaires ne peut pas être supprimé : il est archivé (et peut être réactivé).
9. Un bénéficiaire supprimé est archivé (jamais effacé physiquement) et disparaît des listes et statistiques.
10. Toute connexion, saisie, modification, export et action admin est journalisée.

## 5. Écrans
Connexion · Changement de mot de passe obligatoire · Tableau de bord · Liste des bénéficiaires (recherche FR/AR, filtres, export CSV) · Ajouter / modifier un bénéficiaire avec contrôle de doublons en direct · Associations et Établissements (filtres ; ajout/modification/suppression/archivage réservés à l'admin ; consultation seule pour les provinces) · Ajouter un bénéficiaire : association, puis établissement · Utilisateurs (admin) · Journal d'audit (admin). Interface bilingue FR/AR avec RTL.

## 6. Hors périmètre v1
Fusion de doublons, import de l'historique, création de comptes depuis l'interface, édition de la liste des catégories depuis l'interface, pièces jointes, notifications.

## 7. Points à confirmer
- Noms arabes des provinces et rattachements régionaux.
- Libellé exact et nature de la catégorie « المركبات ».
- Politique de rétention des sauvegardes et du journal d'audit.
