#!/bin/bash

# Payment Aggregator Demo - Stop script

set -e

echo "🛑 Stopping Payment Aggregator Demo..."

# Function to check if docker-compose is available
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null; then
        echo "❌ docker-compose is not installed."
        return 1
    fi
    return 0
}

# Function to stop docker-compose services
stop_docker_services() {
    if [ -f "docker-compose.yml" ]; then
        echo "🔄 Stopping Docker Compose services..."
        docker-compose down --remove-orphans
        
        # Also remove any volumes if they exist
        echo "🗑️  Removing Docker volumes..."
        docker-compose down --volumes --remove-orphans 2>/dev/null || true
        
        echo "✅ Docker services stopped successfully"
    else
        echo "⚠️  No docker-compose.yml found, skipping Docker services"
    fi
}

# Function to stop any running Gradle processes
stop_gradle_processes() {
    echo "🔄 Stopping any running Gradle processes..."
    
    # Find and kill Gradle daemon processes
    if pgrep -f "gradle.*daemon" > /dev/null; then
        echo "🔄 Stopping Gradle daemons..."
        ./gradlew --stop 2>/dev/null || true
        pkill -f "gradle.*daemon" 2>/dev/null || true
        echo "✅ Gradle daemons stopped"
    else
        echo "ℹ️  No Gradle daemons running"
    fi
    
    # Find and kill any bootRun processes
    if pgrep -f "bootRun" > /dev/null; then
        echo "🔄 Stopping bootRun processes..."
        pkill -f "bootRun" 2>/dev/null || true
        echo "✅ bootRun processes stopped"
    else
        echo "ℹ️  No bootRun processes running"
    fi
    
    # Find and kill any Java processes running our application
    if pgrep -f "PaymentAggregatorApplication" > /dev/null; then
        echo "🔄 Stopping Payment Aggregator application..."
        pkill -f "PaymentAggregatorApplication" 2>/dev/null || true
        echo "✅ Payment Aggregator application stopped"
    else
        echo "ℹ️  No Payment Aggregator application running"
    fi
}

# Function to stop any standalone Docker containers
stop_standalone_containers() {
    echo "🔄 Checking for standalone containers..."
    
    # Stop any containers with our app name
    CONTAINERS=$(docker ps -q --filter "name=payagg" 2>/dev/null || true)
    if [ ! -z "$CONTAINERS" ]; then
        echo "🔄 Stopping standalone Payment Aggregator containers..."
        docker stop $CONTAINERS 2>/dev/null || true
        docker rm $CONTAINERS 2>/dev/null || true
        echo "✅ Standalone containers stopped"
    else
        echo "ℹ️  No standalone containers running"
    fi
}

# Function to clean up any orphaned processes on common ports
cleanup_ports() {
    echo "🔄 Checking for processes on common ports..."
    
    # Check port 8080 (Spring Boot)
    if lsof -ti:8080 > /dev/null 2>&1; then
        echo "🔄 Stopping process on port 8080..."
        lsof -ti:8080 | xargs kill -9 2>/dev/null || true
        echo "✅ Port 8080 freed"
    else
        echo "ℹ️  Port 8080 is free"
    fi
    
    # Note: We don't kill PostgreSQL (5432) or Redis (6379) as they might be used by other applications
}

# Main execution
main() {
    echo "🚀 Starting cleanup process..."
    echo ""
    
    # Stop Docker services first
    if check_docker_compose; then
        stop_docker_services
    fi
    
    echo ""
    
    # Stop Gradle processes
    stop_gradle_processes
    
    echo ""
    
    # Stop standalone containers
    if command -v docker &> /dev/null; then
        stop_standalone_containers
    else
        echo "ℹ️  Docker not available, skipping container cleanup"
    fi
    
    echo ""
    
    # Clean up ports
    cleanup_ports
    
    echo ""
    echo "🎉 Payment Aggregator Demo stopped successfully!"
    echo ""
    echo "📋 Summary:"
    echo "   • Docker Compose services: Stopped and removed"
    echo "   • Gradle processes: Stopped"
    echo "   • Application processes: Stopped"
    echo "   • Port 8080: Freed"
    echo ""
    echo "💡 To start again, run: ./scripts/run.sh"
}

# Run main function
main
