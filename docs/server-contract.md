# Server Contract

この文書は SD UI とサーバーの間の通信契約と、応答を UI state へ変換する規則をまとめます。

契約そのものの正本はサーバー側リポジトリ (`sd-ui-infra`) の `docs/client_integration.md` です。
本書は、その契約をクライアントがどう受け取り、どの層でどう解釈するかを定めます。
契約が更新されたときは、先に本書を合わせてから実装を変更します。

## エンドポイント

現在の唯一のエンドポイントは UI スキーマ取得の `GET {baseUrl}ui/{surface}` です。
`baseUrl` は `/api/` で終わるため、実際のパスは `/api/ui/{surface}` になります。

| 項目 | 内容 |
| --- | --- |
| `surface` | 画面識別子。現在は `dashboard` のみ |
| `If-None-Match` | 直前の応答の `ETag`。保持していない場合は送らない |
| `Authorization` | `Bearer <access token>`。MSAL で取得し、ベース URL と同じ host / port の `https` リクエストにだけ付ける。構成は [`docs/authentication.md`](authentication.md) |

| ステータス | 意味 |
| --- | --- |
| 200 | UI スキーマを返す。`ETag` と `Cache-Control: private, max-age=60` を伴う |
| 304 | `If-None-Match` が一致。ボディなし |
| 401 | トークンが無効、または `UiSchema.Read` scope が無い。理由は返らない |
| 404 | 未知の surface |

401 と 404 のボディは `{"error":{"code":"unauthorized"}}` の形ですが、クライアントは
ステータスだけで分類し、ボディを読みません。

## レスポンス

```json
{
  "surface": "dashboard",
  "version": "2026-06-01",
  "revision": "a1b2c3",
  "theme": "dark",
  "density": "compact",
  "components": [
    {
      "id": "billing-summary",
      "type": "Card",
      "props": { "title": "Billing", "span": 6 },
      "children": [
        { "id": "invoice-list", "type": "List", "props": { "source": "invoices" }, "children": [] }
      ]
    }
  ]
}
```

- `surface` / `version` / `revision`: 必須。欠けている応答は復号失敗として扱う
- `theme`: 任意。`light` / `dark`。未知の値と欠損は `light` として扱う
- `density`: 任意。`comfortable` / `compact`。未知の値と欠損は `comfortable` として扱う
- `components`: 任意。欠損は空として扱う。`children` も同じ
- `id`: 空白のみは値オブジェクトの生成制約により通信失敗として扱う
- 未知のフィールドは無視する（`Json { ignoreUnknownKeys = true }`）

### component の入れ子の上限

`components` から `children` へ続く入れ子は、最上位の component を 1 段目として 32 段までを受け付けます。
33 段以上の応答は復号前に例外として拒否し、通信失敗（`MainUiStatus.FAILED`）として扱います。
極端に深い入れ子で `StackOverflowError` が発生し、`catch` で捕捉できずにプロセスが終了することを防ぐための、クライアント側の制限です。

### 未知の component type

`type` が `Card` / `List` / `Chart` / `Button` / `Banner` / `Panel` 以外の component は、
mapper がその要素だけを落とします。子要素も同じ規則で再帰的に落とします。
サーバーが新しい `type` を追加しても、古いアプリが落ちないことを前提にした契約です。

### props の扱い

`props` は component の `type` ごとに異なる自由形状です。
クライアントは JSON の primitive だけを `Map<String, String>` として取り込み、
object、配列、`null` の値は落とします。数値と真偽値は文字列表現になります。

表示に使う prop は `type` ごとに決まっており、対応する prop が無い場合は `id` を表示します。

| type | 表示に使う prop |
| --- | --- |
| `Card` | `title` |
| `Panel` | `title`、無ければ `source` |
| `List` | `source` |
| `Chart` | `metric` |
| `Button` | `label` |
| `Banner` | `tone` |

### revision と ETag

同じユーザーでも権限や設定が変われば `revision` は同じまま `ETag` が変わります。
キャッシュの一致判定には `ETag` だけを使い、`revision` は表示にのみ使います。

## ベース URL

- 既定値は `gradle.properties` の `API_BASE_URL` に置く。現在の値は dev の実 API
- 既定値を持たず、未設定または空の場合はビルドを失敗させる
- 一時的に差し替える場合だけ Gradle プロパティで上書きする
- 末尾の `/` はビルド時に補完される
- 値は注入せず、Retrofit を組み立てる `NetworkModule` と、接続先を UI state へ載せる `MainViewModel` が
  `BuildConfig.API_BASE_URL` を直接参照する

```powershell
.\gradlew.bat :app:assembleDebug "-PAPI_BASE_URL=http://10.0.2.2:8080/api/"
```

