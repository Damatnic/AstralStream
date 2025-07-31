#!/bin/bash

# AstralStream Elite Upgrade Agent Installation Script
# This script sets up the complete environment for upgrading your video player

set -e  # Exit on any error

echo "========================================"
echo "AstralStream Elite Upgrade Agent Setup"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# Check if Python is installed
check_python() {
    print_header "Checking Python installation..."
    
    if command -v python3 &> /dev/null; then
        PYTHON_VERSION=$(python3 --version | cut -d' ' -f2)
        print_status "Python $PYTHON_VERSION found"
        
        # Check if version is >= 3.8
        if python3 -c "import sys; exit(0 if sys.version_info >= (3, 8) else 1)"; then
            print_status "Python version is compatible"
        else
            print_error "Python 3.8 or higher is required"
            exit 1
        fi
    else
        print_error "Python 3 is not installed"
        echo "Please install Python 3.8 or higher from https://python.org"
        exit 1
    fi
}

# Check if pip is installed
check_pip() {
    print_header "Checking pip installation..."
    
    if command -v pip3 &> /dev/null; then
        print_status "pip3 found"
    else
        print_warning "pip3 not found, attempting to install..."
        if command -v apt-get &> /dev/null; then
            sudo apt-get update && sudo apt-get install -y python3-pip
        elif command -v yum &> /dev/null; then
            sudo yum install -y python3-pip
        elif command -v brew &> /dev/null; then
            brew install python
        else
            print_error "Could not install pip. Please install it manually."
            exit 1
        fi
    fi
}

# Create virtual environment
create_venv() {
    print_header "Creating virtual environment..."
    
    if [ ! -d "venv" ]; then
        python3 -m venv venv
        print_status "Virtual environment created"
    else
        print_status "Virtual environment already exists"
    fi
    
    # Activate virtual environment
    source venv/bin/activate
    print_status "Virtual environment activated"
    
    # Upgrade pip
    pip install --upgrade pip
}

# Install dependencies
install_dependencies() {
    print_header "Installing dependencies..."
    
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt
        print_status "Dependencies installed from requirements.txt"
    else
        print_warning "requirements.txt not found, installing essential packages..."
        pip install pyyaml pathlib2 click colorama tqdm jinja2
    fi
}

# Install the package
install_package() {
    print_header "Installing AstralStream Elite Agent..."
    
    if [ -f "setup.py" ]; then
        pip install -e .
        print_status "Package installed in development mode"
    else
        print_error "setup.py not found"
        exit 1
    fi
}

# Verify installation
verify_installation() {
    print_header "Verifying installation..."
    
    if command -v astral-upgrade &> /dev/null; then
        print_status "Installation successful! Command 'astral-upgrade' is available"
    else
        print_error "Installation failed - command not found"
        exit 1
    fi
}

# Create example configuration
create_example_config() {
    print_header "Creating example configuration..."
    
    if [ ! -f "example-config.yaml" ]; then
        cp config.yaml example-config.yaml 2>/dev/null || {
            cat > example-config.yaml << EOF
# Example AstralStream Elite Agent Configuration
project:
  name: "AstralStream"
  version: "2.0.0-elite"
  package_name: "com.astralplayer"

quality_targets:
  code_coverage: 85
  performance_score: 95
  security_score: 100

upgrade_features:
  architecture:
    clean_architecture: true
    mvvm_pattern: true
  security:
    encryption: "AES-256"
    certificate_pinning: true
  performance:
    adaptive_streaming: true
    cache_size_mb: 500
EOF
        }
        print_status "Example configuration created"
    else
        print_status "Example configuration already exists"
    fi
}

# Show usage instructions
show_usage() {
    print_header "Usage Instructions"
    echo ""
    echo "To upgrade your AstralStream project:"
    echo ""
    echo "1. Navigate to your project directory:"
    echo "   cd /path/to/your/astralstream/project"
    echo ""
    echo "2. Run the upgrade agent:"
    echo "   astral-upgrade --project-path ."
    echo ""
    echo "3. Optional flags:"
    echo "   --dry-run          Preview changes without modifying files"
    echo "   --config FILE      Use custom configuration file"
    echo "   --skip-backup      Skip backup creation (not recommended)"
    echo ""
    echo "4. For help:"
    echo "   astral-upgrade --help"
    echo ""
    print_status "Setup complete! Your AstralStream Elite Agent is ready to use."
}

# Main installation process
main() {
    print_header "Starting AstralStream Elite Agent installation..."
    
    check_python
    check_pip
    create_venv
    install_dependencies
    install_package
    verify_installation
    create_example_config
    show_usage
    
    echo ""
    echo "========================================"
    echo -e "${GREEN}Installation Complete!${NC}"
    echo "========================================"
    echo ""
    echo "Next steps:"
    echo "1. Navigate to your AstralStream project directory"
    echo "2. Run: source $(pwd)/venv/bin/activate"
    echo "3. Run: astral-upgrade --project-path ."
    echo ""
}

# Handle script interruption
trap 'echo ""; print_error "Installation interrupted"; exit 1' INT

# Run main function
main "$@"