(function () {
  const STORAGE_KEY = "webpage-lang";
  const supported = ["de", "en"];

  function preferredLanguage() {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (supported.includes(saved)) {
      return saved;
    }

    const browser = (navigator.language || "de").toLowerCase();
    return browser.startsWith("en") ? "en" : "de";
  }

  function attrKey(base, lang) {
    return `${base}${lang === "de" ? "De" : "En"}`;
  }

  function readAttrValue(el, base, lang) {
    return el.dataset[attrKey(base, lang)];
  }

  function applyLang(lang) {
    document.documentElement.lang = lang;
    localStorage.setItem(STORAGE_KEY, lang);

    const body = document.body;
    if (body && body.dataset.titleDe && body.dataset.titleEn) {
      document.title = lang === "de" ? body.dataset.titleDe : body.dataset.titleEn;
    }

    document.querySelectorAll("[data-i18n-de][data-i18n-en]").forEach((el) => {
      el.textContent = lang === "de" ? el.dataset.i18nDe : el.dataset.i18nEn;
    });

    document.querySelectorAll("[data-i18n-html-de][data-i18n-html-en]").forEach((el) => {
      el.innerHTML = lang === "de" ? el.dataset.i18nHtmlDe : el.dataset.i18nHtmlEn;
    });

    document.querySelectorAll("[data-i18n-placeholder-de][data-i18n-placeholder-en]").forEach((el) => {
      const value = lang === "de" ? el.dataset.i18nPlaceholderDe : el.dataset.i18nPlaceholderEn;
      el.setAttribute("placeholder", value);
    });

    document.querySelectorAll("[data-i18n-content-de][data-i18n-content-en]").forEach((el) => {
      const value = lang === "de" ? el.dataset.i18nContentDe : el.dataset.i18nContentEn;
      el.setAttribute("content", value);
    });

    document.querySelectorAll("[data-lang-switch]").forEach((btn) => {
      const active = btn.dataset.langSwitch === lang;
      btn.classList.toggle("active", active);
      btn.setAttribute("aria-pressed", active ? "true" : "false");
    });
  }

  function initSwitches() {
    const initialLang = preferredLanguage();

    document.querySelectorAll("[data-lang-switch]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const nextLang = btn.dataset.langSwitch;
        if (supported.includes(nextLang)) {
          applyLang(nextLang);
        }
      });
    });

    applyLang(initialLang);
  }

  document.addEventListener("DOMContentLoaded", initSwitches);
})();
