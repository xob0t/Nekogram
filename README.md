# 🐾 Nekogram
[![Crowdin](https://badges.crowdin.net/e/a094217ac83905ae1625526d59bba8dc/localized.svg)](https://neko.crowdin.com/nekogram)  
Nekogram is a third-party Telegram client with not many but useful modifications.

- Website: https://nekogram.app
- Telegram channel: https://t.me/nekoupdates
- Downloads: https://nekogram.app/download
- Feedback: https://github.com/Nekogram/Nekogram/issues

## About this fork

This repository is a fork of a fork:

[Nekogram/Nekogram](https://github.com/Nekogram/Nekogram) → [risin42/Nekogram](https://github.com/risin42/Nekogram) → [xob0t/Nekogram](https://github.com/xob0t/Nekogram)

`main` tracks `risin42/main` (which already merges Nekogram's `main` and Telegram's `master`) and adds the changes below. To sync, merge `risin42/main` into `main`.

### Changes in this fork

#### Dialogs list

- **Account avatar in the top menu.** When the bottom navigation bar is hidden (Neko settings), the top-right "⋮" button shows the current account's avatar instead. Tap it to open the usual menu. Swipe down on it to switch to the next logged-in account. Accounts hidden behind a passcode are skipped. Also proposed upstream as [Nekogram#347](https://github.com/Nekogram/Nekogram/pull/347).
- **Option to disable pull-to-search** (Neko settings → General → "Disable pull down to show search"). Pulling the chat list down no longer reveals the search field, and the header doesn't reserve space for it. Pull-to-archive keeps working.
- **Pull and scroll fixes for the chat list.**
  - The header scroll offset is clamped, so the list can't end up half-scrolled or drawn blank after a pull.
  - The archived chats row stays hidden in the collapsed list when the archive is hidden.
  - Pull overscroll and the search offset reset when the split-pane chat on the right closes.
- **Chat list title hides behind the right-pane chat header.** In split view the chat list title fades out as the chat pane opens, instead of drawing over the chat header.
- **Option to hide the story camera button** (Neko settings → General → "Hide story camera button"). Removes the small camera button above the compose button and its first-run hint.

#### Chats and topics

- **Forum topic read state syncs properly.** Reading messages in a topic of a regular forum group now updates the topic's unread counter, both in storage and in the open topic list.
- **Stale scroll positions in topics expire.** Telegram reopens a topic at the last scroll position. If new messages arrived in the topic since then, this fork discards the saved position and opens the topic at the bottom instead.

#### Downloads

- **Media downloads keep running in the background.** A foreground service (`DownloadService`, type `dataSync`) shows a progress notification while files are downloading, so Android doesn't kill the download when the app is in the background. The service stops when the queue empties.
- **Canceled downloads leave the download list.** Canceling a file removes it from the downloads list and the notification right away.

#### CI

`.github/workflows/release.yml` builds signed APKs on every push to `main` and uploads them as two artifacts, `Release-arm64-v8a` and `Release-universal`, then posts them to Telegram. It needs these repository secrets:

| Secret | Contents |
| --- | --- |
| `GOOGLE_SERVICES_JSON_B64` | base64 of `TMessagesProj/google-services.json` |
| `RELEASE_KEYSTORE_B64` | base64 of the release keystore, written to `TMessagesProj/config/release.keystore` |
| `LOCAL_PROPERTIES` | lines appended to `local.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) |
| `APP_ID`, `APP_HASH` | Telegram API credentials, patched into `Extra.java` |
| `HELPER_BOT_TOKEN`, `HELPER_BOT_TARGET`, `HELPER_BOT_CANARY_TARGET` | bot and chats used by `Tools/scripts/upload.py` |

Add `[skip upload]` to the commit message to build without posting to Telegram.

## API, Protocol documentation

Telegram API manuals: https://core.telegram.org/api

MTProto protocol manuals: https://core.telegram.org/mtproto

## Compilation Guide

1. Download the Nekogram source code ( `git clone --recursive --shallow-submodules https://github.com/Nekogram/Nekogram.git` )
1. Fill out storeFile, storePassword, keyAlias, keyPassword in local.properties to access your release.keystore
1. Go to https://console.firebase.google.com/, create two android apps with application IDs tw.nekomimi.nekogram and tw.nekomimi.nekogram.beta, turn on firebase messaging and download `google-services.json`, which should be copied into `TMessagesProj` folder.
1. Open the project in the Studio (note that it should be opened, NOT imported).
1. Fill out values in `TMessagesProj/src/main/java/tw/nekomimi/nekogram/Extra.java` – there’s a link for each of the variables showing where and which data to obtain.
1. You are ready to compile Nekogram.

## Localization

Nekogram is forked from Telegram, thus most locales follows the translations of Telegram for Android, checkout https://translations.telegram.org/en/android/.

As for the Nekogram specialized strings, we use Crowdin to translate Nekogram. Join project at https://neko.crowdin.com/nekogram. Help us bring Nekogram to the world!
