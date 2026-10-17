import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';
import { SUPABASE_URL, SUPABASE_ANON_KEY } from './config.js';

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

export async function isAdmin() {
  try {
    const { data: { session }, error: sessionError } = await supabase.auth.getSession();
    if (sessionError || !session || !session.user) {
      return false;
    }
    const userId = session.user.id;
    const { data, error } = await supabase
      .from('admins')
      .select('user_id')
      .eq('user_id', userId)
      .maybeSingle();

    if (error || !data) {
      return false;
    }
    return true;
  } catch (err) {
    console.error('Error checking admin status:', err);
    return false;
  }
}

export async function getSetting(key, fallback) {
  try {
    const { data, error } = await supabase
      .from('site_settings')
      .select('value')
      .eq('key', key)
      .maybeSingle();

    if (error || !data || data.value === undefined || data.value === null) {
      return fallback;
    }
    return data.value;
  } catch (err) {
    console.warn(`Failed to fetch setting ${key}, using fallback.`, err);
    return fallback;
  }
}
