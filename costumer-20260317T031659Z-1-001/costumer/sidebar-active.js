function logout() {
  localStorage.removeItem('va_pilot_current_user');
  sessionStorage.removeItem('va_pilot_current_user');
  window.location.href = 'login.html';
}

document.addEventListener('DOMContentLoaded', () => {
  const nav = document.querySelector('aside nav');
  if (!nav) return;

  const links = nav.querySelectorAll('a[href]');
  const current = window.location.pathname.split('/').pop().toLowerCase();
  const aliasMap = {
    'payment.html': 'orders.html'
  };
  const target = aliasMap[current] || current;

  const activeClasses = [
    'bg-brand-pink/10','text-brand-pink','border','border-brand-pink/20','shadow-[0_0_15px_rgba(224,0,255,0.2)]',
    'bg-brand-blue/10','text-brand-blue','border-brand-blue/20','shadow-[0_0_15px_rgba(59,130,246,0.15)]',
    'bg-brand-purple/10','text-brand-purple','border-brand-purple/20','shadow-[0_0_15px_rgba(122,0,255,0.1)]',
    'border-brand-pink/20','text-gray-400','hover:text-white','hover:bg-white/5','font-medium','font-semibold'
  ];

  links.forEach((link) => {
    link.classList.remove(...activeClasses);
    link.classList.add(
      'flex','items-center','gap-3','px-4','py-3','rounded-xl','transition-all',
      'text-gray-400','hover:text-white','hover:bg-white/5','font-medium'
    );

    const href = (link.getAttribute('href') || '').toLowerCase();
    if (href === target) {
      link.classList.remove('text-gray-400','hover:text-white','hover:bg-white/5','font-medium');
      link.classList.add(
        'bg-brand-pink/10','text-brand-pink','border','border-brand-pink/20',
        'shadow-[0_0_15px_rgba(224,0,255,0.2)]','font-semibold'
      );
    }
  });
});
