# Unit Test

この文書は SD UI の Unit Test / instrumented test 方針をまとめます。

## 基本方針

- JVM Unit Test（`app/src/test`）は JUnit Jupiter を使う
- instrumented test（`app/src/androidTest`）は JUnit 4 と AndroidX Test を使う
- テスト関数名は英語にする
- 1つのテスト関数では、1つの振る舞いまたは1つのシナリオだけを検証する
- Domain の値オブジェクト、UseCase、Repository interface 境界を優先してテストする

## テスト配置の判断

ライブラリ名ではなく、検証する振る舞いが実 Android 環境を必要とするかで `src/test` と `src/androidTest` を選びます。

JVM Unit Test で優先してテストするもの:

- 値オブジェクトの生成条件と不正値
- UseCase の正常系と失敗系
- Repository interface を境界にした Domain の振る舞い
- mapper の変換ルール
- ViewModel の状態遷移と UI state の生成
- MockWebServer で完結する Retrofit の JSON 変換と HTTP エラーの扱い

instrumented test で検証するもの:

- Compose UI の表示分岐と操作
- Android の Context、ライフサイクルとの統合が必要な振る舞い

後回しにしてよいもの:

- Compose UI の細かい見た目
- まだ仕様が固まっていない画面構成

## 命名

- テストクラス名は対象クラス名に `Test` を付ける
- テスト関数名は英語の camelCase で、検証する振る舞いが分かる名前にする
- 日本語の関数名やバッククォート関数名は使わない

## AAA Comments

- テストは Arrange / Act / Assert の流れで書く
- 基本は `// Arrange`、`// Act`、`// Assert` コメントを入れる
- Arrange が不要な場合は `// Arrange` を省略してよい
- Act と Assert は原則として分け、必要なラベルを別の表現へ置き換えない
- `assertThrows(Xxx::class.java) { ... }` のように、操作と検証が1つの式で構造上交錯する場合に限り `// Act & Assert` を使う
- 検証観点は、`// Act` または `// Act & Assert` の直下に `// <観点>: <意図>` の形で1行添える

## テスト観点（命名とコメント）

テストが「何を検証しているか」を、関数名の接頭辞とコメントで明示します。

| 観点 | 意味 |
|------|------|
| `Normal` | 正常系。期待どおりの入力で期待どおりの結果になる |
| `Boundary` | 境界・特殊入力（空文字、空白のみ、欠損フィールド、no-op など） |
| `Error` | 失敗系。例外の送出や失敗結果の生成 |
| `Interaction` | 依存（Repository など）の呼び出し有無・回数・順序の検証 |
| `Flow` | `StateFlow` / event stream の発火を検証 |
| `Coroutine` | 仮想時間・キャンセルなど coroutine の挙動 |
| `StateTransition` | 操作前後の UI state の遷移 |

適用ルール:

- 関数名は観点を接頭辞に付ける（camelCase）。例: `normalHealthResponseIsDeserialized`、`errorHttpErrorBecomesRepositoryFailure`
- AAA ラベルは残したまま、`// Act` または `// Act & Assert` の直下に観点を1行置く。例: `// Boundary: blank status is rejected`
- 複数観点が絡む場合は `// Boundary/Interaction: ...` のように `/` で連結する
- 観点は検証の主目的に合わせて選び、無理に増やさない

例:

```kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServerStatusCodeTest {
    @Test
    fun normalConstructorReturnsTrimmedValueWhenInputHasSurroundingWhitespace() {
        // Arrange
        val input = "  ok  "

        // Act
        // Normal: surrounding whitespace is removed
        val code = ServerStatusCode(input)

        // Assert
        assertEquals("ok", code.value)
    }
}
```

## Assert

- 1つの振る舞いまたは1つのシナリオを成立させるために必要な、関連する複数の assertion は許容する
- 異なる入力条件、異なる操作、または同じシナリオを共同で説明しない異なる期待結果を検証する場合は、テスト関数を分ける
- assertion 数を1つにするためだけに、`Pair`、`Triple`、比較用 data class などへ値をまとめない
- 対象が自然に data class や値オブジェクトとして比較できる場合は、オブジェクト全体を `assertEquals` で比較する
- 期待値の全フィールドを書くと検証したい差分が埋もれる場合に限り、関係するプロパティだけを個別に検証する
- JVM Unit Test で、同一の実行結果に含まれる複数のプロパティを個別に検証する場合は `assertAll` を使う
- instrumented test / Compose UI Test（JUnit 4）では、同じシナリオを共同で検証する複数の assertion を直接記述してよい

## Coroutine Test

- suspend 関数や Flow を扱う JVM Unit Test は `kotlinx.coroutines.test.runTest` を使う
- `runBlocking` は新規・更新する JVM Unit Test では使わない
- 1つのテストで使う `TestDispatcher` は、同じ `TestCoroutineScheduler` を共有する
- `StandardTestDispatcher` は処理を scheduler にキューイングするため、実行順と仮想時間を明示的に制御したい場合の基本とする
- `UnconfinedTestDispatcher` は即時開始が必要で、実行順が検証対象でない場合だけ意図して選ぶ
- ViewModel の `viewModelScope` を検証するときは、ViewModel 生成前に `Dispatchers.setMain(testDispatcher)` を行い、`@AfterEach` で `Dispatchers.resetMain()` する
- `viewModelScope` の完了を待つ場合は `advanceUntilIdle()` を使う

## Test Double

- モックライブラリは未導入。テスト用の実装は、テストクラスと同じファイル内の private な fake で用意する
- fake は、依存の状態や返却結果を単純に制御したい場合、呼び出し回数を数えたい場合に使う
- Retrofit を跨ぐ経路は MockWebServer で実際の JSON 応答と HTTP エラーを再現し、`@AfterEach` で `shutdown()` する
- Screen が多くの callback を必須引数に持つ場合は、no-op を既定値にした private ヘルパーをテストクラス内に置き、各テストからは検証したい state だけを渡す

## Instrumented Test

instrumented test は JUnit 4 と AndroidX Test の runner / rule を使います。
テスト関数名、AAA コメント、観点 prefix / comment の方針は JVM Unit Test とそろえ、coroutine を扱う場合だけ `runTest` を使います。

Hilt の instrumented test 用の差し替え（`HiltTestRunner` など）は未導入です。
Compose UI Test は `createAndroidComposeRule<ComponentActivity>()` で Composable を直接呼び、DI を経由しません。

## 適用範囲

この方針は新規のテストと、実質的に変更するテストへ適用します。
既存テスト全体は、意図した移行作業でない限り一括変更しません。
現在の既存テストは、バッククォート関数名で AAA コメントを持たない書き方が残っています。

## 実行

実行コマンドは [`docs/development-setup.md`](development-setup.md) を参照します。
