import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { Client } from "ssh2";
import { execSync } from "child_process";
import { readFileSync, existsSync, createReadStream } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

// Load .env from same directory
const __dirname = dirname(fileURLToPath(import.meta.url));
const envPath = resolve(__dirname, ".env");
if (existsSync(envPath)) {
  for (const line of readFileSync(envPath, "utf-8").split("\n")) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eqIdx = trimmed.indexOf("=");
    if (eqIdx > 0) {
      const key = trimmed.slice(0, eqIdx).trim();
      const val = trimmed.slice(eqIdx + 1).trim();
      if (!process.env[key]) process.env[key] = val;
    }
  }
}

const PROJECT_ROOT = resolve(__dirname, "../..");
const JAR_NAME = "ai-love-daily-1.0.0.jar";
const JAR_LOCAL = resolve(PROJECT_ROOT, `backend/target/${JAR_NAME}`);
const START_SCRIPT = resolve(PROJECT_ROOT, "deploy/start-backend.sh");
const STOP_SCRIPT = resolve(PROJECT_ROOT, "deploy/stop-backend.sh");

// Default config
const DEFAULTS = {
  host: process.env.DEPLOY_HOST || "123.60.31.79",
  port: parseInt(process.env.DEPLOY_PORT || "22", 10),
  username: process.env.DEPLOY_USERNAME || "root",
  password: process.env.DEPLOY_PASSWORD || "",
  remoteDir: process.env.DEPLOY_REMOTE_DIR || "/root/project",
};

function log(msg) {
  const ts = new Date().toISOString().slice(11, 19);
  process.stderr.write(`[${ts}] ${msg}\n`);
}

// SSH helpers
function sshConnect(config) {
  return new Promise((resolve, reject) => {
    const conn = new Client();
    conn
      .on("ready", () => resolve(conn))
      .on("error", reject)
      .connect({
        host: config.host,
        port: config.port,
        username: config.username,
        password: config.password,
      });
  });
}

function sshExec(conn, cmd) {
  return new Promise((resolve, reject) => {
    conn.exec(cmd, (err, stream) => {
      if (err) return reject(err);
      let stdout = "";
      let stderr = "";
      stream
        .on("data", (d) => (stdout += d.toString()))
        .stderr.on("data", (d) => (stderr += d.toString()));
      stream.on("close", (code) => {
        if (code !== 0) {
          reject(new Error(`Command failed (exit ${code}): ${cmd}\n${stderr || stdout}`));
        } else {
          resolve(stdout.trim());
        }
      });
    });
  });
}

function scpUpload(conn, localPath, remotePath) {
  return new Promise((resolve, reject) => {
    conn.sftp((err, sftp) => {
      if (err) return reject(err);
      const readStream = createReadStream(localPath);
      const writeStream = sftp.createWriteStream(remotePath);
      writeStream.on("close", resolve);
      writeStream.on("error", reject);
      readStream.pipe(writeStream);
    });
  });
}

function scpUploadContent(conn, content, remotePath) {
  return new Promise((resolve, reject) => {
    conn.sftp((err, sftp) => {
      if (err) return reject(err);
      const writeStream = sftp.createWriteStream(remotePath);
      writeStream.on("close", resolve);
      writeStream.on("error", reject);
      writeStream.end(content);
    });
  });
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

// Health check
async function healthCheck(url, maxRetries = 15, interval = 3000) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const res = await fetch(url, { signal: AbortSignal.timeout(5000) });
      if (res.ok) return true;
    } catch {
      // not ready yet
    }
    await sleep(interval);
  }
  return false;
}

