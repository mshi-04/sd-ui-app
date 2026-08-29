---
name: ui-implementation
description: 画面の表示、操作、遷移、表示文言を変える依頼で使う。SD UI の UI 層の Compose screen、Route、ViewModel、UI state、strings.xml を担当する。
---

# 目的

SD UI の UI 層を、既存の Compose / MVVM 構成に沿って変更する。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md)
- [`docs/unit-test.md`](../../../docs/unit-test.md)
- [`app/src/main/java/com/example/sdui/ui/`](../../../app/src/main/java/com/example/sdui/ui/)
- [`app/src/main/res/values/strings.xml`](../../../app/src/main/res/values/strings.xml)
- 必要に応じて [`app/src/androidTest/java/com/example/sdui/ui/`](../../../app/src/androidTest/java/com/example/sdui/ui/)

# 役割クラスと参照先

変更対象の役割クラスを見分け、該当する reference だけを読んでから実装する。
薄い役割は reference を作らず docs に委ねる。

| 役割クラス | 参照 |
| --- | --- |
| screen（`XxxRoute` / `XxxScreen`） | [`references/screen.md`](references/screen.md) |
| viewmodel | [`references/viewmodel.md`](references/viewmodel.md) |
| UI state | [`references/ui-state.md`](references/ui-state.md) |
| theme / Preview / strings | reference なし。`docs/implementation-guidelines.md` の UI / Localization 方針に従う |
| navigation | reference なし。Navigation Compose は未導入のため、必要になった時点で導入を提案する |

# 手順

1. 変更対象の役割クラスを判定し、該当 reference を読む。
2. `ui -> domain <- data` の依存方向と、ViewModel 経由の domain アクセスを保つ。
3. reference の観点に沿って実装する。
4. 変更に応じて ViewModel test または Compose UI Test の要否を判断する。
5. 変更内容に該当する検証をすべて実行する。Kotlin 変更は `.\gradlew.bat :app:testDebugUnitTest`、UI / resource 変更は `.\gradlew.bat :app:lintDebug`、Compose UI Test は `.\gradlew.bat :app:connectedDebugAndroidTest` で検証し、複数に該当するときは各 task を組み合わせる。task 一覧の正本は [`docs/development-setup.md`](../../../docs/development-setup.md)。
6. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- `ui` から DTO、Retrofit API、Repository implementation を直接参照しない。
- UI 変更が Domain、Data へ波及するときは、変更対象に対応する Skill を併用する。
- ViewModel に Android View や Context 依存を直接持ち込まない。
- 固定色の直書きを避け、Material 3 theme とライト / ダーク対応を優先する。
- 表示文字列は `strings.xml` へ寄せ、書式指定子付きの resource で値を埋め込む。
