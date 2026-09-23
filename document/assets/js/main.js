/**
 * main.js
 * ハンズオン教材ページの最小限の挙動:
 *   - 目次(TOC)の自動生成（h2/h3 から）+ 現在位置ハイライト
 *   - コードブロックのコピー ボタン
 *   - ライト/ダークテーマ切り替え（localStorage に保存）
 *   - 狭幅時のサイドバー開閉
 * フレームワーク不使用の vanilla JS。
 */
(function () {
  "use strict";

  // ---------------------------------------------------------
  // 目次 (TOC) 自動生成
  // ---------------------------------------------------------
  function buildToc() {
    var tocList = document.querySelector("[data-toc-list]");
    var content = document.querySelector(".lesson-content");
    if (!tocList || !content) return;

    var headings = content.querySelectorAll("h2, h3");
    if (headings.length === 0) return;

    var usedIds = Object.create(null);
    var rootUl = document.createElement("ul");
    rootUl.className = "toc__list";
    var currentH2Li = null;
    var currentH2Ul = null;

    headings.forEach(function (heading) {
      if (!heading.id) {
        var base = slugify(heading.textContent);
        var id = base;
        var i = 1;
        while (usedIds[id]) {
          id = base + "-" + i++;
        }
        usedIds[id] = true;
        heading.id = id;
      }

      var li = document.createElement("li");
      var a = document.createElement("a");
      a.className = "toc__link";
      a.href = "#" + heading.id;
      a.textContent = heading.textContent;
      a.dataset.tocTarget = heading.id;
      li.appendChild(a);

      if (heading.tagName === "H2") {
        rootUl.appendChild(li);
        currentH2Li = li;
        currentH2Ul = null;
      } else {
        // h3: 直前の h2 の子として入れ子にする。h2 が無い場合はルート直下に置く。
        if (currentH2Li) {
          if (!currentH2Ul) {
            currentH2Ul = document.createElement("ul");
            currentH2Li.appendChild(currentH2Ul);
          }
          currentH2Ul.appendChild(li);
        } else {
          rootUl.appendChild(li);
        }
      }
    });

    tocList.appendChild(rootUl);
    highlightTocOnScroll(content);
  }

  function slugify(text) {
    return (
      String(text)
        .trim()
        .toLowerCase()
        .replace(/[^\w぀-ヿ㐀-鿿\s-]/g, "")
        .replace(/\s+/g, "-") || "section"
    );
  }

  // 現在位置のハイライト（IntersectionObserver 使用、未対応環境ではスキップ）
  function highlightTocOnScroll(content) {
    if (!("IntersectionObserver" in window)) return;

    var headings = Array.prototype.slice.call(content.querySelectorAll("h2, h3"));
    if (headings.length === 0) return;

    var links = document.querySelectorAll(".toc__link");
    var linkMap = {};
    links.forEach(function (link) {
      linkMap[link.dataset.tocTarget] = link;
    });

    var observer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          var link = linkMap[entry.target.id];
          if (!link) return;
          if (entry.isIntersecting) {
            links.forEach(function (l) {
              l.classList.remove("is-active");
            });
            link.classList.add("is-active");
          }
        });
      },
      { rootMargin: "-20% 0px -70% 0px", threshold: 0 }
    );

    headings.forEach(function (heading) {
      observer.observe(heading);
    });
  }

  // ---------------------------------------------------------
  // コードブロック コピー ボタン
  // ---------------------------------------------------------
  function initCodeCopyButtons() {
    var buttons = document.querySelectorAll(".code-block__copy");
    buttons.forEach(function (button) {
      button.addEventListener("click", function () {
        var block = button.closest(".code-block");
        if (!block) return;
        var codeEl = block.querySelector("pre code, pre");
        if (!codeEl) return;

        var text = codeEl.innerText;

        copyText(text).then(function (ok) {
          var original = button.dataset.label || button.textContent;
          button.textContent = ok ? "コピーしました" : "コピー失敗";
          button.classList.toggle("is-copied", ok);
          window.setTimeout(function () {
            button.textContent = original;
            button.classList.remove("is-copied");
          }, 1800);
        });
      });
    });
  }

  function copyText(text) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      return navigator.clipboard.writeText(text).then(
        function () {
          return true;
        },
        function () {
          return false;
        }
      );
    }
    // フォールバック
    try {
      var textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.style.position = "fixed";
      textarea.style.opacity = "0";
      document.body.appendChild(textarea);
      textarea.select();
      var ok = document.execCommand("copy");
      document.body.removeChild(textarea);
      return Promise.resolve(ok);
    } catch (e) {
      return Promise.resolve(false);
    }
  }

  // ---------------------------------------------------------
  // テーマ切り替え（ライト/ダーク）
  // ---------------------------------------------------------
  var THEME_KEY = "handson-theme";

  function initThemeToggle() {
    var toggle = document.querySelector("[data-theme-toggle]");
    var root = document.documentElement;

    var saved = getSavedTheme();
    if (saved) {
      root.setAttribute("data-theme", saved);
    }
    updateToggleLabel(toggle, currentTheme(root));

    if (!toggle) return;

    toggle.addEventListener("click", function () {
      var next = currentTheme(root) === "dark" ? "light" : "dark";
      root.setAttribute("data-theme", next);
      saveTheme(next);
      updateToggleLabel(toggle, next);
    });
  }

  function currentTheme(root) {
    var attr = root.getAttribute("data-theme");
    if (attr) return attr;
    return window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light";
  }

  function updateToggleLabel(toggle, theme) {
    if (!toggle) return;
    toggle.textContent = theme === "dark" ? "☀️ ライト" : "🌙 ダーク";
    toggle.setAttribute("aria-pressed", theme === "dark" ? "true" : "false");
  }

  function getSavedTheme() {
    try {
      return window.localStorage.getItem(THEME_KEY);
    } catch (e) {
      return null;
    }
  }

  function saveTheme(theme) {
    try {
      window.localStorage.setItem(THEME_KEY, theme);
    } catch (e) {
      // localStorage が使えない環境では無視する
    }
  }

  // ---------------------------------------------------------
  // 狭幅時のサイドバー開閉
  // ---------------------------------------------------------
  function initTocToggle() {
    var toggle = document.querySelector("[data-toc-toggle]");
    var sidebar = document.querySelector(".layout__sidebar");
    if (!toggle || !sidebar) return;

    toggle.addEventListener("click", function () {
      var isOpen = sidebar.classList.toggle("is-open");
      toggle.setAttribute("aria-expanded", isOpen ? "true" : "false");
    });

    // TOC リンクをクリックしたら閉じる（狭幅時）
    sidebar.addEventListener("click", function (event) {
      if (event.target.closest(".toc__link")) {
        sidebar.classList.remove("is-open");
        toggle.setAttribute("aria-expanded", "false");
      }
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    initThemeToggle();
    buildToc();
    initCodeCopyButtons();
    initTocToggle();

    if (window.hljs) {
      document.querySelectorAll("pre code").forEach(function (block) {
        window.hljs.highlightElement(block);
      });
    }
  });
})();
