#!/usr/bin/env bash
set -euo pipefail
if [[ $EUID -ne 0 ]]; then echo "Executa: sudo ./install-systemd.sh"; exit 1; fi
INSTALL_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVICE_USER="${SUDO_USER:-$(stat -c %U "$INSTALL_DIR")}" 
if [[ "$SERVICE_USER" == root ]]; then echo 'Instala a partir de um utilizador normal com sudo, não diretamente como root.'; exit 1; fi
command -v javac >/dev/null || { echo 'Java JDK 21 necessário'; exit 1; }
id "$SERVICE_USER" >/dev/null
install -d -m 750 -o "$SERVICE_USER" -g "$(id -gn "$SERVICE_USER")" "$INSTALL_DIR/data" "$INSTALL_DIR/out"
ENVFILE=/etc/haccp-auto.env
if [[ ! -f "$ENVFILE" ]]; then
  echo 'Definir password inicial do administrador (mín. 12 caracteres):'
  read -r -s -p 'Password: ' ADMIN_PASSWORD; echo
  [[ ${#ADMIN_PASSWORD} -ge 12 ]] || { echo 'Password demasiado curta'; exit 1; }
  [[ "$ADMIN_PASSWORD" =~ ^[a-zA-Z0-9_!@.+=-]+$ ]] || { echo 'Usa letras, números ou _ ! @ . + = - na password inicial'; exit 1; }
  { printf 'HACCP_ADMIN_PASSWORD=%s\n' "$ADMIN_PASSWORD"; printf 'HACCP_BIND=0.0.0.0\nHACCP_PORT=8080\nHACCP_SECURE_COOKIE=false\n'; } > "$ENVFILE"
  unset ADMIN_PASSWORD
fi
chmod 600 "$ENVFILE"
cat >/etc/systemd/system/haccp-auto.service <<EOF
[Unit]
Description=HACCP Auto (Java 21)
After=network.target

[Service]
Type=simple
User=$SERVICE_USER
WorkingDirectory=$INSTALL_DIR
EnvironmentFile=$ENVFILE
ExecStart=$INSTALL_DIR/run.sh
Restart=on-failure
RestartSec=5
NoNewPrivileges=true
ProtectSystem=strict
ReadWritePaths=$INSTALL_DIR/data $INSTALL_DIR/out
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF
chmod +x "$INSTALL_DIR/run.sh"
systemctl daemon-reload
systemctl enable --now haccp-auto
systemctl --no-pager status haccp-auto || true
echo "Endereço de rede: http://IP_DO_SERVIDOR:8080"
echo 'Se a firewall UFW estiver ativa: sudo ufw allow from 192.168.1.0/24 to any port 8080 proto tcp (adapta à tua sub-rede)'
