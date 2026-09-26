import type { User } from "../types";

const API_BASE = "https://jsonplaceholder.typicode.com";

type JsonPlaceholderUser = {
  id: number;
  name: string;
  username: string;
};

/**
 * ユーザー一覧を取得する。オプション省略（auto no cache）。
 * Data Cache は使わないが、静的ルートではビルド時に 1 回だけ取得される。
 */
export async function fetchUsers(): Promise<User[]> {
  const res = await fetch(`${API_BASE}/users`);

  if (!res.ok) {
    throw new Error(`ユーザー一覧の取得に失敗しました (status: ${res.status})`);
  }

  const users: JsonPlaceholderUser[] = await res.json();

  return users.map((user) => ({
    id: user.id,
    name: user.name,
    username: user.username,
  }));
}
