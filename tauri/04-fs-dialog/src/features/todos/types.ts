// Rust 側の #[serde(rename_all = "camelCase")] と対応する型。
// created_at は rename_all により createdAt として届く。
export type Todo = {
  id: number;
  title: string;
  done: boolean;
  createdAt: number;
};
