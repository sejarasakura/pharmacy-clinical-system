(() => {
  const projections = document.querySelectorAll('[data-projected-balance]');
  if (!projections.length) return;
  projections.forEach((projection) => {
    const [currentId, inputId, outputId] = projection.dataset.projectedBalance.split(':');
    const current = document.getElementById(currentId);
    const input = document.getElementById(inputId);
    const output = document.getElementById(outputId);
    if (!current || !input || !output) return;
    const update = () => {
      const value = Number(current.value) + Number(input.value || 0);
      output.textContent = Number.isFinite(value) ? String(value) : '—';
    };
    input.addEventListener('input', update);
    update();
  });
})();
