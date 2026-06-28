/** 示例：通过 Node.js 脚本提交压测任务 */
const MASTER = process.env.MASTER_URL || 'http://localhost:8080';

const plan = {
  name: 'demo-ping-test',
  mode: 'DISTRIBUTED',
  load: { concurrency: 50, durationSeconds: 15 },
  requests: [
    { method: 'GET', url: `${MASTER}/api/demo/ping` }
  ]
};

fetch(`${MASTER}/api/tasks`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(plan)
})
  .then(r => r.json())
  .then(result => {
    console.log('Task submitted:', result);
    const taskId = result.data.taskId;
    console.log(`Dashboard: ${MASTER}/index.html  Task ID: ${taskId}`);
  })
  .catch(console.error);
