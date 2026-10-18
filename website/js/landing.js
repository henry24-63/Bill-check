import { supabase, isAdmin, getSetting } from './supabase.js';
import { currentLang, setLang, t, applyTranslations, getCurrentLang } from './i18n.js';

let logoTapCount = 0;
let logoTapTimer = null;

document.addEventListener('DOMContentLoaded', async () => {
  // 1. Theme initialization
  const savedTheme = localStorage.getItem('app_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  const themeToggleBtn = document.getElementById('theme-toggle');
  if (themeToggleBtn) {
    themeToggleBtn.textContent = savedTheme === 'dark' ? '☀️' : '🌙';
    themeToggleBtn.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('app_theme', next);
      themeToggleBtn.textContent = next === 'dark' ? '☀️' : '🌙';
    });
  }

  // 2. Language toggle initialization
  applyTranslations(getCurrentLang());
  const langToggleBtn = document.getElementById('lang-toggle');
  if (langToggleBtn) {
    langToggleBtn.addEventListener('click', () => {
      const current = getCurrentLang();
      const next = current === 'ur' ? 'en' : 'ur';
      setLang(next);
      loadDynamicContent();
    });
  }

  // 3. Secret 5-tap logo handler
  const logoEl = document.getElementById('brand-logo');
  if (logoEl) {
    logoEl.addEventListener('click', (e) => {
      e.preventDefault();
      logoTapCount++;
      if (logoTapCount === 1) {
        logoTapTimer = setTimeout(() => {
          logoTapCount = 0;
        }, 2000);
      }
      if (logoTapCount >= 5) {
        clearTimeout(logoTapTimer);
        logoTapCount = 0;
        window.location.href = '/admin/login.html';
      }
    });
  }

  // 4. Check Admin Status for navbar
  try {
    const adminCheck = await isAdmin();
    if (adminCheck) {
      const navLinks = document.getElementById('nav-links');
      if (navLinks) {
        const adminBtn = document.createElement('a');
        adminBtn.href = '/admin/';
        adminBtn.className = 'btn btn-outline';
        adminBtn.textContent = t('admin_btn');
        navLinks.insertBefore(adminBtn, navLinks.firstChild);
      }
    }
  } catch (err) {
    console.warn('Admin check skipped/failed:', err);
  }

  // 5. Load dynamic site settings & app config
  await loadDynamicContent();
});

async function loadDynamicContent() {
  const isUrdu = getCurrentLang() === 'ur';

  // Hero settings
  const heroTitleKey = isUrdu ? 'hero_title_ur' : 'hero_title';
  const heroTextKey = isUrdu ? 'hero_text_ur' : 'hero_text';
  
  const heroTitle = await getSetting(heroTitleKey, await getSetting('hero_title', t('hero_title_default')));
  const heroText = await getSetting(heroTextKey, await getSetting('hero_text', t('hero_text_default')));

  const heroTitleEl = document.getElementById('hero-title');
  const heroTextEl = document.getElementById('hero-text');
  if (heroTitleEl) heroTitleEl.textContent = heroTitle;
  if (heroTextEl) heroTextEl.textContent = heroText;

  // Disclaimer settings
  const disclaimerKey = isUrdu ? 'disclaimer_ur' : 'disclaimer_en';
  const disclaimerText = await getSetting(disclaimerKey, await getSetting('disclaimer', t('disclaimer_default')));
  const disclaimerEl = document.getElementById('footer-disclaimer');
  if (disclaimerEl) disclaimerEl.textContent = disclaimerText;

  // Install steps
  const installStepsKey = isUrdu ? 'install_steps_ur' : 'install_steps_en';
  const defaultSteps = isUrdu ? 
    `<li>فون کی سیٹنگز (Settings) کھولیں۔</li><li>سکیورٹی یا پرائیویسی (Security / Privacy) میں جائیں۔</li><li>'Install unknown apps' یا 'Unknown sources' کو الاؤ کریں۔</li><li>ڈاؤن لوڈ کی گئی APK فائل پر کلک کرکے انسٹال کریں</li>` :
    `<li>Open your Android phone Settings.</li><li>Navigate to Security or Privacy settings.</li><li>Enable 'Install unknown apps' for your browser or file manager.</li><li>Tap the downloaded APK file and follow prompts to install.</li>`;
  
  const installSteps = await getSetting(installStepsKey, defaultSteps);
  const installStepsEl = document.getElementById('install-steps');
  if (installStepsEl) installStepsEl.innerHTML = installSteps;

  // App Config & Download Button
  try {
    const { data: configData, error } = await supabase
      .from('app_config')
      .select('*')
      .eq('id', 1)
      .maybeSingle();

    if (!error && configData) {
      const versionEl = document.getElementById('app-version');
      const sizeEl = document.getElementById('app-size');
      const changelogEl = document.getElementById('app-changelog');
      const downloadBtn = document.getElementById('download-btn');

      if (versionEl) versionEl.textContent = `${t('version_label')}: ${configData.latest_version_name || '1.0.0'}`;
      if (sizeEl && configData.apk_size_mb) sizeEl.textContent = `${t('size_label')}: ${configData.apk_size_mb} MB`;
      if (changelogEl) changelogEl.textContent = configData.changelog || 'Bug fixes and performance improvements.';

      if (downloadBtn) {
        if (configData.maintenance_mode) {
          downloadBtn.textContent = isUrdu ? 'مینٹیننس موڈ' : 'Under Maintenance';
          downloadBtn.style.opacity = '0.6';
          downloadBtn.style.pointerEvents = 'none';
        } else if (!configData.apk_url) {
          downloadBtn.textContent = t('coming_soon');
          downloadBtn.style.opacity = '0.6';
          downloadBtn.style.pointerEvents = 'none';
        } else {
          downloadBtn.textContent = t('download_btn');
          downloadBtn.href = configData.apk_url;
          downloadBtn.onclick = async (e) => {
            // Silent fail increment stat
            try {
              await supabase.rpc('increment_stat', { p_key: 'apk_download' });
            } catch (rpcErr) {
              console.warn('Stat increment failed:', rpcErr);
            }
          };
        }
      }
    }
  } catch (err) {
    console.warn('Failed to load app_config:', err);
  }

  // 6. Inject dynamic SEO meta tags from Groq AI / Supabase
  await injectSeoMetaTags();
}

