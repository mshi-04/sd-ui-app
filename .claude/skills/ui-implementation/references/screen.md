# screen

`XxxRoute` で ViewModel と画面を接続し、`XxxScreen` は state と callback だけで描画する。

## 確認する対象

- `ui/route/` の対象 Route と、`hiltViewModel()` / `collectAsStateWithLifecycle()` の配線
- `ui/screen/` の対象 Screen と、受け取る `XxxUiState` / callback
- 画面が参照する `strings.xml` の文言

## 実装時の注意

- Route は state 収集と callback の受け渡しに徹し、表示ロジックを持たない。
- Screen は ViewModel、domain model、DTO を引数に取らず、UI state と callback だけを受け取る。
- 表示分岐は `when` で UI state の型を網羅し、`else` で握りつぶさない。
- 表示文字列は `stringResource` から取り、文字列連結で組み立てない。
- `Modifier` は既定値付きの引数として先頭の必須引数の後に置き、呼び出し側から差し替えられるようにする。
- Preview は `@Preview` を付けた private Composable にし、テーマを適用した状態で確認する。

## テスト判断

- 表示分岐と操作可否は Compose UI Test（`app/src/androidTest`）で押さえる。
- callback が多い Screen は、no-op を既定値にした private ヘルパー経由で `setContent` する。

## 検証観点

- 新しい UI state を追加したときに、表示分岐の網羅が漏れていないか。
- ライト / ダーク、長い文言で表示が破綻しないか。
