# Authentication

この文書は SD UI の Entra ID サインインの構成と、アプリ内での扱いをまとめます。

Entra ID 側のアプリ登録とスコープの正本はサーバー側リポジトリ (`sd-ui-infra`) です。
本書は、その登録をアプリがどう使うかを定めます。

## dev 環境の設定値

| 項目 | 値 | 置き場所 |
| --- | --- | --- |
| authority | `https://login.microsoftonline.com/11111111-1111-4111-8111-111111111111` | `res/raw/msal_config.json` |
| client_id | `22222222-2222-4222-8222-222222222222` | `res/raw/msal_config.json` |
| account_mode | `SINGLE` | `res/raw/msal_config.json` |
| redirect_uri | `msauth://com.example.sdui/AAAAAAAAAAAAAAAAAAAAAAAAAAA%3D` | `res/raw/msal_config.json` |
| scope | `api://33333333-3333-4333-8333-333333333333/UiSchema.Read` | `gradle.properties` の `API_SCOPE` |

`AndroidManifest.xml` の `BrowserTabActivity` には **URL エンコード前**の値を書きます。

```xml
<data android:scheme="msauth"
      android:host="com.example.sdui"
      android:path="/AAAAAAAAAAAAAAAAAAAAAAAAAAA=" />
```

この署名ハッシュは debug キーストア由来です。release ビルドは署名証明書が異なるため、
**release 用の redirect URI を Entra ID へ別途登録し、`msal_config.json` と Manifest を差し替える必要があります**。
現状のリポジトリには debug の値だけが入っています。

このクライアントは API 側で pre-authorized 済みのため、スコープの同意画面は表示されません。

## アプリ内の構成

```
LoginScreen ── LoginViewModel ── SignInUseCase ─┐
SessionRoute ─ SessionViewModel ─ RestoreSessionUseCase ─┼─ AuthRepository ── MsalAuthClient
                                └ SignOutUseCase ────────┘
AuthorizationInterceptor ── AccessTokenProvider ─────────── MsalAuthClient
```

- `domain`: `AuthSession`（`SignedIn` / `SignedOut`）、`AuthAccount`、値オブジェクト `AccountId` / `AccountName`、
  `AuthRepository`、`RestoreSessionUseCase` / `SignInUseCase` / `SignOutUseCase`。MSAL の型は現れません
- `data/auth`: MSAL を包む `MsalAuthClient`、対話サインインに必要な Activity を保持する `SignInActivityHolder`、
  interceptor 向けの `AccessTokenProvider`
- `ui`: `SessionRoute` がセッション状態で `LoginRoute` と `MainRoute` を出し分けます

`MsalAuthClient` は対話サインイン時に Activity を必要とします。`MainActivity` が composition root として
`SignInActivityHolder` へ自身を登録し、`data` 層はそこから Activity を取ります。
`ui` から `data` への依存は発生しません。

## 画面の流れ

1. 起動時に `SessionViewModel` が `RestoreSessionUseCase` を呼び、サイレントでトークン取得を試みる
2. 取得できれば `MainRoute`、できなければ `LoginRoute` を表示する
3. サインイン成功を `LoginRoute` が `SessionRoute` へ伝え、セッションを取り直す
4. UI スキーマ取得が 401（`UiSchemaResolution.Unauthorized`）になった場合はサインアウトし、ログイン画面へ戻す
5. `MainScreen` のサインアウトボタンからも同じ経路でログイン画面へ戻る

401 は「トークンが無い / 不正 / 期限切れ / scope 不足」を意味します。
役割を割り当てていないアカウントは 401 ではなく 200 が返り、`usage-summary` だけが表示されます。

## Authorization ヘッダーとローカル検証の排他

`AuthorizationInterceptor` は `@DevIdentityConfigured` が `true` のとき何もしません。

| ビルド | `DEV_OID` / `DEV_ROLES` | 送るもの |
| --- | --- | --- |
| debug | 指定あり | `X-Dev-Oid` / `X-Dev-Roles`（`Authorization` は付けない） |
| debug | 未指定 | `Authorization: Bearer <access token>` |
| release | — | `Authorization: Bearer <access token>` |

`@DevIdentityConfigured` の provider は `app/src/debug` と `app/src/release` の
それぞれの `DevIdentityModule` にあります。release には `X-Dev-*` を送る経路自体がありません。

トークンを取得できない場合は `Authorization` を付けずに送り、サーバーの 401 でログイン画面へ戻します。

## ログに出さないもの

- アクセストークン、UPN、メールアドレスはアプリからログ出力しません
- MSAL 自身の PII ログは `msal_config.json` の `logging` で無効化しています

```json
"logging": { "pii_enabled": false, "log_level": "WARNING", "logcat_enabled": false }
```

UPN は画面には表示します（サインイン中のアカウントの確認用）。ログ出力とは別に扱います。

## 動作確認

実 API へ向けてインストールします。

```powershell
.\gradlew.bat installDebug
```

接続先は `gradle.properties` の `API_BASE_URL` から入るため、Android Studio の Run ボタンでも同じです。
`DEV_OID` / `DEV_ROLES` を渡すと `Authorization` を送らなくなるため、実 API の確認では指定しません。

表示取得に失敗した場合は、画面に接続先が出ます。`https://sdui-api.example.invalid/api/`
以外が出ている場合は、接続先の設定が上書きされています。

テストユーザーごとに `dashboard` の表示が変わることを確認します。
アカウントを変えるときは、画面のサインアウトボタンで一度セッションを切ります。

| テストユーザー | 期待する表示 |
| --- | --- |
| `billing@example.invalid` | `usage-summary` に加えて `billing-summary` |
| `support@example.invalid` | `usage-summary` に加えて `impersonation-banner` |
| `plain@example.invalid` | `usage-summary` のみ |

役割を割り当てていないアカウント（管理者アカウントを含む）は `usage-summary` だけになります。
これは仕様どおりで、通信不良ではありません。初回サインインではパスワード変更を求められる場合があります。
