import { Client } from 'ssh2';

const CONFIG = {
  host: process.env.DEPLOY_HOST || '',
  port: parseInt(process.env.DEPLOY_PORT || '22', 10),
  username: process.env.DEPLOY_USERNAME || 'root',
  password: process.env.DEPLOY_PASSWORD || ''
};

function sshExec(conn, cmd) {
  return new Promise((resolve, reject) => {
    conn.exec(cmd, (err, stream) => {
      if (err) return reject(err);
      let out = '', errOut = '';
      stream.on('data', d => out += d.toString())
            .stderr.on('data', d => errOut += d.toString());
      stream.on('close', code => {
        if (code !== 0) reject(new Error('Exit ' + code + ': ' + (errOut || out)));
        else resolve(out.trim());
      });
    });
  });
}

function scpUploadContent(conn, content, remote) {
  return new Promise((resolve, reject) => {
    conn.sftp((err, sftp) => {
      if (err) return reject(err);
      const ws = sftp.createWriteStream(remote);
      ws.on('close', resolve);
      ws.on('error', reject);
      ws.end(content);
    });
  });
}

const script = `#!/usr/bin/env bash
# Memory cleanup script - drop page cache, dentries, inodes
LOG=/var/log/mem-cleanup.log
echo "$(date) - Memory cleanup" >> $LOG
echo "Before:" >> $LOG
free -h >> $LOG
sync
echo 3 > /proc/sys/vm/drop_caches
echo "After:" >> $LOG
free -h >> $LOG
echo "---" >> $LOG
`;

async function main() {
  const conn = new Client();
  await new Promise((resolve, reject) => {
    conn.on('ready', () => { console.log('SSH connected.'); resolve(); })
        .on('error', reject)
        .connect(CONFIG);
  });
  try {
    console.log('=== Current Memory ===');
    console.log(await sshExec(conn, 'free -h'));

    console.log('Writing cleanup script...');
    await scpUploadContent(conn, script, '/root/clean-mem.sh');
    await sshExec(conn, 'chmod +x /root/clean-mem.sh');

    console.log('Setting up cron job (every 6 hours)...');
    let existing = '';
    try { existing = await sshExec(conn, 'crontab -l'); } catch {}
    if (!existing.includes('clean-mem.sh')) {
      const newCron = (existing ? existing.trim() + '\n' : '') + '0 */6 * * * /root/clean-mem.sh\n';
      await scpUploadContent(conn, newCron, '/tmp/crontab_new');
      await sshExec(conn, 'crontab /tmp/crontab_new && rm /tmp/crontab_new');
      console.log('Cron job added.');
    } else {
      console.log('Cron job already exists.');
    }

    console.log('Running cleanup now...');
    await sshExec(conn, '/root/clean-mem.sh');

    console.log('=== Memory After Cleanup ===');
    console.log(await sshExec(conn, 'free -h'));

    console.log('\nCron schedule:');
    console.log(await sshExec(conn, 'crontab -l'));
  } finally {
    conn.end();
  }
}

main().catch(e => { console.error('FAILED:', e.message); process.exit(1); });
