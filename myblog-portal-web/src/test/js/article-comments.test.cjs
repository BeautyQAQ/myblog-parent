// Run with Node.js: node myblog-portal-web/src/test/js/article-comments.test.cjs
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/js/article-comments.js'), 'utf8');

class Element {
    constructor(tag = 'div') { this.tagName = tag; this.children = []; this.attributes = {}; this.listeners = {}; this.disabled = false; this.value = ''; this._text = ''; }
    set textContent(value) { this._text = String(value); this.children = []; }
    get textContent() { return this._text + this.children.map(child => child.textContent).join(''); }
    setAttribute(name, value) { this.attributes[name] = String(value); }
    getAttribute(name) { return this.attributes[name] || null; }
    append(...nodes) { this.children.push(...nodes); }
    replaceChildren(...nodes) { this._text = ''; this.children = nodes; }
    addEventListener(name, listener) { this.listeners[name] = listener; }
    fire(name) { this.listeners[name]({preventDefault() {}}); }
}
function setup(member = true, randomUuidAvailable = true) {
    const ids = ['article-comments', 'comments-message', 'comments-total', 'article-comment-count', 'comments-list', 'comments-pagination', 'comments-refresh'];
    if (member) ids.push('comment-form', 'my-comments', 'comment-content', 'comment-submit', 'comment-submit-message', 'my-comments-message', 'my-comments-list', 'my-comments-pagination');
    const nodes = Object.fromEntries(ids.map(id => [id, new Element()]));
    nodes['article-comments'].attributes = {'data-public-url': '/articles/42/comments', 'data-delete-base': '/comments/'};
    if (member) { nodes['my-comments'].attributes['data-url'] = '/articles/42/comments/mine'; nodes['comment-form'].action = '/articles/42/comments'; }
    const requests = [];
    let nextKey = 0;
    let nextTimer = 0;
    const timers = new Map();
    const sandbox = {
        document: {getElementById: id => nodes[id] || null, createElement: tag => new Element(tag), querySelector: () => ({content: 'csrf'})},
        window: {crypto: randomUuidAvailable ? {randomUUID: () => 'request-' + (++nextKey)}
            : {getRandomValues: bytes => { bytes.fill(++nextKey); return bytes; }},
            setTimeout: callback => { timers.set(++nextTimer, callback); return nextTimer; }, clearTimeout: id => timers.delete(id)},
        AbortController, URLSearchParams, Date, Array, TypeError,
        fetch: (url, options) => new Promise((resolve, reject) => {
            const request = {url, options, resolve: data => resolve({ok: true, json: async () => ({status: 200, data})}),
                reject, fail: (status, msg) => resolve({ok: false, json: async () => ({status, msg})})};
            options.signal.addEventListener('abort', () => { const error = new Error('timeout'); error.name = 'AbortError'; reject(error); });
            requests.push(request);
        })
    };
    vm.runInNewContext(source, sandbox);
    return {nodes, requests, timers};
}
function page(comments = [], total = comments.length, pageNum = 1, pages = 1) { return {comments, total, pageNum, pageSize: 10, pages}; }
async function flush() { await new Promise(resolve => setImmediate(resolve)); }
function completeInitial(state, comments = [], own = []) {
    state.requests[0].resolve(page(comments));
    if (state.requests[1]) state.requests[1].resolve(page(own));
}
function posts(state) { return state.requests.filter(request => request.options.method === 'POST'); }
function find(node, predicate) { if (predicate(node)) return node; for (const child of node.children) { const match = find(child, predicate); if (match) return match; } }
async function tests() {
    let passed = 0;
    {
        const state = setup(false);
        const hostile = {id: 3, username: '<img onerror=alert(1)>', content: '<script>alert(1)</script>\n正文', created: 1};
        completeInitial(state, [hostile]); await flush();
        assert.equal(state.requests.length, 1);
        const content = find(state.nodes['comments-list'], node => node.className === 'comment-content');
        assert.equal(content.textContent, hostile.content);
        assert.equal(content.children.length, 0);
        assert.equal(state.nodes['article-comment-count'].textContent, '1');
        passed++;
    }
    {
        const state = setup(); completeInitial(state); await flush();
        const input = state.nodes['comment-content']; const form = state.nodes['comment-form'];
        input.value = '第一条评论'; form.fire('submit'); form.fire('submit');
        assert.equal(posts(state).length, 1); assert.equal(state.nodes['comment-submit'].disabled, true);
        const first = posts(state)[0]; first.fail(503, '稍后重试'); await flush();
        assert.equal(input.value, '第一条评论'); assert.equal(state.nodes['comment-submit'].disabled, false);
        form.fire('submit'); const retry = posts(state)[1];
        assert.equal(retry.options.body, first.options.body);
        retry.resolve({status: 'PENDING'}); await flush();
        assert.equal(input.value, '');
        assert.match(state.nodes['comment-submit-message'].textContent, /等待管理员审核/);
        assert.equal(state.nodes['comment-submit'].disabled, false);
        passed++;
    }
    {
        const state = setup(); completeInitial(state); await flush();
        const input = state.nodes['comment-content']; const form = state.nodes['comment-form'];
        input.value = '旧内容'; form.fire('submit'); const first = posts(state)[0]; first.fail(503, '重试'); await flush();
        input.value = '新内容'; input.fire('input'); form.fire('submit');
        assert.notEqual(new URLSearchParams(first.options.body).get('requestId'), new URLSearchParams(posts(state)[1].options.body).get('requestId'));
        posts(state)[1].resolve({status: 'DELETED'}); await flush();
        assert.match(state.nodes['comment-submit-message'].textContent, /已删除/);
        assert.doesNotMatch(state.nodes['comment-submit-message'].textContent, /等待管理员审核/);
        passed++;
    }
    {
        const state = setup(); completeInitial(state, [], [{id: 8, content: '我的评论', username: '我', status: 'APPROVED'}]); await flush();
        const remove = find(state.nodes['my-comments-list'], node => node.tagName === 'button');
        remove.fire('click'); remove.fire('click'); assert.equal(posts(state).length, 1);
        assert.equal(posts(state)[0].url, '/comments/8/delete'); assert.equal(posts(state)[0].options.body, '');
        posts(state)[0].resolve({status: 'DELETED'}); await flush();
        state.requests.slice(3).forEach(request => request.resolve(page([]))); await flush();
        assert.equal(state.nodes['article-comment-count'].textContent, '0');
        assert.match(state.nodes['my-comments-list'].textContent, /还没有/);
        passed++;
    }
    {
        const state = setup(false); completeInitial(state); await flush();
        state.nodes['comments-refresh'].fire('click'); const older = state.requests[1];
        state.nodes['comments-refresh'].fire('click'); const newer = state.requests[2];
        newer.resolve(page([{content: '最新结果', username: 'a'}])); await flush();
        older.resolve(page([{content: '旧结果', username: 'b'}])); await flush();
        assert.match(state.nodes['comments-list'].textContent, /最新结果/); assert.doesNotMatch(state.nodes['comments-list'].textContent, /旧结果/);
        passed++;
    }
    {
        const state = setup(true, false); completeInitial(state); await flush();
        state.nodes['comment-content'].value = '局域网 HTTP 评论'; state.nodes['comment-form'].fire('submit');
        const first = posts(state)[0];
        const key = new URLSearchParams(first.options.body).get('requestId');
        assert.match(key, /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/);
        first.fail(503, '重试'); await flush(); state.nodes['comment-form'].fire('submit');
        assert.equal(new URLSearchParams(posts(state)[1].options.body).get('requestId'), key);
        passed++;
    }
    {
        const state = setup(); completeInitial(state); await flush();
        const input = state.nodes['comment-content']; input.value = '😀'.repeat(1001); state.nodes['comment-form'].fire('submit');
        assert.equal(posts(state).length, 0);
        input.value = '😀'.repeat(1000); state.nodes['comment-form'].fire('submit');
        assert.equal(posts(state).length, 1);
        [...state.timers.values()].forEach(callback => callback()); await flush();
        assert.equal(state.nodes['comment-submit'].disabled, false); assert.equal(input.value, '😀'.repeat(1000));
        assert.match(state.nodes['comment-submit-message'].textContent, /请求未完成/);
        passed++;
    }
    return passed;
}
tests().then(count => console.log(count + ' comment browser-script behavior checks passed')).catch(error => { console.error(error); process.exitCode = 1; });