/**
 * Dynamically injects SEO Meta Tags, OpenGraph, Twitter Cards, and JSON-LD schema
 * generated by Groq AI into the document head for search crawlers and social shares.
 */
async function injectSeoMetaTags() {
  try {
    const rawSeo = await getSetting('seo_meta_tags', null);
    let seo = null;
    if (rawSeo) {
      try {
        seo = typeof rawSeo === 'string' ? JSON.parse(rawSeo) : rawSeo;
      } catch (e) {
        console.warn('Error parsing seo_meta_tags JSON:', e);
      }
    }

    // Default high-converting fallbacks if not yet generated
    const title = seo?.title || await getSetting('meta_title', 'PowerPulse — Check Pakistan Electricity Bills Online & Utility Hub');
    const description = seo?.description || await getSetting('meta_description', 'Instant online electricity bill checker for LESCO, IESCO, K-Electric, GEPCO, MEPCO, PESCO. Auto-fill 14-digit reference number, appliance calculator & emergency helplines.');
    const keywords = seo?.keywords || await getSetting('meta_keywords', 'electricity bill check, bijli bill online, lesco bill, iesco bill, kelectric bill, gepco bill, mepco bill, pesco bill, electricity unit calculator, pakistan utility bills');
    const ogTitle = seo?.og_title || title;
    const ogDesc = seo?.og_description || description;
    const twitterTitle = seo?.twitter_title || title;
    const twitterDesc = seo?.twitter_description || description;

    // Document Title
    document.title = title;

    // Helper to set or create meta tag
    function setMetaTag(attribute, attrValue, content) {
      if (!content) return;
      let el = document.querySelector(`meta[${attribute}="${attrValue}"]`);
      if (!el) {
        el = document.createElement('meta');
        el.setAttribute(attribute, attrValue);
        document.head.appendChild(el);
      }
      el.setAttribute('content', content);
    }

    setMetaTag('name', 'description', description);
    setMetaTag('name', 'keywords', keywords);
    setMetaTag('name', 'robots', 'index, follow, max-image-preview:large');

    // OpenGraph
    setMetaTag('property', 'og:title', ogTitle);
    setMetaTag('property', 'og:description', ogDesc);
    setMetaTag('property', 'og:type', 'website');
    setMetaTag('property', 'og:url', window.location.origin + window.location.pathname);
    setMetaTag('property', 'og:site_name', 'PowerPulse');

    // Twitter Card
    setMetaTag('name', 'twitter:card', 'summary_large_image');
    setMetaTag('name', 'twitter:title', twitterTitle);
    setMetaTag('name', 'twitter:description', twitterDesc);

    // JSON-LD Structured Data Schema
    let schemaJson = seo?.schema_json;
    if (!schemaJson) {
      schemaJson = {
        '@context': 'https://schema.org',
        '@type': 'WebApplication',
        'name': 'PowerPulse',
        'url': window.location.origin,
        'applicationCategory': 'UtilitiesApplication',
        'operatingSystem': 'Android, Web Browser',
        'description': description,
        'offers': {
          '@type': 'Offer',
          'price': '0',
          'priceCurrency': 'PKR'
        }
      };
    }

    let schemaEl = document.getElementById('seo-json-ld');
    if (!schemaEl) {
      schemaEl = document.createElement('script');
      schemaEl.id = 'seo-json-ld';
      schemaEl.type = 'application/ld+json';
      document.head.appendChild(schemaEl);
    }
    schemaEl.textContent = typeof schemaJson === 'object' ? JSON.stringify(schemaJson) : schemaJson;
  } catch (err) {
    console.warn('Failed to inject dynamic SEO meta tags:', err);
  }
}
