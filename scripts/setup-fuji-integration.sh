#!/bin/bash

##############################################################################
# Configuration script for F-UJI integration in Dataverse
# 
# This script configures the necessary parameters to integrate the
# F-UJI (FAIRsFAIR FAIR Assessment Tool) service with Dataverse
#
# Usage: ./setup-fuji-integration.sh [OPTIONS]
##############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
DATAVERSE_URL="${DATAVERSE_URL:-http://localhost:8080}"
DATAVERSE_API_KEY="${DATAVERSE_API_KEY:-}"
FUJI_SERVICE_URL=""
FUJI_USERNAME=""
FUJI_PASSWORD=""
FUJI_METRIC_VERSION="0.8"
INTERACTIVE=true

# Function to print messages
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show help
show_help() {
    cat << EOF
Usage: $0 [OPTIONS]

Configure F-UJI integration in Dataverse

OPTIONS:
    -u, --dataverse-url URL     Dataverse URL (default: http://localhost:8080)
    -k, --dataverse-key KEY     Dataverse API token (admin)
    -f, --fuji-url URL          F-UJI service URL
    --fuji-user USER            Username for F-UJI authentication (if needed)
    --fuji-pass PASS            Password for F-UJI authentication (if needed)
    --fuji-metric-version VER   FAIR metrics version (default: 0.8)
    -n, --non-interactive       Non-interactive mode
    -h, --help                  Show this help

EXAMPLES:
    # Interactive mode
    $0

    # Configure with local instance without authentication
    $0 -u http://localhost:8080 -k <API_TOKEN> -f http://localhost:1071/fuji/api/v1/evaluate

    # Configure with authentication and specific metric version
    $0 -f http://localhost:1071/fuji/api/v1/evaluate --fuji-user marvel --fuji-pass wonderwoman --fuji-metric-version 0.7 -k <API_TOKEN>

EOF
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -u|--dataverse-url)
            DATAVERSE_URL="$2"
            shift 2
            ;;
        -k|--dataverse-key)
            DATAVERSE_API_KEY="$2"
            shift 2
            ;;
        -f|--fuji-url)
            FUJI_SERVICE_URL="$2"
            INTERACTIVE=false
            shift 2
            ;;
        --fuji-user)
            FUJI_USERNAME="$2"
            shift 2
            ;;
        --fuji-pass)
            FUJI_PASSWORD="$2"
            shift 2
            ;;
        --fuji-metric-version)
            FUJI_METRIC_VERSION="$2"
            shift 2
            ;;
        -n|--non-interactive)
            INTERACTIVE=false
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Banner
echo "=================================================="
echo "  F-UJI Integration Configuration - Dataverse"
echo "=================================================="
echo ""

# Verify connectivity with Dataverse
    print_info "Verifying connectivity with Dataverse..."
if curl -s -f "${DATAVERSE_URL}/api/info/version" > /dev/null; then
    DATAVERSE_VERSION=$(curl -s "${DATAVERSE_URL}/api/info/version" | grep -o '"version":"[^"]*"' | cut -d'"' -f4)
    print_success "Conectado a Dataverse versión ${DATAVERSE_VERSION}"
else
    print_error "Cannot connect to Dataverse at ${DATAVERSE_URL}"
    print_info "Verify that Dataverse is running"
    exit 1
fi

echo ""

# Modo interactivo
if [ "$INTERACTIVE" = true ]; then
    print_info "Interactive configuration"
    echo ""

    read -p "Dataverse admin token (optional, recommended): " DATAVERSE_API_KEY
    
    # Ask for F-UJI service URL
    echo "Select the F-UJI service to use:"
    echo "  1) Local instance (http://localhost:1071)"
    echo "  2) Custom URL"
    read -p "Option [1-2]: " FUJI_OPTION
    
    case $FUJI_OPTION in
        1)
            FUJI_SERVICE_URL="http://localhost:1071/fuji/api/v1/evaluate"
            ;;
        2)
            read -p "Enter F-UJI service URL: " FUJI_SERVICE_URL
            ;;
        *)
            print_error "Invalid option"
            exit 1
            ;;
    esac
    
    echo ""
    read -p "Does the F-UJI service require authentication? (y/N): " NEEDS_AUTH
    if [[ $NEEDS_AUTH =~ ^[Yy]$ ]]; then
        read -p "F-UJI username: " FUJI_USERNAME
        read -sp "F-UJI password: " FUJI_PASSWORD
        echo ""
    fi
    
    echo ""
    echo "FAIR metrics version to use:"
    echo "  1) 0.8 (recommended, default)"
    echo "  2) Other version"
    read -p "Option [1-2] (default: 1): " METRIC_OPTION
    
    case $METRIC_OPTION in
        2)
            read -p "Enter metric version: " FUJI_METRIC_VERSION
            ;;
        *)
            FUJI_METRIC_VERSION="0.8"
            ;;
    esac
