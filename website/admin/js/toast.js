export function showToast(message, type = 'success') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${type === 'success' ? '✅' : '❌'}</span> <div>${message}</div>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

export function showConfirm(message, onConfirm) {
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay active';
  overlay.innerHTML = `
    <div class="modal-content" style="max-width: 400px; text-align: center;">
      <h3 style="margin-bottom: 15px;">Confirm Action</h3>
      <p style="color: var(--text-muted); margin-bottom: 25px;">${message}</p>
      <div style="display: flex; gap: 10px; justify-content: center;">
        <button id="confirm-no" class="btn btn-outline">Cancel</button>
        <button id="confirm-yes" class="btn btn-danger">Confirm</button>
      </div>
    </div>
  `;
  document.body.appendChild(overlay);

  overlay.querySelector('#confirm-no').onclick = () => overlay.remove();
  overlay.querySelector('#confirm-yes').onclick = () => {
    overlay.remove();
    onConfirm();
  };
}
