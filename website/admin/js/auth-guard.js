import { supabase, isAdmin } from '../../js/supabase.js';

export async function initAuthGuard() {
  document.body.classList.add('auth-pending');

  try {
    const adminStatus = await isAdmin();
    if (!adminStatus) {
      window.location.replace('/');
      return;
    }

    document.body.classList.remove('auth-pending');
    initInactivityTimer();
  } catch (err) {
    console.error('Auth guard verification failed:', err);
    window.location.replace('/');
  }
}

function initInactivityTimer() {
  let inactivityTimeout;
  const timeoutMs = 30 * 60 * 1000; // 30 minutes

  async function logoutUser() {
    await supabase.auth.signOut();
    alert('Session expired due to inactivity.');
    window.location.replace('/admin/login.html');
  }

  function resetTimer() {
    clearTimeout(inactivityTimeout);
    inactivityTimeout = setTimeout(logoutUser, timeoutMs);
  }

  window.addEventListener('mousemove', resetTimer);
  window.addEventListener('keypress', resetTimer);
  window.addEventListener('click', resetTimer);
  window.addEventListener('scroll', resetTimer);

  resetTimer();
}
