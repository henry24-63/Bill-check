export function initAdminNav(activePageKey) {
  const sidebar = document.createElement('aside');
  sidebar.innerHTML = `
    <div class="sidebar-brand">
      ⚡ Admin Portal
    </div>
    <ul class="sidebar-menu">
      <li><a href="/admin/" ${activePageKey === 'dashboard' ? 'class="active"' : ''}>📊 Dashboard</a></li>
      <li><a href="/admin/app-config.html" ${activePageKey === 'app-config' ? 'class="active"' : ''}>🚀 App Update & Config</a></li>
      <li><a href="/admin/announcements.html" ${activePageKey === 'announcements' ? 'class="active"' : ''}>📢 Announcements</a></li>
      <li><a href="/admin/feature-flags.html" ${activePageKey === 'feature-flags' ? 'class="active"' : ''}>🚩 Feature Flags</a></li>
      <li><a href="/admin/ads-control.html" ${activePageKey === 'ads-control' ? 'class="active"' : ''}>💰 Ads Control</a></li>
      <li><a href="/admin/site-settings.html" ${activePageKey === 'site-settings' ? 'class="active"' : ''}>⚙️ Site Settings</a></li>
      <li><a href="/admin/tariff-tester.html" ${activePageKey === 'tariff-tester' ? 'class="active"' : ''}>🧮 Tariff Tester</a></li>
      <li><a href="/admin/feedback.html" ${activePageKey === 'feedback' ? 'class="active"' : ''}>💬 Feedback Inbox</a></li>
      <li><a href="/admin/audit-log.html" ${activePageKey === 'audit-log' ? 'class="active"' : ''}>📜 Audit Log</a></li>
      <li><a href="/admin/backup-restore.html" ${activePageKey === 'backup-restore' ? 'class="active"' : ''}>💾 Backup & Restore</a></li>
      <li style="border-top: 1px solid var(--border); margin: 10px 0;"></li>
      <li><a href="/admin/bill-links.html" ${activePageKey === 'bill-links' ? 'class="active"' : ''}>🔗 Bill Links</a></li>
      <li><a href="/admin/helplines.html" ${activePageKey === 'helplines' ? 'class="active"' : ''}>☎️ Helplines</a></li>
      <li><a href="/admin/package-codes.html" ${activePageKey === 'package-codes' ? 'class="active"' : ''}>📱 Package Codes</a></li>
      <li><a href="/admin/tariff-slabs.html" ${activePageKey === 'tariff-slabs' ? 'class="active"' : ''}>📊 Tariff Slabs</a></li>
      <li><a href="/admin/tariff-charges.html" ${activePageKey === 'tariff-charges' ? 'class="active"' : ''}>💵 Tariff Charges</a></li>
      <li><a href="/admin/appliances.html" ${activePageKey === 'appliances' ? 'class="active"' : ''}>⚡ Appliances</a></li>
      <li><a href="/admin/tips.html" ${activePageKey === 'tips' ? 'class="active"' : ''}>💡 Energy Tips</a></li>
      <li style="border-top: 1px solid var(--border); margin: 10px 0;"></li>
      <li><a href="/admin/account.html" ${activePageKey === 'account' ? 'class="active"' : ''}>👤 Account & Auth</a></li>
      <li><a href="/" target="_blank">🌐 View Live Site</a></li>
    </ul>
  `;
  document.body.insertBefore(sidebar, document.body.firstChild);

  // Topbar
  const mainContent = document.querySelector('.main-content');
  if (mainContent) {
    const topbar = document.createElement('div');
    topbar.className = 'topbar';
    topbar.innerHTML = `
      <div class="topbar-title">Admin Management</div>
      <div class="topbar-actions">
        <button id="theme-toggle" class="btn btn-outline btn-sm">🌙</button>
        <button id="logout-btn" class="btn btn-danger btn-sm">Logout</button>
      </div>
    `;
    mainContent.insertBefore(topbar, mainContent.firstChild);

    topbar.querySelector('#logout-btn').onclick = async () => {
      const { supabase } = await import('../../js/supabase.js');
      await supabase.auth.signOut();
      window.location.replace('/admin/login.html');
    };

    const themeBtn = topbar.querySelector('#theme-toggle');
    const currentTheme = localStorage.getItem('app_theme') || 'light';
    document.documentElement.setAttribute('data-theme', currentTheme);
    themeBtn.textContent = currentTheme === 'dark' ? '☀️' : '🌙';
    themeBtn.onclick = () => {
      const cur = document.documentElement.getAttribute('data-theme');
      const next = cur === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      localStorage.setItem('app_theme', next);
      themeBtn.textContent = next === 'dark' ? '☀️' : '🌙';
    };
  }
}
