#!/bin/bash

# Payment Aggregator Demo - One-liner run script

set -e

echo "🚀 Starting Payment Aggregator Demo..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose is not installed. Please install docker-compose and try again."
    exit 1
fi

# Stop any existing containers
echo "🛑 Stopping existing containers..."
docker-compose down --remove-orphans

# Build and start services
echo "🔨 Building and starting services..."
docker-compose up --build -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be healthy..."
timeout=300
elapsed=0
interval=5

while [ $elapsed -lt $timeout ]; do
    if docker-compose ps | grep -q "healthy"; then
        if [ $(docker-compose ps | grep "healthy" | wc -l) -eq 3 ]; then
            echo "✅ All services are healthy!"
            break
        fi
    fi
    
    echo "⏳ Waiting for services... ($elapsed/$timeout seconds)"
    sleep $interval
    elapsed=$((elapsed + interval))
done

if [ $elapsed -ge $timeout ]; then
    echo "❌ Services failed to start within $timeout seconds"
    echo "📋 Service status:"
    docker-compose ps
    echo "📋 Application logs:"
    docker-compose logs app
    exit 1
fi

# Display service information
echo ""
echo "🎉 Payment Aggregator Demo is running!"
echo ""
echo "📋 Service URLs:"
echo "   • API: http://localhost:8080"
echo "   • Swagger UI: http://localhost:8080/swagger-ui.html"
echo "   • Health Check: http://localhost:8080/actuator/health"
echo "   • Metrics: http://localhost:8080/actuator/metrics"
echo ""
echo "📋 Database connections:"
echo "   • PostgreSQL: localhost:5432 (user: payagg, password: payagg, db: payagg)"
echo "   • Redis: localhost:6379"
echo ""
echo "🔧 Useful commands:"
echo "   • View logs: docker-compose logs -f"
echo "   • Stop services: docker-compose down"
echo "   • Restart services: docker-compose restart"
echo ""
echo "📖 Check the README.md for API usage examples!"