fi

# Validate that we have the service URL
if [ -z "$FUJI_SERVICE_URL" ]; then
    print_error "F-UJI service URL not specified"
    show_help
    exit 1
fi

echo ""
print_info "Configuration to apply:"
echo "  - Dataverse URL: ${DATAVERSE_URL}"
if [ -n "$DATAVERSE_API_KEY" ]; then
    echo "  - Dataverse API token: configured"
else
    echo "  - Dataverse API token: no"
fi
echo "  - F-UJI URL: ${FUJI_SERVICE_URL}"
echo "  - Metric version: ${FUJI_METRIC_VERSION}"
if [ -n "$FUJI_USERNAME" ]; then
    echo "  - Authentication: Yes (user: ${FUJI_USERNAME})"
else
    echo "  - Authentication: No"
fi

echo ""

# Confirm in interactive mode
if [ "$INTERACTIVE" = true ]; then
    read -p "Continue with configuration? (Y/n): " CONFIRM
    if [[ $CONFIRM =~ ^[Nn]$ ]]; then
        print_info "Configuration cancelled"
        exit 0
    fi
fi

echo ""
print_info "Applying configuration..."

# Optional authentication header
if [ -n "$DATAVERSE_API_KEY" ]; then
    CURL_API_KEY_HEADER=(-H "X-Dataverse-key: ${DATAVERSE_API_KEY}")
else
    CURL_API_KEY_HEADER=()
fi

# Configure F-UJI service URL
print_info "Configuring F-UJI service URL..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT -d "${FUJI_SERVICE_URL}" \
    "${CURL_API_KEY_HEADER[@]}" \
    "${DATAVERSE_URL}/api/admin/settings/:FujiServiceUrl")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    print_success "F-UJI service URL configured successfully"
else
    print_error "Error configuring F-UJI service URL (HTTP $HTTP_CODE)"
    echo "$RESPONSE"
    exit 1
fi

# Configure credentials if provided
if [ -n "$FUJI_USERNAME" ]; then
    print_info "Configuring F-UJI credentials..."
    
    # Configure username
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT -d "${FUJI_USERNAME}" \
        "${CURL_API_KEY_HEADER[@]}" \
        "${DATAVERSE_URL}/api/admin/settings/:FujiUsername")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "200" ]; then
        print_error "Error configuring username (HTTP $HTTP_CODE)"
        exit 1
    fi
    
    # Configure password
    RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT -d "${FUJI_PASSWORD}" \
        "${CURL_API_KEY_HEADER[@]}" \
        "${DATAVERSE_URL}/api/admin/settings/:FujiPassword")
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" != "200" ]; then
        print_error "Error configuring password (HTTP $HTTP_CODE)"
        exit 1
    fi
    
    print_success "Credentials configured successfully"
fi

# Configure metric version
print_info "Configuring F-UJI metric version (${FUJI_METRIC_VERSION})..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT -d "${FUJI_METRIC_VERSION}" \
    "${CURL_API_KEY_HEADER[@]}" \
    "${DATAVERSE_URL}/api/admin/settings/:FujiMetricVersion")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    print_success "Metric version configured: ${FUJI_METRIC_VERSION}"
else
    print_error "Error configuring metric version (HTTP $HTTP_CODE)"
    echo "$RESPONSE"
    exit 1
fi

echo ""
print_info "Verifying configuration..."

extract_setting_value() {
    echo "$1" | sed -n \
        -e 's/.*"content":"\([^"]*\)".*/\1/p' \
        -e 's/.*"message":"\([^"]*\)".*/\1/p' | head -n1
}

# Verify service URL
CONFIGURED_URL_RESPONSE=$(curl -s -w "\n%{http_code}" \
    "${CURL_API_KEY_HEADER[@]}" \
    "${DATAVERSE_URL}/api/admin/settings/:FujiServiceUrl")
CONFIGURED_URL_HTTP_CODE=$(echo "$CONFIGURED_URL_RESPONSE" | tail -n1)
# Extract the value from the "content" field of the JSON response
CONFIGURED_URL=$(extract_setting_value "$CONFIGURED_URL_RESPONSE")
if [ "$CONFIGURED_URL_HTTP_CODE" != "200" ]; then
    print_warning "Could not verify URL (HTTP $CONFIGURED_URL_HTTP_CODE)."
elif [ "$CONFIGURED_URL" = "$FUJI_SERVICE_URL" ]; then
    print_success "URL verificada: ${CONFIGURED_URL}"
