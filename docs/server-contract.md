# Server Contract

この文書は SD UI とサーバーの間の通信契約と、応答を UI state へ変換する規則をまとめます。
契約は仮のもので、確定した時点で本書を更新します。

## エンドポイント

現在の唯一のエンドポイントは疎通確認用の `GET {baseUrl}health` です。

```json
{
  "status": "ok",
  "message": "ready"
}
```

- `status`: 必須。欠けている応答は復号失敗として扱う
- `message`: 任意。省略、`null`、空白のみは「メッセージなし」として扱う
- 未知のフィールドは無視する（`Json { ignoreUnknownKeys = true }`）

## ベース URL

- Gradle プロパティ `API_BASE_URL` で差し替える
- 末尾の `/` はビルド時に補完される
- 未指定時の既定値は `https://example.com/`
- 値は `BuildConfig.API_BASE_URL` を経由し、`@ApiBaseUrl` qualifier 付きで注入される

```powershell
.\gradlew.bat :app:assembleDebug "-PAPI_BASE_URL=http://10.0.2.2:8080/"
```

`debug` ビルドだけがローカル開発向けの平文 HTTP 通信を許可します（`app/src/debug/AndroidManifest.xml`）。
`release` ビルドでは許可しません。

## 応答の分類

応答は次の3つに分類し、それぞれ別の UI state へ変換します。

| 分類 | 条件 | domain 表現 | UI state |
| --- | --- | --- | --- |
| 接続可能 | HTTP 成功かつ `status` が `ok`（大文字小文字を区別しない） | `ServerAvailability.Available` | `ServerConnectionUiState.Connected` |
| サーバー都合の利用不可 | HTTP 成功かつ `status` が `ok` 以外 | `ServerAvailability.Unavailable` | `ServerConnectionUiState.Unavailable` |
| 通信失敗 | HTTP エラー、復号失敗、接続失敗 | `Result.failure` | `ServerConnectionUiState.Failed` |

`status` が空文字や空白のみの場合は、値オブジェクトの生成制約により通信失敗として扱われます。

## 変換の経路

```
HealthResponseDto -> ServerStatus -> ServerAvailability -> ServerConnectionUiState
     (data/model)   (data/mapper)    (domain/usecase)       (ui/viewmodel)
```

- `HealthResponseDto`: サーバー応答をそのまま写した DTO
- `ServerStatus`: `ServerStatusCode` と任意の `ServerStatusMessage` を持つ domain model
- `ServerAvailability`: `status` の解釈結果を表す sealed interface
- `ServerConnectionUiState`: 画面表示の分岐を表す UI 契約

エンドポイントやフィールドを増やすときも、この4段の分担を保ちます。

## 契約を変更するとき

1. 本書のエンドポイントと応答の分類を先に更新する
2. DTO、mapper、domain model、UseCase、UI state のどこまで影響するかを判定する
3. MockWebServer による JVM Unit Test で、成功系・欠損フィールド・HTTP エラーを押さえる
4. 表示文言を変える場合は `strings.xml` を更新する

## 今後の予定

Azure 固有の URL、認証、証明書設定は、サーバー契約が確定した時点で追加します。
現時点の通信層は通常の Retrofit API として分離しており、認証ヘッダーや interceptor は導入していません。
