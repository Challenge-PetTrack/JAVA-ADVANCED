document.querySelectorAll('[data-gerar-relatorio]').forEach(form => {
    form.addEventListener('submit', event => {
        if (form.dataset.enviando === 'true') { event.preventDefault(); return; }
        form.dataset.enviando = 'true';
        form.setAttribute('aria-busy', 'true');
        const button = form.querySelector('button[type="submit"]');
        button.disabled = true;
        button.textContent = 'Gerando relatório…';
        form.querySelector('[role="status"]').textContent = 'Aguarde a geração e o salvamento do relatório. Isso pode levar alguns segundos.';
    });
});
window.addEventListener('pageshow', () => {
    document.querySelectorAll('[data-gerar-relatorio]').forEach(form => {
        form.dataset.enviando = 'false';
        form.removeAttribute('aria-busy');
        const button = form.querySelector('button[type="submit"]');
        button.disabled = false;
        button.textContent = 'Gerar novo relatório';
        form.querySelector('[role="status"]').textContent = '';
    });
});
document.querySelectorAll('[data-imprimir-relatorio]').forEach(button => {
    button.hidden = false;
    button.addEventListener('click', () => window.print());
});
