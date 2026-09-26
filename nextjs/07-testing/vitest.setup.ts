// unit プロジェクト（jsdom）専用のセットアップファイル。
// @testing-library/jest-dom の toBeInTheDocument() などのカスタムマッチャーを追加する。
import "@testing-library/jest-dom/vitest";

import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

// test.globals を true にしていない（globals は使わず describe/it/expect を明示 import する方針）ため、
// @testing-library/react が Jest/Vitest のグローバル afterEach を検出できず、
// render() したコンポーネントが次のテストの DOM に残ってしまう。
// そのため cleanup() をここで手動で afterEach に登録する。
afterEach(() => {
  cleanup();
});