// Build .env content for remote server
function buildRemoteEnv() {
  return `SPRING_PROFILE=prod

MYSQL_HOST=${process.env.MYSQL_HOST || "127.0.0.1"}
MYSQL_PORT=${process.env.MYSQL_PORT || "3306"}
MYSQL_DATABASE=${process.env.MYSQL_DATABASE || "ai_love_daily"}
MYSQL_USERNAME=${process.env.MYSQL_USERNAME || "yexw"}
MYSQL_PASSWORD=${process.env.MYSQL_PASSWORD || "Yxw72707"}

REDIS_HOST=${process.env.REDIS_HOST || "127.0.0.1"}
REDIS_PORT=${process.env.REDIS_PORT || "6379"}
REDIS_PASSWORD=${process.env.REDIS_PASSWORD || "AiLoveDailyRedis!2026#9rV"}
REDIS_DATABASE=${process.env.REDIS_DATABASE || "0"}

FILE_UPLOAD_PATH=/root/project/data/uploads
JAVA_OPTS="-Xms256m -Xmx512m"
`;
}

// Build start.sh content for remote (adapted paths)
function buildRemoteStartSh(remoteDir) {
  return `#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${remoteDir}"
JAR_PATH="\${APP_HOME}/${JAR_NAME}"
LOG_DIR="\${APP_HOME}/logs"
PID_FILE="\${APP_HOME}/backend.pid"
ENV_FILE="\${APP_HOME}/.env"

if [[ -f "\${ENV_FILE}" ]]; then
  set -a
  source "\${ENV_FILE}"
  set +a
fi

JAVA_OPTS="\${JAVA_OPTS:--Xms256m -Xmx512m}"
FILE_UPLOAD_PATH="\${FILE_UPLOAD_PATH:-\${APP_HOME}/data/uploads}"

mkdir -p "\${LOG_DIR}" "\${FILE_UPLOAD_PATH}"

if [[ -f "\${PID_FILE}" ]]; then
  OLD_PID="\$(cat "\${PID_FILE}")"
  if [[ -n "\${OLD_PID}" ]] && kill -0 "\${OLD_PID}" 2>/dev/null; then
    echo "Backend is already running. PID=\${OLD_PID}"
    exit 1
  fi
fi

cd "\${APP_HOME}"

nohup java \${JAVA_OPTS} -jar "\${JAR_PATH}" \\
  --spring.profiles.active="\${SPRING_PROFILE:-prod}" \\
  > "\${LOG_DIR}/backend.out.log" \\
  2> "\${LOG_DIR}/backend.err.log" &

echo \$! > "\${PID_FILE}"
echo "Backend started. PID=\$(cat "\${PID_FILE}")"
echo "Logs: \${LOG_DIR}/backend.out.log"
`;
}

// Build stop.sh content for remote
function buildRemoteStopSh(remoteDir) {
  return `#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${remoteDir}"
PID_FILE="\${APP_HOME}/backend.pid"

if [[ ! -f "\${PID_FILE}" ]]; then
  echo "PID file not found: \${PID_FILE}"
  exit 1
fi

PID="\$(cat "\${PID_FILE}")"

if [[ -z "\${PID}" ]] || ! kill -0 "\${PID}" 2>/dev/null; then
  echo "Process is not running."
  rm -f "\${PID_FILE}"
  exit 1
fi

kill "\${PID}"
rm -f "\${PID_FILE}"
echo "Backend stopped. PID=\${PID}"
`;
}

