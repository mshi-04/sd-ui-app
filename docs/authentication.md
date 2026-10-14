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
LoginScreen ── LoginViewModel ── SignInUseCase ──┐
SdUiApp ── AuthViewModel ─────── SignOutUseCase ──┼─ AuthRepository ── MsalAuthClient
  └─ MainRoute                                    ┘
AuthorizationInterceptor ── AccessTokenProvider ──────────────────── MsalAuthClient
```

- `domain`: `AuthSession`（`SignedIn` / `SignedOut`）、`AuthAccount`、値オブジェクト `AccountId` / `AccountName`、
  `AuthRepository`、`SignInUseCase` / `SignOutUseCase`。MSAL の型は現れません
- `data/auth`: MSAL を包む `MsalAuthClient`、対話サインインに必要な Activity を保持する `SignInActivityHolder`、
  interceptor 向けの `AccessTokenProvider`
- `ui`: `SdUiApp` が `AuthViewModel` の認証状態（`AuthUiStatus`）で、サインアウト中画面、サインアウト失敗画面、
  `LoginRoute`、`MainRoute` を出し分けます。`LoginRoute` は `LoginViewModel` と `LoginScreen` を接続します

`MsalAuthClient` は対話サインイン時に Activity を必要とします。`MainActivity` が composition root として
`SignInActivityHolder` へ自身を登録し、`data` 層はそこから Activity を取ります。
`ui` から `data` への依存は発生しません。

## 画面の流れ

1. 起動時に `AuthViewModel` が `SignOutUseCase` を呼び、前回のセッションが残っていれば破棄する
   （この間は `SIGNING_OUT` で、サインアウト中画面を表示する）
2. サインアウトに成功すると `SIGNED_OUT` になり、`LoginRoute` を表示する。利用者は起動のたびにサインインする
3. `LoginViewModel` が `SignInUseCase` を呼び、成功すると `LoginRoute` が返されたアカウント名を
   `AuthViewModel` へ伝える。`AuthViewModel` はセッションを取り直さず、サインイン状態（`SIGNED_IN`）を直接更新し、
   `MainRoute` を表示する。`MainRoute` は表示された時点で UI スキーマの取得を始め、再表示時は前回の取得を置き換える
4. UI スキーマ取得が 401（`UiSchemaResolution.Unauthorized`）になった場合はサインアウトし、ログイン画面へ戻す
5. `MainScreen` のサインアウトボタンからも同じ経路でログイン画面へ戻る
6. サインアウトに失敗すると `SIGN_OUT_FAILED` になり、サインアウト失敗画面を表示する。
   保護された内容とログイン画面は出さず、再試行ボタンで同じサインアウトをやり直す。
   `signOut()` を受け付けるのは `SIGNED_IN` と `SIGN_OUT_FAILED` のときだけで、`SIGNING_OUT` と `SIGNED_OUT` では無視する

起動時のサインアウトは、前回のセッションを自動で復元しない設計の結果です。
加えて、MSAL の single-account モードでは、アカウントがキャッシュに残っていると `signIn()` が
「An account is already signed in.」で失敗するため、起動時に残りを消す必要があります。
`MsalAuthClient.signOut()` は、キャッシュ上のアカウントが無ければ何もしません。
MSAL の `signOut()` が `false` を返しても、その後にアカウントが残っていなければ成功として扱い、
残っている場合だけ失敗にします（`false` はトークンキャッシュに該当レコードが無かった場合にも返るため）。

MSAL のサインアウトはアプリ内のアカウントとトークンを消すだけで、ブラウザーの Entra ID セッションは終了しません。
そのため、次のサインインではパスワード入力なしに SSO で完了することがあります。

401 は「トークンが無い / 不正 / 期限切れ / scope 不足」を意味します。
役割を割り当てていないアカウントは 401 ではなく 200 が返り、`usage-summary` だけが表示されます。

`AuthorizationInterceptor` は、`https` で host と port がベース URL と一致するリクエストにだけ
`Authorization` を付けます。それ以外のリクエストではトークンを取得せずに送ります。
対象のリクエストでもトークンを取得できない場合は `Authorization` を付けずに送り、
サーバーの 401 でログイン画面へ戻します。

## ログに出さないもの

- アクセストークン、UPN、メールアドレスはアプリからログ出力しません
- MSAL 自身の PII ログは `msal_config.json` の `logging` で無効化しています

```json
"logging": { "pii_enabled": false, "log_level": "WARNING", "logcat_enabled": false }
```

UPN は画面には表示します（サインイン中のアカウントの確認用）。ログ出力とは別に扱います。

## バックアップと端末間移行

MSAL のトークン・アカウントキャッシュを含むアプリデータは、クラウドバックアップにも端末間移行にも含めません。
Android 12 以降では `android:allowBackup="false"` だけでは端末間移行が止まらないため、
`data_extraction_rules.xml` の `<cloud-backup>` と `<device-transfer>` の両方で、全ドメイン
（`root` / `file` / `database` / `sharedpref` / `external`）を `path="."` で除外しています。

## 動作確認

実 API へ向けてインストールします。

```powershell
.\gradlew.bat installDebug
```

接続先は `gradle.properties` の `API_BASE_URL` から入るため、Android Studio の Run ボタンでも同じです。

表示取得に失敗した場合は、画面に接続先が出ます。`https://sdui-api.example.invalid/api/`
以外が出ている場合は、接続先の設定が上書きされています。

テストユーザーごとに `dashboard` の表示が変わることを確認します。
アカウントを変えるときは、画面のサインアウトボタンで一度セッションを切ります。
サインインし直すと `MainRoute` が再表示され、その利用者のスキーマを取り直します。

| テストユーザー | 期待する表示 |
| --- | --- |
| `billing@example.invalid` | `usage-summary` に加えて `billing-summary` |
| `support@example.invalid` | `usage-summary` に加えて `impersonation-banner` |
| `plain@example.invalid` | `usage-summary` のみ |

役割を割り当てていないアカウント（管理者アカウントを含む）は `usage-summary` だけになります。
これは仕様どおりで、通信不良ではありません。初回サインインではパスワード変更を求められる場合があります。
