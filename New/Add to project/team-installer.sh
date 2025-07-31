#!/bin/bash
# AstralStream Expert Agent Team Installer

echo "=================================================="
echo "🚀 AstralStream Expert Agent Team Installer"
echo "=================================================="

# Check Python version
python_version=$(python3 --version 2>&1 | awk '{print $2}')
echo "✓ Found Python: $python_version"

# Create virtual environment
echo "📦 Creating virtual environment..."
python3 -m venv astralstream_agents
source astralstream_agents/bin/activate

# Install requirements
echo "📥 Installing dependencies..."
cat > requirements.txt << EOF
pyyaml>=6.0
pathlib2>=2.3.7
click>=8.1.0
colorama>=0.4.6
tqdm>=4.65.0
jinja2>=3.1.2
aiofiles>=23.1.0
EOF

pip install -r requirements.txt

# Download agent team script
echo "⬇️ Downloading expert agent team..."
curl -o astralstream_expert_team.py "https://raw.githubusercontent.com/astralstream/agents/main/astralstream_expert_team.py"
chmod +x astralstream_expert_team.py

# Create launcher script
cat > run_expert_team.sh << 'EOF'
#!/bin/bash
# AstralStream Expert Team Launcher

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=================================================="
echo -e "🚀 AstralStream Expert Agent Team"
echo -e "==================================================${NC}"

# Check if project path is provided
if [ -z "$1" ]; then
    echo -e "${RED}Error: Please provide the path to your AstralStream project${NC}"
    echo "Usage: ./run_expert_team.sh /path/to/your/project"
    exit 1
fi

PROJECT_PATH="$1"

# Verify project path exists
if [ ! -d "$PROJECT_PATH" ]; then
    echo -e "${RED}Error: Project path does not exist: $PROJECT_PATH${NC}"
    exit 1
fi

# Check for AndroidManifest.xml to verify it's an Android project
if [ ! -f "$PROJECT_PATH/app/src/main/AndroidManifest.xml" ]; then
    echo -e "${YELLOW}Warning: This doesn't appear to be an Android project${NC}"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Activate virtual environment
source astralstream_agents/bin/activate

# Show project info
echo -e "${GREEN}Project Path: $PROJECT_PATH${NC}"
echo -e "${GREEN}Starting expert agent team...${NC}"
echo

# Run the expert team
python astralstream_expert_team.py --project-path "$PROJECT_PATH"

# Check exit code
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Success! Your project has been upgraded to 10/10!${NC}"
    echo -e "Check ${PROJECT_PATH}/EXPERT_TEAM_REPORT.md for details"
else
    echo -e "${RED}❌ The upgrade process needs manual review${NC}"
    echo -e "Check the report for details"
fi

deactivate
EOF

chmod +x run_expert_team.sh

echo "✅ Installation complete!"
echo ""
echo "To run the expert team on your project:"
echo "./run_expert_team.sh /path/to/your/astralstream/project"
echo ""
echo "Example:"
echo "./run_expert_team.sh ~/AndroidStudioProjects/AstralStream"