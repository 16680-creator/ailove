# Linux Prod Bundle

这个目录用于 Linux 生产部署。

## Contents

- `app.jar`: 后端 Spring Boot 可执行包
- `start.sh`: 生产启动脚本
- `stop.sh`: 停止脚本
- `.env.example`: 环境变量模板

## Run

```bash
cp .env.example .env
chmod +x start.sh stop.sh
./start.sh
```

## Stop

```bash
./stop.sh
```

## Default Upload Path

默认上传目录建议使用：

```bash
/root/project/love/uploads
```
