(() => {
  const shell = document.querySelector('.app-shell');
  if (!shell) return;
  shell.classList.add('is-enhanced');

  const modal = document.querySelector('[data-modal]');
  if (!modal) return;

  const backgroundRegions = shell.querySelectorAll('.sidebar, .topbar');
  backgroundRegions.forEach((region) => region.setAttribute('inert', ''));

  const focusableSelector = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
  const focusable = () => Array.from(modal.querySelectorAll(focusableSelector));
  const initialFocus = modal.querySelector('[autofocus]') || focusable()[0];
  if (initialFocus) initialFocus.focus();

  modal.addEventListener('keydown', (event) => {
    const form = modal.querySelector('form');
    if (event.key === 'Escape' && form?.getAttribute('aria-busy') !== 'true') {
      const cancel = modal.querySelector('[data-modal-cancel]');
      if (cancel) cancel.click();
      return;
    }
    if (event.key !== 'Tab') return;
    const items = focusable();
    if (!items.length) return;
    const first = items[0];
    const last = items[items.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  });
})();
