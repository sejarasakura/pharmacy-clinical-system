(() => {
  const toggles = document.querySelectorAll('[data-password-toggle]');
  if (!toggles.length) return;
  toggles.forEach((toggle) => toggle.addEventListener('click', () => {
    const input = document.getElementById(toggle.dataset.passwordToggle);
    if (!input) return;
    const reveal = input.type === 'password';
    input.type = reveal ? 'text' : 'password';
    toggle.textContent = reveal ? 'Hide' : 'Show';
    toggle.setAttribute('aria-pressed', String(reveal));
  }));
})();
