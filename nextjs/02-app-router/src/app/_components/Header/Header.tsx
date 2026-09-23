"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import styles from "./Header.module.scss";

const NAV_ITEMS = [
  { href: "/", label: "Home" },
  { href: "/about", label: "About" },
  { href: "/blog", label: "Blog" },
] as const;

export function Header() {
  const pathname = usePathname();

  return (
    <header className={styles.header}>
      <nav className={styles.nav}>
        <ul className={styles.list}>
          {NAV_ITEMS.map((item) => {
            const isActive =
              item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);

            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={isActive ? `${styles.link} ${styles.linkActive}` : styles.link}
                  aria-current={isActive ? "page" : undefined}
                >
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
    </header>
  );
}
