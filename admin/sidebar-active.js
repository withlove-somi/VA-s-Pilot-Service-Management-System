document.addEventListener('DOMContentLoaded', () => {
  const nav = document.querySelector('aside nav');
  if (!nav) return;

  const links = nav.querySelectorAll('a[href]');
  const current = window.location.pathname.split('/').pop().toLowerCase();
  const aliasMap = {
    'admin-chat.html': 'admin-inbox.html',
    'admin-customer-profile.html': 'admin-inbox.html'
  };
  const target = aliasMap[current] || current;

  const classesToStrip = [
    'bg-brand-purple/10','text-brand-purple','border','border-brand-purple/20','shadow-[0_0_15px_rgba(122,0,255,0.1)]',
    'text-gray-400','hover:text-white','hover:bg-white/5','font-medium','font-semibold'
  ];

  links.forEach((link) => {
    link.classList.remove(...classesToStrip);
    link.classList.add(
      'flex','items-center','gap-3','px-4','py-3','rounded-xl','transition-all',
      'text-gray-400','hover:text-white','hover:bg-white/5','font-medium'
    );

    const href = (link.getAttribute('href') || '').toLowerCase();
    if (href === target) {
      link.classList.remove('text-gray-400','hover:text-white','hover:bg-white/5','font-medium');
      link.classList.add(
        'bg-brand-purple/10','text-brand-purple','border','border-brand-purple/20',
        'shadow-[0_0_15px_rgba(122,0,255,0.1)]','font-semibold'
      );
    }
  });
});
