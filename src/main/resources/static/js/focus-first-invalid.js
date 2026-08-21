(() => {
  const invalid = document.querySelector('[autofocus], .input-error');
  if (!invalid) return;
  invalid.focus({preventScroll: true});
  invalid.scrollIntoView({block: 'center', behavior: 'smooth'});
})();