画面が表示された時点で取得を始めるため、初期状態は `MainUiStatus.LOADING` です。
取得に失敗した状態（`SURFACE_NOT_FOUND` / `FAILED`）では、接続先を画面へ表示します。
接続先の誤りとサーバー側の不具合を、画面だけで区別できるようにするためです。

`Authorization` ヘッダーは、`https` で、host と port がベース URL と一致するリクエストにだけ付けます。
ベース URL が `http` の場合や別の host へのリクエストでは、トークンの取得も行わず、ヘッダーなしで送ります。

`debug` ビルドだけが、`10.0.2.2` と `localhost` への平文 HTTP 通信を許可します
（`app/src/debug/res/xml/network_security_config.xml`）。
それ以外の host と `release` ビルドでは平文を許可しません。

## 応答の分類

応答は次の5つに分類し、それぞれ別の UI state へ変換します。

| 分類 | 条件 | domain 表現 | UI state の status |
| --- | --- | --- | --- |
| 解決済み | HTTP 200 | `UiSchemaResolution.Resolved` | `MainUiStatus.LOADED` |
| 変更なし | HTTP 304 | `UiSchemaResolution.Resolved`（保持中のスキーマ） | `MainUiStatus.LOADED` |
| 認証されない | HTTP 401 | `UiSchemaResolution.Unauthorized` | `MainUiStatus.UNAUTHORIZED` |
| 未知の surface | HTTP 404 | `UiSchemaResolution.SurfaceNotFound` | `MainUiStatus.SURFACE_NOT_FOUND` |
| 通信失敗 | その他の HTTP エラー、復号失敗、入れ子の上限超過、接続失敗、値オブジェクトの制約違反 | `Result.failure` | `MainUiStatus.FAILED` |

401 と 404 は契約に書かれた結果のため、`Result.failure` ではなく値として扱います。

## 条件付き取得

Repository は surface ごとに直前の `ETag` と解決済みスキーマを保持し、次の要求で
`If-None-Match` として送ります。304 が返った場合は保持中のスキーマを返します。
保持は プロセス内のメモリだけで、永続化は導入していません。プロセスが再生成されると
最初の要求は無条件取得に戻ります。

保持中の `ETag` とスキーマは、サインアウト時に破棄します。サインアウトが失敗した場合も破棄し、
別のアカウントへ前のアカウントの `ETag` を送りません。サインアウトより前に始まった取得の応答は、
サインアウト後に完了しても保持しません。

保持しているスキーマが無い状態で 304 を受け取った場合は、返せるスキーマが無いため通信失敗として扱います。

## 変換の経路

```
JSON 文字列 -> JsonElement -> UiSchemaDto -> UiSchema -> UiSchemaResolution -> MainUiState
 (data/remote)  (data/remote)  (data/model)  (data/mapper)  (data/repository)   (ui/viewmodel)
```

- JSON 文字列 / `JsonElement`: `SdUiApi` は応答ボディを復号せず `ResponseBody` で返す。
  `UiSchemaRemoteDataSource` が、括弧の入れ子（128 段まで）、`JsonElement` への解析、
  `components` / `children` の入れ子（32 段まで）の順に反復処理で検査し、上限内の場合だけ `UiSchemaDto` へ復号する。
  JSON 解析と DTO への復号はどちらも呼び出しスタック上で再帰するため、いずれも検査を済ませてから行う
- `UiSchemaDto` / `UiComponentDto`: サーバー応答をそのまま写した DTO。`props` は `JsonObject`
- `UiSchema` / `UiComponent`: 未知 type を除き、`props` を文字列へ寄せた domain model
- `UiSchemaResolution`: 契約上の結果（解決済み / 認証されない / 未知の surface）を表す sealed interface
- `MainUiState` / `DashboardUiModel`: 画面表示の分岐（`MainUiStatus`）と表示内容を表す UI 契約。ViewModel がコンストラクタで直接組み立てる

エンドポイントやフィールドを増やすときも、この分担を保ちます。

## 未実装のもの

以下は未実装です。実装済みとして扱わず、必要になった時点で導入します。

- release ビルド用の redirect URI 登録（現在は debug キーストア由来の値だけを持つ）
- `props` の object / 配列 / `null` 値の保持
- `Button` などの component が持つ action の実行
- `ETag` と解決済みスキーマの永続化
- OkHttp の `Cache` による `Cache-Control` の尊重（現在は毎回サーバーへ確認する）

## 契約を変更するとき

1. `sd-ui-infra` の `docs/client_integration.md` を確認し、本書のエンドポイントと応答の分類を先に更新する
2. DTO、mapper、domain model、Repository、UI state のどこまで影響するかを判定する
3. fake の Interceptor と MockK を使う JVM Unit Test で、成功系・欠損フィールド・条件付き取得・HTTP エラーを押さえる
4. 表示文言を変える場合は `strings.xml` を更新する
