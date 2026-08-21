(() => {
  const forms = document.querySelectorAll('form[data-submit-lock]');
  if (!forms.length) return;
  forms.forEach((form) => form.addEventListener('submit', () => {
    form.setAttribute('aria-busy', 'true');
    const submit = form.querySelector('[type="submit"]');
    if (submit) submit.disabled = true;
  }));
})();