else
    print_warning "Configured URL does not match expected"
    print_warning "  Expected: ${FUJI_SERVICE_URL}"
    print_warning "  Configured: ${CONFIGURED_URL:-<empty or error>}"
fi

# Verify metric version
CONFIGURED_VERSION_RESPONSE=$(curl -s -w "\n%{http_code}" \
    "${CURL_API_KEY_HEADER[@]}" \
    "${DATAVERSE_URL}/api/admin/settings/:FujiMetricVersion")
CONFIGURED_VERSION_HTTP_CODE=$(echo "$CONFIGURED_VERSION_RESPONSE" | tail -n1)
CONFIGURED_VERSION=$(extract_setting_value "$CONFIGURED_VERSION_RESPONSE")
if [ "$CONFIGURED_VERSION_HTTP_CODE" != "200" ]; then
    print_warning "Could not verify version (HTTP $CONFIGURED_VERSION_HTTP_CODE)."
elif [ "$CONFIGURED_VERSION" = "$FUJI_METRIC_VERSION" ]; then
    print_success "Versión de métricas verificada: ${CONFIGURED_VERSION}"
else
    print_warning "Configured metric version does not match expected"
    print_warning "  Expected: ${FUJI_METRIC_VERSION}"
    print_warning "  Configured: ${CONFIGURED_VERSION:-<empty or error>}"
fi

# Test connectivity with F-UJI
echo ""
print_info "Testing connectivity with F-UJI service..."

# Make test request (using configured metric version)
TEST_PAYLOAD="{\"object_identifier\":\"doi:10.5072/FK2/TEST\",\"test_debug\":true,\"metric_version\":\"${FUJI_METRIC_VERSION}\"}"

# Function to make requests to F-UJI with or without authentication
fuji_request() {
    local payload="$1"
    local url="$2"
    
    if [ -n "$FUJI_USERNAME" ] && [ -n "$FUJI_PASSWORD" ]; then
        # Generate Basic Auth token in base64
        local AUTH_TOKEN=$(echo -n "${FUJI_USERNAME}:${FUJI_PASSWORD}" | base64)
        curl -s -w "\n%{http_code}" -X POST \
            -H "Authorization: Basic ${AUTH_TOKEN}" \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -d "$payload" \
            "$url" 2>&1
    else
        curl -s -w "\n%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -d "$payload" \
            "$url" 2>&1
    fi
}

# Prepare curl command with or without authentication
FUJI_RESPONSE=$(fuji_request "$TEST_PAYLOAD" "${FUJI_SERVICE_URL}")

FUJI_HTTP_CODE=$(echo "$FUJI_RESPONSE" | tail -n1)

if [ "$FUJI_HTTP_CODE" = "200" ] || [ "$FUJI_HTTP_CODE" = "201" ]; then
    print_success "Servicio F-UJI respondiendo correctamente"
else
    print_warning "F-UJI service is not responding as expected (HTTP $FUJI_HTTP_CODE)"
    print_info "This may be normal if the service requires additional configuration"
fi

# Final summary
echo ""
echo "=================================================="
echo "  Configuration completed"
echo "=================================================="
echo ""
print_success "F-UJI integration has been configured"
echo ""
print_info "Next steps:"
echo "  1. Verify that the F-UJI service is running"
echo "  2. Open a published dataset page in Dataverse"
echo "  3. You should see the FAIR assessment widget"
echo ""
print_info "Para más información, consulta: doc/FUJI-INTEGRATION.md"
echo ""

# Offer test with real dataset
if [ "$INTERACTIVE" = true ]; then
    echo ""
    read -p "Do you want to test with a specific PID? (y/N): " TEST_SPECIFIC
    if [[ $TEST_SPECIFIC =~ ^[Yy]$ ]]; then
        read -p "Enter dataset PID: " TEST_PID
        if [ -n "$TEST_PID" ]; then
            print_info "Testing with PID: ${TEST_PID}"
            TEST_PAYLOAD="{\"object_identifier\":\"${TEST_PID}\",\"test_debug\":false,\"metric_version\":\"0.8\"}"
            
            # Use fuji_request function (remove HTTP code from the end)
            FULL_RESPONSE=$(fuji_request "$TEST_PAYLOAD" "${FUJI_SERVICE_URL}")
            HTTP_CODE=$(echo "$FULL_RESPONSE" | tail -n1)
            RESULT=$(echo "$FULL_RESPONSE" | sed '$d')
            
            echo ""
            if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
                print_success "Evaluation completed (HTTP $HTTP_CODE)"
            else
                print_warning "HTTP response: $HTTP_CODE"
            fi
            
            echo ""
            print_info "Evaluation result:"
            echo "$RESULT" | python3 -m json.tool 2>/dev/null || echo "$RESULT"
        fi
    fi
fi

exit 0