// Main deploy function
async function deploy(params) {
  const config = { ...DEFAULTS, ...params };
  const steps = [];

  try {
    // Step 1: Maven build
    if (!params.skipBuild) {
      log("Step 1/7: Maven build...");
      try {
        execSync("mvn clean package -DskipTests -f backend/pom.xml", {
          cwd: PROJECT_ROOT,
          stdio: "pipe",
          timeout: 300_000,
        });
        steps.push("Maven build: OK");
        log("Maven build succeeded.");
      } catch (e) {
        throw new Error(`Maven build failed: ${e.stderr?.toString() || e.message}`);
      }
    } else {
      steps.push("Maven build: SKIPPED");
      log("Step 1/7: Maven build skipped.");
    }

    // Verify jar exists
    if (!existsSync(JAR_LOCAL)) {
      throw new Error(`Jar not found: ${JAR_LOCAL}`);
    }

    // Step 2: SSH connect
    log(`Step 2/7: Connecting to ${config.host}:${config.port}...`);
    const conn = await sshConnect(config);
    log("SSH connected.");

    try {
      // Step 3: Stop old process
      log("Step 3/7: Stopping old process...");
      try {
        await sshExec(conn, `bash ${config.remoteDir}/stop.sh`);
        steps.push("Stop old process: OK");
        log("Old process stopped.");
      } catch {
        steps.push("Stop old process: SKIPPED (not running)");
        log("No running process found.");
      }

      // Step 4: Create remote dir and upload files
      log("Step 4/7: Uploading files...");
      await sshExec(conn, `mkdir -p ${config.remoteDir}/logs ${config.remoteDir}/data/uploads`);

      // Upload jar
      await scpUpload(conn, JAR_LOCAL, `${config.remoteDir}/${JAR_NAME}`);
      log(`  Uploaded ${JAR_NAME}`);

      // Upload start.sh and stop.sh
      await scpUploadContent(conn, buildRemoteStartSh(config.remoteDir), `${config.remoteDir}/start.sh`);
      await scpUploadContent(conn, buildRemoteStopSh(config.remoteDir), `${config.remoteDir}/stop.sh`);
      await sshExec(conn, `chmod +x ${config.remoteDir}/start.sh ${config.remoteDir}/stop.sh`);
      log("  Uploaded start.sh + stop.sh");

      steps.push("Upload files: OK (jar + scripts)");

      // Step 5: Write .env
      log("Step 5/7: Writing .env...");
      const envContent = buildRemoteEnv();
      // Escape for shell heredoc
      await sshExec(conn, `cat > ${config.remoteDir}/.env << 'ENVEOF'\n${envContent}ENVEOF`);
      steps.push("Write .env: OK");
      log(".env written.");

      // Step 6: Start service
      log("Step 6/7: Starting service...");
      await sshExec(conn, `bash ${config.remoteDir}/start.sh`);
      steps.push("Start service: OK");
      log("Service starting...");

      // Step 7: Health check
      log("Step 7/7: Health check (waiting for /api/auth/dev-login)...");
      const healthUrl = `http://127.0.0.1:8080/api/auth/dev-login?userId=1`;
      // We need to do health check from remote server
      let healthy = false;
      for (let i = 0; i < 20; i++) {
        await sleep(3000);
        try {
          const result = await sshExec(
            conn,
            `curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:8080/api/auth/dev-login?userId=1"`
          );
          if (result === "200") {
            healthy = true;
            break;
          }
          log(`  Health check attempt ${i + 1}: HTTP ${result}`);
        } catch {
          log(`  Health check attempt ${i + 1}: not ready`);
        }
      }

      if (healthy) {
        steps.push("Health check: OK (HTTP 200)");
        log("Health check passed!");
      } else {
        steps.push("Health check: TIMEOUT (service may still be starting)");
        log("Health check timed out - service may need more time.");
      }
    } finally {
      conn.end();
    }

    return {
      success: true,
      message: "Deployment completed",
      steps,
      target: `${config.username}@${config.host}:${config.remoteDir}`,
    };
  } catch (e) {
    steps.push(`ERROR: ${e.message}`);
    return {
      success: false,
      message: `Deployment failed: ${e.message}`,
      steps,
    };
  }
}

// MCP Server setup
const server = new McpServer({
  name: "deploy",
  version: "1.0.0",
});

server.tool(
  "deploy",
  "Deploy the ai-love-daily backend to a remote server. Builds the jar, uploads it, configures .env, and starts the service.",
  {
    host: z.string().optional().describe("Remote host IP (default from .env or 123.60.31.79)"),
    port: z.number().optional().describe("SSH port (default 22)"),
    username: z.string().optional().describe("SSH username (default root)"),
    password: z.string().optional().describe("SSH password"),
    remoteDir: z.string().optional().describe("Remote deploy directory (default /root/project)"),
    skipBuild: z.boolean().optional().describe("Skip Maven build step (default false)"),
  },
  async (params) => {
    const result = await deploy(params);
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(result, null, 2),
        },
      ],
    };
  }
);

// Start server
const transport = new StdioServerTransport();
await server.connect(transport);
log("Deploy MCP server started.");
