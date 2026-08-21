(() => {
  const controls = document.querySelectorAll('[data-filter-clear]');
  if (!controls.length) return;
  controls.forEach((control) => control.addEventListener('click', () => {
    const form = document.getElementById(control.dataset.filterClear);
    if (!form) return;
    form.querySelectorAll('input:not([type="hidden"]), select').forEach((field) => {
      field.value = '';
    });
    form.requestSubmit ? form.requestSubmit() : form.submit();
  }));
})();
