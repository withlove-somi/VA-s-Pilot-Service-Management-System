document.addEventListener('DOMContentLoaded', () => {
    const threadEmail = resolveUserEmail();
    if (!threadEmail) return;

    const chatHTML = `
        <div id="global-chat-widget" class="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-4 pointer-events-none font-sans text-white">
            <div id="chat-window" class="w-96 h-[500px] bg-[#071822]/95 backdrop-blur-xl border border-cyan-400/20 rounded-2xl shadow-[0_0_40px_rgba(0,0,0,0.8)] flex flex-col pointer-events-auto transition-all duration-300 origin-bottom-right scale-0 opacity-0">
                <div class="p-4 border-b border-white/10 flex justify-between items-center bg-black/20 rounded-t-2xl">
                    <div class="flex items-center gap-2">
                        <div class="w-2 h-2 rounded-full bg-green-400 animate-pulse"></div>
                        <h3 class="font-semibold text-sm tracking-wide">Customer Support</h3>
                    </div>
                    <button id="close-chat" class="text-gray-400 hover:text-white transition-colors">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
                    </button>
                </div>

                <div id="chat-messages" class="flex-1 p-4 overflow-y-auto space-y-4 flex flex-col scroll-smooth"></div>

                <form id="chat-form" class="p-4 border-t border-white/10 bg-black/20 rounded-b-2xl flex gap-3">
                    <input type="text" id="chat-input" placeholder="Type message..." class="flex-1 bg-black/40 border border-white/10 rounded-xl px-4 py-3 text-sm text-white focus:outline-none focus:border-cyan-400 transition-colors">
                    <button type="submit" id="send-btn" class="w-12 h-12 rounded-xl bg-cyan-500 text-white flex items-center justify-center hover:bg-cyan-400 transition-colors shadow-[0_0_10px_rgba(34,211,238,0.35)]">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5 ml-1"><path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" /></svg>
                    </button>
                </form>
            </div>

            <button id="chat-toggle" class="w-16 h-16 rounded-full bg-gradient-to-r from-cyan-500 to-blue-500 flex items-center justify-center text-white shadow-[0_0_20px_rgba(34,211,238,0.45)] hover:shadow-[0_0_30px_rgba(34,211,238,0.65)] hover:-translate-y-1 transition-all pointer-events-auto">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-8 h-8"><path stroke-linecap="round" stroke-linejoin="round" d="M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 01-2.555-.337A5.972 5.972 0 015.41 20.97a5.969 5.969 0 01-.474-.065 4.48 4.48 0 00.978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25z" /></svg>
            </button>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', chatHTML);

    const chatToggleBtn = document.getElementById('chat-toggle');
    const chatCloseBtn = document.getElementById('close-chat');
    const chatWindow = document.getElementById('chat-window');
    const chatInput = document.getElementById('chat-input');
    const chatMessages = document.getElementById('chat-messages');
    const chatForm = document.getElementById('chat-form');
    const API_BASE = 'https://va-s-pilot-service-management-system-production-d6a0.up.railway.app';
    const seenMessageIds = new Set();

    async function apiJson(path, options = {}) {
        const response = await fetch(`${API_BASE}${path}`, {
            headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
            ...options
        });
        let data = {};
        try {
            data = await response.json();
        } catch (e) {
            data = {};
        }
        if (!response.ok || data.ok === false) {
            throw new Error(data.error || `Request failed (${response.status})`);
        }
        return data;
    }

    function toggleChat() {
        chatWindow.classList.toggle('scale-0');
        chatWindow.classList.toggle('opacity-0');
        chatWindow.classList.toggle('scale-100');
        chatWindow.classList.toggle('opacity-100');
        if (!chatWindow.classList.contains('scale-0')) {
            chatInput.focus();
            refreshThread();
        }
    }

    function resolveUserEmail() {
        const bodyEmail = document.body?.dataset?.userEmail;
        const nodeEmail = document.querySelector('[data-user-email]')?.dataset?.userEmail;
        const inputEmail = document.getElementById('user-email')?.value;
        const displayEmail = document.getElementById('user-email-display')?.textContent;
        const globalEmail = window.currentUserEmail || window.currentUser?.email;
        const email = String(bodyEmail || nodeEmail || inputEmail || displayEmail || globalEmail || '').trim().toLowerCase();
        return email || '';
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    function normalizeMessageId(message) {
        if (!message) return '';
        const id = message._id || message.id || '';
        return String(id || '').trim();
    }

    function renderThread(thread) {
        if (!thread.length) {
            chatMessages.innerHTML = `
                <div class="flex flex-col items-start gap-1 w-full">
                    <span class="text-[10px] text-gray-500 font-bold uppercase tracking-wider ml-1">System</span>
                    <div class="bg-white/10 border border-white/5 rounded-2xl rounded-tl-sm px-4 py-2 text-sm max-w-[85%]">
                        Welcome. Send a message and admin will reply here.
                    </div>
                </div>
            `;
            return;
        }

        chatMessages.innerHTML = '';
        thread.forEach((msg) => {
            appendMessage(msg);
        });
    }

    function appendMessage(msg) {
        if (!msg) return;
        const messageId = normalizeMessageId(msg);
        if (messageId && seenMessageIds.has(messageId)) return;
        if (messageId) seenMessageIds.add(messageId);

        const mine = String(msg.sender || '').toLowerCase() === 'customer';
        const messageText = msg.message || msg.text || '';
        const row = document.createElement('div');
        row.className = `flex flex-col ${mine ? 'items-end' : 'items-start'} gap-1 w-full`;
        row.innerHTML = `
            <span class="text-[10px] ${mine ? 'text-cyan-300 mr-1' : 'text-gray-500 ml-1'} font-bold uppercase tracking-wider">${mine ? 'You' : 'Admin'}</span>
            <div class="${mine ? 'bg-gradient-to-r from-cyan-500 to-blue-500 rounded-tr-sm' : 'bg-white/10 border border-white/5 rounded-tl-sm'} rounded-2xl px-4 py-2 text-sm max-w-[85%]">${escapeHtml(messageText)}</div>
        `;
        chatMessages.appendChild(row);
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    async function loadThread() {
        const payload = await apiJson(`/api/messages?userEmail=${encodeURIComponent(threadEmail)}`);
        return payload.messages || [];
    }

    async function refreshThread() {
        try {
            const thread = await loadThread();
            renderThread(thread);
        } catch (err) {
            console.error('Failed to load chat thread:', err);
        }
    }

    function loadSocketIo() {
        return new Promise((resolve, reject) => {
            if (window.io) return resolve(window.io);
            const script = document.createElement('script');
            script.src = `${API_BASE}/socket.io/socket.io.js`;
            script.onload = () => resolve(window.io);
            script.onerror = reject;
            document.head.appendChild(script);
        });
    }

    async function connectSocket() {
        try {
            const ioClient = await loadSocketIo();
            const socket = ioClient(API_BASE, { transports: ['websocket'], withCredentials: true });
            socket.on('connect', () => {
                socket.emit('joinChat', threadEmail);
            });
            socket.on('newMessage', (msg) => {
                if (!msg) return;
                if (String(msg.userEmail || '').trim().toLowerCase() !== threadEmail) return;
                appendMessage(msg);
            });
        } catch (err) {
            console.error('Socket.io failed to connect:', err);
        }
    }

    async function sendMessage() {
        const text = chatInput.value.trim();
        if (!text) return;

        try {
            await apiJson('/api/messages', {
                method: 'POST',
                body: JSON.stringify({ userEmail: threadEmail, sender: 'customer', message: text })
            });
            chatInput.value = '';
            await refreshThread();
        } catch (err) {
            console.error('Failed to send chat message:', err);
        }
    }

    chatToggleBtn.addEventListener('click', toggleChat);
    chatCloseBtn.addEventListener('click', toggleChat);
    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await sendMessage();
    });

    refreshThread();
    connectSocket();
});
