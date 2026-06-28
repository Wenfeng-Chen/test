/** 全 HTTP 方法压测 */
const MASTER = process.env.MASTER_URL || 'http://localhost:8080';
const BASE = `${MASTER}/api/demo/resource`;

const plan = {
  name: 'all-http-methods',
  mode: 'STANDALONE',
  load: { concurrency: 10, durationSeconds: 10 },
  requests: [
    { method: 'GET', url: BASE },
    { method: 'POST', url: BASE, headers: { 'Content-Type': 'application/json' }, body: '{"k":"v"}' },
    { method: 'PUT', url: BASE, headers: { 'Content-Type': 'application/json' }, body: '{"k":"v"}' },
    { method: 'PATCH', url: BASE, headers: { 'Content-Type': 'application/json' }, body: '{"k":"v"}' },
    { method: 'DELETE', url: BASE },
    { method: 'HEAD', url: BASE },
    { method: 'OPTIONS', url: BASE },
  ]
};

fetch(`${MASTER}/api/tasks`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(plan)
})
  .then(r => r.json())
  .then(result => {
    const taskId = result.data.taskId;
    console.log(`[ALL_METHODS] taskId=${taskId}, dashboard=${MASTER}/index.html`);
  })
  .catch(console.error);
