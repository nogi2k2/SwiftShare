#!/bin/bash

set -e  

echo "Updating package index"
sudo apt update

echo "Installing prerequisites"
sudo apt install -y ca-certificates curl gnupg lsb-release git

echo "Setting up Docker GPG keyring"
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo "Adding Docker APT repository"
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
| sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

echo "Updating package index (after Docker repo)"
sudo apt update

echo "Installing Docker Engine"
sudo apt install -y docker-ce docker-ce-cli containerd.io

echo "Enabling Docker to start on boot"
sudo systemctl enable docker

echo "Installing Docker Compose plugin"
sudo apt install -y docker-compose-plugin

echo "Docker setup completed"

echo "Cloning PeerLink repo"
git clone https://github.com/nogi2k2/SwiftShare.git
cd SwiftShare

echo "Starting Docker containers"
sudo docker compose up -d

echo "Deployment complete"
