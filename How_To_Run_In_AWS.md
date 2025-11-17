# Tech Store API

A comprehensive Spring Boot REST API for a tech store, featuring complete product management, admin panel, and advanced e-commerce functionality.

### Steps to run the app in AWS EC2

#### Create logs directory and permissions
- sudo mkdir -p /srv/techstore/logs
- sudo chown -R 1000:1000 /srv/techstore/logs
- sudo chmod -R 777 /srv/techstore/logs

#### Create lucene directory and permissions
- sudo mkdir -p /srv/techstore/lucene
- sudo chown -R 1000:1000 /srv/techstore/lucene
- sudo chmod -R 777 /srv/techstore/lucene

#### Create upload directory and permissions
- sudo mkdir -p /srv/techstore/uploads
- sudo chown -R 1000:1000 /srv/techstore/uploads
- sudo chmod -R 777 /srv/techstore/uploads

#### Run the app
- docker compose up -d


#### docker-compose.prod.yml

services:
# PostgreSQL Database
postgres:
image: postgres:15-alpine
container_name: techstore-postgres
environment:
POSTGRES_USER: ${POSTGRES_USER}
POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
POSTGRES_DB: ${POSTGRES_DB}
ports:
- "5432:5432"
volumes:
- postgres_data:/var/lib/postgresql/data
#      - ./init-databases.sh:/docker-entrypoint-initdb.d/init-databases.sh
    networks:
      - techstore-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U techstore_user -d techstore"]
      interval: 30s
      timeout: 10s
      retries: 3

# React Application
frontend:
image: ludogoriesoft/care-tech-ui:latest
container_name: care-tech-ui
env_file:
- .env
ports:
- "3000:80"
restart: unless-stopped

  # Spring Boot Application
  app:
  container_name: techstore-api
  image: ludogoriesoft/techstore-api
  ports:
  - "8080:8080"
  env_file:
  - .env
  volumes:
  - /srv/techstore/uploads:/app/uploads
  - /srv/techstore/lucene:/app/lucene-indexes:rw
  - /srv/techstore/logs:/app/logs:rw

  depends_on:
  postgres:
  condition: service_healthy
  networks:
  - techstore-network
  healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
  restart: unless-stopped

volumes:
postgres_data:
uploads_data:

networks:
techstore-network:
driver: bridge



