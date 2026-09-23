import { fetchUsers } from "@/features/users/api/fetchUsers";
import styles from "./AuthorList.module.scss";

// async コンポーネント。fetch が終わるまでの間、page.tsx 側の Suspense fallback が表示され、
// 準備できたこの部分だけが後からストリーミングで差し込まれる。
export async function AuthorList() {
  const users = await fetchUsers();

  return (
    <aside className={styles.authors}>
      <p className={styles.authorsTitle}>投稿者一覧</p>
      <ul className={styles.authorsList}>
        {users.map((user) => (
          <li key={user.id} className={styles.authorsItem}>
            {user.name}（@{user.username}）
          </li>
        ))}
      </ul>
    </aside>
  );
}
