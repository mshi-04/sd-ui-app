# UseCase

サーバー応答や保存結果を業務上の意味へ解釈する境界を UseCase に置く。

## 確認する対象

- `domain/` の現在の構成（UseCase は置いておらず、ViewModel が Repository interface に依存する）
- 依存する Repository interface の戻り値（`Result` を含む）と、呼び出し元の ViewModel

## 実装時の注意

- 既存の書き方にそろえ、`suspend operator fun invoke()` で1つの操作を表す。
- Repository が返す `Result` は `map` / `fold` で扱い、成功値だけを解釈する。
- 失敗をここで握りつぶさず、`Result` のまま呼び出し元へ返す。
- 判定に使う定数は `private companion object` に置き、意味の分かる名前を付ける。
- ビジネスルール、複数 Repository の調停、複数画面からの再利用がない単純委譲は UseCase 化しない。

## テスト判断

- 正常系、境界（判定条件の別表記や未知の値）、失敗系を JVM Unit Test で押さえる。
- Repository は private な fake で差し替える。

## 検証観点

- 判定ルールが UI 側に重複していないか。
- 失敗の理由が呼び出し元で区別できるか。
