import { supabase } from '../../js/supabase.js';
import { showToast, showConfirm } from './toast.js';

export async function initCrud(containerId, options) {
  const container = document.getElementById(containerId);
  if (!container) return;

  const {
    tableName,
    title,
    columns, // [{key, label, type ('text'|'number'|'boolean'|'select'|'textarea'|'file'|'url'), options, required, min, max}]
    defaultValues = {},
    validateRow = null
  } = options;

  let state = {
    data: [],
    search: '',
    showInactive: false,
    selectedIds: new Set(),
    page: 1,
    pageSize: 10,
    editingId: null
  };

  container.innerHTML = `
    <div class="card">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 15px;">
        <h2>${title}</h2>
        <div style="display: flex; gap: 10px; flex-wrap: wrap;">
          <input type="text" id="crud-search" placeholder="Search..." style="width: 200px;">
          <label style="display: flex; align-items: center; gap: 5px; font-weight: normal; cursor: pointer;">
            <input type="checkbox" id="crud-show-inactive"> Show Inactive
          </label>
          <button id="crud-add-btn" class="btn">➕ Add New</button>
          <button id="crud-export-btn" class="btn btn-outline">📤 Export CSV</button>
          <input type="file" id="crud-import-file" accept=".csv" style="display: none;">
          <button id="crud-import-btn" class="btn btn-outline">📥 Import CSV</button>
        </div>
      </div>

      <div id="bulk-actions" style="display: none; margin-bottom: 15px; background: var(--bg); padding: 10px; border-radius: 8px; align-items: center; gap: 10px; flex-wrap: wrap;">
        <span id="selected-count" style="font-weight: 600;">0 selected</span>
        <button id="bulk-activate" class="btn btn-sm">Activate Selected</button>
        <button id="bulk-deactivate" class="btn btn-sm btn-outline">Deactivate Selected</button>
        <button id="bulk-delete" class="btn btn-sm btn-danger">Delete Selected</button>
        <button id="bulk-delete-all" class="btn btn-sm btn-danger btn-outline">🗑️ Delete ALL Records</button>
      </div>

      <div class="table-responsive">
        <table>
          <thead>
            <tr>
              <th style="width: 40px;"><input type="checkbox" id="select-all"></th>
              ${columns.map(c => `<th>${c.label}</th>`).join('')}
              <th style="width: 120px;">Actions</th>
            </tr>
          </thead>
          <tbody id="crud-tbody">
            <tr><td colspan="${columns.length + 2}" style="text-align: center;">Loading...</td></tr>
          </tbody>
        </table>
      </div>

      <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 20px;">
        <span id="pagination-info" style="color: var(--text-muted); font-size: 0.9rem;"></span>
        <div style="display: flex; gap: 10px;">
          <button id="prev-page" class="btn btn-outline btn-sm">Previous</button>
          <button id="next-page" class="btn btn-outline btn-sm">Next</button>
        </div>
      </div>
    </div>

    <!-- Modal Form -->
    <div id="crud-modal" class="modal-overlay">
      <div class="modal-content">
        <div class="modal-header">
          <h3 id="modal-title">Add / Edit</h3>
          <button class="modal-close" id="modal-close-btn">&times;</button>
        </div>
        <form id="crud-form">
          <div id="modal-fields"></div>
          <div style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 25px;">
            <button type="button" id="modal-cancel" class="btn btn-outline">Cancel</button>
            <button type="submit" class="btn">Save Changes</button>
          </div>
        </form>
      </div>
    </div>
  `;

  async function loadData() {
    try {
      let query = supabase.from(tableName).select('*').order('id', { ascending: false });
      if (!state.showInactive && columns.some(c => c.key === 'is_active')) {
        query = query.eq('is_active', true);
      }
      const { data, error } = await query;
      if (error) throw error;
      state.data = data || [];
      renderTable();
    } catch (err) {
      showToast(`Error loading data: ${err.message}`, 'error');
    }
  }

  function renderTable() {
    const tbody = container.querySelector('#crud-tbody');
    const filtered = state.data.filter(row => {
      if (!state.search) return true;
      return Object.values(row).some(v => String(v).toLowerCase().includes(state.search.toLowerCase()));
    });

    const start = (state.page - 1) * state.pageSize;
    const paginated = filtered.slice(start, start + state.pageSize);

    if (paginated.length === 0) {
      tbody.innerHTML = `<tr><td colspan="${columns.length + 2}" style="text-align: center; color: var(--text-muted);">No records found.</td></tr>`;
      container.querySelector('#pagination-info').textContent = '';
      return;
    }

    tbody.innerHTML = paginated.map(row => `
      <tr>
        <td><input type="checkbox" class="row-select" data-id="${row.id}" ${state.selectedIds.has(row.id) ? 'checked' : ''}></td>
        ${columns.map(c => {
          let val = row[c.key];
          if (c.type === 'boolean') val = val ? '✅' : '❌';
          if (c.type === 'file' && val) val = `<a href="${val}" target="_blank">View File</a>`;
          return `<td style="max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${val !== undefined && val !== null ? val : ''}</td>`;
        }).join('')}
        <td>
          <div style="display: flex; gap: 5px;">
            <button class="btn btn-outline btn-sm edit-btn" data-id="${row.id}" title="Edit">✏️</button>
            <button class="btn btn-outline btn-sm dup-btn" data-id="${row.id}" title="Duplicate">📋</button>
            <button class="btn btn-outline btn-sm del-btn" data-id="${row.id}" title="Delete" style="color: #ef4444;">🗑️</button>
          </div>
        </td>
      </tr>
    `).join('');

    container.querySelector('#pagination-info').textContent = `Showing ${start + 1} to ${Math.min(start + state.pageSize, filtered.length)} of ${filtered.length} entries`;

    // Attach row events
    tbody.querySelectorAll('.row-select').forEach(cb => {
      cb.onchange = (e) => {
        const id = parseInt(e.target.dataset.id);
        if (e.target.checked) state.selectedIds.add(id);
        else state.selectedIds.delete(id);
        updateBulkBar();
      };
    });

    tbody.querySelectorAll('.edit-btn').forEach(btn => {
      btn.onclick = () => openModal(parseInt(btn.dataset.id));
    });

    tbody.querySelectorAll('.dup-btn').forEach(btn => {
      btn.onclick = () => duplicateRow(parseInt(btn.dataset.id));
    });

    tbody.querySelectorAll('.del-btn').forEach(btn => {
      btn.onclick = () => deleteRow(parseInt(btn.dataset.id));
    });
  }

  function updateBulkBar() {
    const bar = container.querySelector('#bulk-actions');
    const countEl = container.querySelector('#selected-count');
    if (state.selectedIds.size > 0) {
      bar.style.display = 'flex';
      countEl.textContent = `${state.selectedIds.size} selected`;
    } else {
      bar.style.display = 'none';
    }
  }

  // Search & Filters
  container.querySelector('#crud-search').oninput = (e) => {
    state.search = e.target.value;
    state.page = 1;
    renderTable();
  };

  container.querySelector('#crud-show-inactive').onchange = (e) => {
    state.showInactive = e.target.checked;
    loadData();
  };

  container.querySelector('#select-all').onchange = (e) => {
    if (e.target.checked) {
      state.data.forEach(r => state.selectedIds.add(r.id));
    } else {
      state.selectedIds.clear();
    }
    renderTable();
    updateBulkBar();
  };

  // Bulk Actions Event Handlers
  container.querySelector('#bulk-delete').onclick = () => {
    if (state.selectedIds.size === 0) return;
    showConfirm(`Delete ${state.selectedIds.size} selected records?`, async () => {
      try {
        const { error } = await supabase.from(tableName).delete().in('id', Array.from(state.selectedIds));
        if (error) throw error;
        showToast('Selected records deleted successfully!');
        state.selectedIds.clear();
        loadData();
      } catch (err) {
        showToast(`Bulk delete failed: ${err.message}`, 'error');
      }
    });
  };

  container.querySelector('#bulk-delete-all').onclick = () => {
    showConfirm(`WARNING: Are you sure you want to DELETE ALL records in ${tableName}? This cannot be undone!`, async () => {
      try {
        const { error } = await supabase.from(tableName).delete().gte('id', 0);
        if (error) throw error;
        showToast('All records deleted successfully!');
        state.selectedIds.clear();
        loadData();
      } catch (err) {
        showToast(`Delete all failed: ${err.message}`, 'error');
      }
    });
  };

  container.querySelector('#bulk-activate').onclick = async () => {
    if (state.selectedIds.size === 0) return;
    try {
      const { error } = await supabase.from(tableName).update({ is_active: true }).in('id', Array.from(state.selectedIds));
      if (error) throw error;
      showToast('Selected records activated!');
      loadData();
    } catch (err) {
      showToast(`Action failed: ${err.message}`, 'error');
    }
  };

  container.querySelector('#bulk-deactivate').onclick = async () => {
    if (state.selectedIds.size === 0) return;
    try {
      const { error } = await supabase.from(tableName).update({ is_active: false }).in('id', Array.from(state.selectedIds));
      if (error) throw error;
      showToast('Selected records deactivated!');
      loadData();
    } catch (err) {
      showToast(`Action failed: ${err.message}`, 'error');
    }
  };

  // Modal handlers
  const modal = container.querySelector('#crud-modal');
  container.querySelector('#crud-add-btn').onclick = () => openModal(null);
  container.querySelector('#modal-close-btn').onclick = () => modal.classList.remove('active');
  container.querySelector('#modal-cancel').onclick = () => modal.classList.remove('active');

  function openModal(id) {
    state.editingId = id;
    container.querySelector('#modal-title').textContent = id ? `Edit ${title}` : `Add ${title}`;
    const fieldsContainer = container.querySelector('#modal-fields');
    
    const record = id ? state.data.find(r => r.id === id) : defaultValues;

    fieldsContainer.innerHTML = columns.map(c => {
      const val = record ? (record[c.key] !== undefined ? record[c.key] : '') : '';
      if (c.type === 'textarea') {
        return `<div class="form-group"><label>${c.label}</label><textarea name="${c.key}" rows="3" ${c.required ? 'required' : ''}>${val}</textarea></div>`;
      }
      if (c.type === 'boolean') {
        return `<div class="form-group" style="display: flex; align-items: center; gap: 10px;"><input type="checkbox" name="${c.key}" ${val ? 'checked' : ''} style="width: auto;"><label style="margin-bottom:0;">${c.label}</label></div>`;
      }
      if (c.type === 'select') {
        return `<div class="form-group"><label>${c.label}</label><select name="${c.key}" ${c.required ? 'required' : ''}>${c.options.map(o => `<option value="${o}" ${val === o ? 'selected' : ''}>${o}</option>`).join('')}</select></div>`;
      }
      if (c.type === 'file') {
        return `<div class="form-group"><label>${c.label}</label><input type="file" name="${c.key}-file"><input type="hidden" name="${c.key}" value="${val}"> ${val ? `<br><a href="${val}" target="_blank">Current file</a>` : ''}</div>`;
      }
      return `<div class="form-group"><label>${c.label}</label><input type="${c.type || 'text'}" name="${c.key}" value="${val}" ${c.required ? 'required' : ''} ${c.min !== undefined ? `min="${c.min}"` : ''} ${c.max !== undefined ? `max="${c.max}"` : ''}></div>`;
    }).join('');

    modal.classList.add('active');
  }

  container.querySelector('#crud-form').onsubmit = async (e) => {
    e.preventDefault();
    const formData = new FormData(e.target);
    const payload = {};

    for (const c of columns) {
      if (c.type === 'boolean') {
        payload[c.key] = e.target.querySelector(`[name="${c.key}"]`).checked;
      } else if (c.type === 'number') {
        payload[c.key] = formData.get(c.key) !== '' ? Number(formData.get(c.key)) : null;
      } else if (c.type === 'file') {
        const fileInput = e.target.querySelector(`[name="${c.key}-file"]`);
        if (fileInput && fileInput.files && fileInput.files[0]) {
          const file = fileInput.files[0];
          const fileName = `${Date.now()}_${file.name}`;
          const bucket = tableName === 'bill_links' ? 'logos' : 'apk';
          const { data: uploadData, error: uploadErr } = await supabase.storage.from(bucket).upload(fileName, file);
          if (uploadErr) {
            showToast(`File upload failed: ${uploadErr.message}`, 'error');
            return;
          }
          const { data: { publicUrl } } = supabase.storage.from(bucket).getPublicUrl(fileName);
          payload[c.key] = publicUrl;
        } else {
          payload[c.key] = formData.get(c.key);
        }
      } else {
        payload[c.key] = formData.get(c.key);
      }
    }

    // Extra validations
    if (tableName === 'tariff_slabs') {
      if (payload.from_unit > payload.to_unit) {
        showToast('Validation Error: from_unit must be <= to_unit', 'error');
        return;
      }
    }
    if (tableName === 'package_codes') {
      const ussdRegex = /^[*#0-9]+$/;
      if (payload.ussd_code && !ussdRegex.test(payload.ussd_code)) {
        showToast('Validation Error: USSD code must match ^[*#0-9]+$', 'error');
        return;
      }
    }
    if (tableName === 'helplines') {
      const phoneRegex = /^[0-9+\-\s]+$/;
      if (payload.number && !phoneRegex.test(payload.number)) {
        showToast('Validation Error: Helpline number can only contain digits, +, -, spaces', 'error');
        return;
      }
    }
    if (columns.some(c => c.type === 'url' || c.key === 'url')) {
      for (const c of columns) {
        if ((c.type === 'url' || c.key === 'url') && payload[c.key] && !payload[c.key].startsWith('https://')) {
          showToast(`Validation Error: ${c.label} must start with https://`, 'error');
          return;
        }
      }
    }

    if (validateRow) {
      const err = validateRow(payload, state.data, state.editingId);
      if (err) {
        showToast(err, 'error');
        return;
      }
    }

    try {
      if (state.editingId) {
        const { error } = await supabase.from(tableName).update(payload).eq('id', state.editingId);
        if (error) throw error;
        showToast('Record updated successfully! (Note: content_version bumped by DB)');
      } else {
        const { error } = await supabase.from(tableName).insert([payload]);
        if (error) throw error;
        showToast('Record added successfully!');
      }
      modal.classList.remove('active');
      loadData();
    } catch (err) {
      showToast(`Save failed: ${err.message}`, 'error');
    }
  };

  async function deleteRow(id) {
    showConfirm('Are you sure you want to delete this record?', async () => {
      try {
        const { error } = await supabase.from(tableName).delete().eq('id', id);
        if (error) throw error;
        showToast('Record deleted.');
        loadData();
      } catch (err) {
        showToast(`Delete failed: ${err.message}`, 'error');
      }
    });
  }

  async function duplicateRow(id) {
    const row = state.data.find(r => r.id === id);
    if (!row) return;
    const copy = { ...row };
    delete copy.id;
    if (copy.created_at) delete copy.created_at;
    try {
      const { error } = await supabase.from(tableName).insert([copy]);
      if (error) throw error;
      showToast('Record duplicated successfully!');
      loadData();
    } catch (err) {
      showToast(`Duplication failed: ${err.message}`, 'error');
    }
  }

  // CSV Export
  container.querySelector('#crud-export-btn').onclick = () => {
    if (state.data.length === 0) {
      showToast('No data to export', 'error');
      return;
    }
    const keys = columns.map(c => c.key);
    const csvRows = [keys.join(',')];
    for (const row of state.data) {
      csvRows.push(keys.map(k => `"${String(row[k] !== undefined ? row[k] : '').replace(/"/g, '""')}"`).join(','));
    }
    const blob = new Blob([csvRows.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${tableName}_export.csv`;
    a.click();
    showToast('CSV exported successfully!');
  };

  // CSV Import
  const importFile = container.querySelector('#crud-import-file');
  container.querySelector('#crud-import-btn').onclick = () => importFile.click();
  importFile.onchange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (event) => {
      const text = event.target.result;
      const lines = text.split('\n').filter(l => l.trim() !== '');
      if (lines.length < 2) {
        showToast('CSV file is empty or missing header', 'error');
        return;
      }
      const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''));
      const batch = [];
      for (let i = 1; i < lines.length; i++) {
        const values = lines[i].split(',').map(v => v.trim().replace(/^"|"$/g, ''));
        const obj = {};
        headers.forEach((h, idx) => {
          obj[h] = values[idx] !== undefined ? values[idx] : '';
        });
        batch.push(obj);
      }
      showConfirm(`Import ${batch.length} rows into ${tableName}?`, async () => {
        try {
          const { error } = await supabase.from(tableName).insert(batch);
          if (error) throw error;
          showToast(`Successfully imported ${batch.length} rows!`);
          loadData();
        } catch (err) {
          showToast(`Import failed: ${err.message}`, 'error');
        }
      });
    };
    reader.readAsText(file);
  };

  loadData();
}
