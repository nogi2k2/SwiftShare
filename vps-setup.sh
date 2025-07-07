#!/bin/bash
set -e
echo "Running Service setup script"

echo "Updating system packages"
sudo apt update
sudo apt upgrade -y

echo "Installing Java"
sudo apt install -y openjdk-17-jdk

echo "Installing Node.js"
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

echo "Installing nginx"
sudo apt install -y nginx

echo "Installing pm2"
sudo npm install -g pm2

echo "Installing Maven"
sudo apt install -y maven

echo "Cloning repository"
git clone https://github.com/nogi2k2/SwiftShare.git
cd SwiftShare

echo "Building Java Backend"
mvn clean package

echo "Building Frontend"
cd ui
npm install
npm run build
cd ..

echo "Setting up Nginx"
if [ -e /etc/nginx/sites-enabled/default ]; then
    sudo rm /etc/nginx/sites-enabled/default
    echo "Removed default Nginx site configuration."
fi

cat <<EOF | sudo tee /etc/nginx/sites-available/swiftshare
server {
    listen 80;
    server_name _; 

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
    }

    # Frontend
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
    }

    add_header X-Content-Type-Options nosniff;
    add_header X-Frame-Options SAMEORIGIN;
    add_header X-XSS-Protection "1; mode=block";
}
EOF

sudo ln -sf /etc/nginx/sites-available/swiftshare /etc/nginx/sites-enabled/swiftshare

sudo nginx -t
if [ $? -eq 0 ]; then
    sudo systemctl restart nginx
    echo "Nginx configured and restarted successfully."
else
    echo "Nginx configuration test failed. Please check /etc/nginx/nginx.conf and /etc/nginx/sites-available/swiftshare."
    exit 1
fi

echo "Starting backend service"
CLASSPATH="target/p2p-1.0-SNAPSHOT.jar:$(mvn dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout -q)"
pm2 start --name swiftshare-backend java -- -cp "$CLASSPATH" p2p.App

echo "Starting frontend service"
cd ui
pm2 start npm --name swiftshare-frontend -- start
cd ..

pm2 save

echo "Setting up PM2 auto-startup"
pm2 startup

echo "Setup completed"
echo "SwiftShare is now running"
echo "Frontend: http://3.110.36.133:80"
