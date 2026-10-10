# Unio

Toutes vos conversations dans une seule application Android : SMS, WhatsApp, Messenger et Facebook.

**Démo du design (prototype web) :** activez GitHub Pages sur le dossier `/docs` (Settings → Pages → Branch `main` → `/docs`).

## Comment ça marche

| Canal | Méthode | Limite |
|---|---|---|
| SMS | `BroadcastReceiver` + `SmsManager` | Android uniquement |
| WhatsApp, Messenger, Facebook | `NotificationListenerService` + réponse rapide (`RemoteInput`) | Pas d'historique antérieur à l'installation ; la réponse exige une notification récente |

Aucune API non officielle n'est utilisée. Toutes les données restent sur l'appareil (base Room locale).

## Lancer le projet

1. Ouvrir le dossier dans **Android Studio** (il génère le wrapper Gradle).
2. Lancer sur un appareil Android 8.0+ (API 26).
3. Dans l'app : autoriser les SMS, puis activer l'accès aux notifications.

Le workflow `.github/workflows/android.yml` compile un APK de debug à chaque push.

## Structure

- `data/` : base Room (messages, fils de discussion)
- `channels/` : réception SMS, capture des notifications, envoi
- `ui/` : écrans Jetpack Compose (boîte unifiée, conversation)
- `docs/` : prototype web du design

## Feuille de route

- [ ] Rôles `ROLE_SMS` et `ROLE_DIALER` (SMS par défaut, MMS, appels via `InCallService`)
- [ ] Fusion des contacts (une personne = tous ses canaux) via `ContactsContract`
- [ ] Chiffrement de la base (SQLCipher)
- [ ] Recherche plein texte, sauvegarde chiffrée
- [ ] Politique de confidentialité (obligatoire pour Google Play)

## Avertissement

Google Play contrôle strictement les permissions SMS et l'accès aux notifications. Prévoir une justification claire et une politique de confidentialité avant publication.
