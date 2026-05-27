import { Client } from 'ssh2';

const CONFIG = { host: '123.60.31.79', port: 22, username: 'root', password: 'Yxw172707' };

function sshExec(conn, cmd) {
  return new Promise((resolve, reject) => {
    conn.exec(cmd, (err, stream) => {
      if (err) return reject(err);
      let out = '', errOut = '';
      stream.on('data', d => out += d.toString()).stderr.on('data', d => errOut += d.toString());
      stream.on('close', code => resolve(out || errOut));
    });
  });
}

function scpContent(conn, content, remote) {
  return new Promise((resolve, reject) => {
    conn.sftp((err, sftp) => {
      if (err) return reject(err);
      const ws = sftp.createWriteStream(remote);
      ws.on('close', resolve); ws.on('error', reject); ws.end(content);
    });
  });
}

const caddyfile = `yexwyu.xyz {
    reverse_proxy 127.0.0.1:8080
}

www.yexwyu.xyz {
    redir https://yexwyu.xyz{uri}
}
`;

async function main() {
  const conn = new Client();
  await new Promise((r, j) => { conn.on('ready', r).on('error', j).connect(CONFIG); });
  try {
    console.log('Writing Caddyfile...');
    await scpContent(conn, caddyfile, '/etc/caddy/Caddyfile');

    console.log('Validating config...');
    console.log(await sshExec(conn, 'caddy validate --config /etc/caddy/Caddyfile 2>&1'));

    console.log('Enabling and starting Caddy...');
    console.log(await sshExec(conn, 'systemctl enable caddy && systemctl restart caddy 2>&1'));

    console.log('Status:');
    console.log(await sshExec(conn, 'systemctl is-active caddy'));

    console.log('Listening ports:');
    console.log(await sshExec(conn, 'ss -tlnp | grep -E "443|80 "'));
  } finally { conn.end(); }
}

main().catch(e => { console.error(e.message); process.exit(1); });
