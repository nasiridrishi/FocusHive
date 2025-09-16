#!/bin/bash

# FocusHive Multi-Agent Merge Strategy Script
# Handles integration of 4 parallel development branches

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

WORKSPACE_DIR="/Users/nasir/uol/focushive/workspaces"
MAIN_REPO="/Users/nasir/uol/focushive/frontend"

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}           Multi-Agent Branch Integration Strategy               ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"

# Function to check conflicts
check_conflicts() {
    local branch1=$1
    local branch2=$2
    local dir1="${WORKSPACE_DIR}/${branch1}"
    local dir2="${WORKSPACE_DIR}/${branch2}"

    echo -e "\n${YELLOW}Checking conflicts: ${branch1} ↔ ${branch2}${NC}"

    # Get list of modified files from both branches
    cd "$dir1"
    git diff --name-only HEAD~10..HEAD 2>/dev/null > /tmp/files1.txt || touch /tmp/files1.txt

    cd "$dir2"
    git diff --name-only HEAD~10..HEAD 2>/dev/null > /tmp/files2.txt || touch /tmp/files2.txt

    # Find common files (potential conflicts)
    comm -12 <(sort /tmp/files1.txt) <(sort /tmp/files2.txt) > /tmp/conflicts.txt

    if [ -s /tmp/conflicts.txt ]; then
        echo -e "${RED}⚠️  Potential conflicts detected:${NC}"
        cat /tmp/conflicts.txt | head -10
        return 1
    else
        echo -e "${GREEN}✅ No conflicts detected${NC}"
        return 0
    fi
}

# Function to analyze agent changes
analyze_agent_changes() {
    local agent=$1
    local agent_dir="${WORKSPACE_DIR}/${agent}"

    if [ ! -d "$agent_dir" ]; then
        echo -e "${RED}❌ ${agent} workspace not found${NC}"
        return
    fi

    cd "$agent_dir"

    echo -e "\n${BLUE}Analyzing ${agent}${NC}"
    echo "────────────────────────"

    # Get statistics
    COMMITS=$(git rev-list --count HEAD 2>/dev/null || echo 0)
    MODIFIED=$(git diff --name-only HEAD~${COMMITS}..HEAD 2>/dev/null | wc -l || echo 0)
    ADDITIONS=$(git diff --shortstat HEAD~${COMMITS}..HEAD 2>/dev/null | grep -oE '[0-9]+ insertion' | grep -oE '[0-9]+' || echo 0)
    DELETIONS=$(git diff --shortstat HEAD~${COMMITS}..HEAD 2>/dev/null | grep -oE '[0-9]+ deletion' | grep -oE '[0-9]+' || echo 0)

    echo "Commits: $COMMITS"
    echo "Files modified: $MODIFIED"
    echo "Lines added: $ADDITIONS"
    echo "Lines deleted: $DELETIONS"

    # Check test coverage if available
    if [ -f "coverage/coverage-summary.json" ]; then
        COVERAGE=$(grep -o '"pct":[0-9.]*' coverage/coverage-summary.json | head -1 | cut -d: -f2)
        echo "Test coverage: ${COVERAGE}%"
    fi

    # List modified directories (to verify boundaries)
    echo -e "\n${YELLOW}Modified directories:${NC}"
    git diff --name-only HEAD~${COMMITS}..HEAD 2>/dev/null | xargs -I {} dirname {} | sort | uniq | head -10
}

# Function to create integration branch
create_integration_branch() {
    echo -e "\n${BLUE}Creating Integration Branch${NC}"
    echo "────────────────────────"

    cd "$MAIN_REPO"

    # Create integration branch from main
    INTEGRATION_BRANCH="integration/sprint-$(date +%Y%m%d-%H%M)"
    git checkout main
    git pull origin main || true
    git checkout -b "$INTEGRATION_BRANCH"

    echo -e "${GREEN}✅ Created integration branch: ${INTEGRATION_BRANCH}${NC}"

    # Apply each agent's changes
    agents=(
        "agent-1-core:Core Infrastructure"
        "agent-2-backend:Backend Features"
        "agent-3-ui:Frontend UI"
        "agent-4-qa:Quality Assurance"
    )

    for agent_info in "${agents[@]}"; do
        IFS=':' read -r agent_name description <<< "$agent_info"
        agent_dir="${WORKSPACE_DIR}/${agent_name}"

        echo -e "\n${YELLOW}Merging ${description}${NC}"

        if [ -d "$agent_dir" ]; then
            cd "$agent_dir"

            # Create patch file
            git diff HEAD~999..HEAD > "/tmp/${agent_name}.patch" 2>/dev/null || true

            if [ -s "/tmp/${agent_name}.patch" ]; then
                cd "$MAIN_REPO"
                git apply --check "/tmp/${agent_name}.patch" 2>/dev/null
                if [ $? -eq 0 ]; then
                    git apply "/tmp/${agent_name}.patch"
                    git add -A
                    git commit -m "feat(${agent_name#agent-*}): integrate ${description} changes"
                    echo -e "${GREEN}✅ Successfully merged ${agent_name}${NC}"
                else
                    echo -e "${RED}❌ Conflicts detected for ${agent_name}${NC}"
                    echo "Manual resolution required"
                fi
            else
                echo -e "${YELLOW}No changes from ${agent_name}${NC}"
            fi
        fi
    done
}

# Main menu
show_menu() {
    echo -e "\n${BLUE}Select Merge Strategy:${NC}"
    echo "────────────────────────"
    echo "1) Analyze all agent changes"
    echo "2) Check for conflicts"
    echo "3) Create integration branch (recommended)"
    echo "4) Sequential merge (one by one)"
    echo "5) Generate merge report"
    echo "6) Exit"

    read -p "Choose option (1-6): " choice

    case $choice in
        1)
            echo -e "\n${BLUE}Analyzing All Agents${NC}"
            analyze_agent_changes "agent-1-core"
            analyze_agent_changes "agent-2-backend"
            analyze_agent_changes "agent-3-ui"
            analyze_agent_changes "agent-4-qa"
            ;;
        2)
            echo -e "\n${BLUE}Checking for Conflicts${NC}"
            check_conflicts "agent-1-core" "agent-2-backend"
            check_conflicts "agent-2-backend" "agent-3-ui"
            check_conflicts "agent-3-ui" "agent-4-qa"
            check_conflicts "agent-1-core" "agent-3-ui"
            ;;
        3)
            create_integration_branch
            ;;
        4)
            echo -e "\n${YELLOW}Sequential merge requires manual steps:${NC}"
            echo "1. cd workspaces/agent-1-core && git push origin feature/core-infrastructure"
            echo "2. Create PR from feature/core-infrastructure to main"
            echo "3. Merge PR after review"
            echo "4. Repeat for other agents in order"
            ;;
        5)
            generate_merge_report
            ;;
        6)
            echo "Exiting..."
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option${NC}"
            ;;
    esac

    # Show menu again
    show_menu
}

# Generate merge report
generate_merge_report() {
    echo -e "\n${BLUE}Generating Merge Report${NC}"
    echo "────────────────────────"

    REPORT_FILE="${WORKSPACE_DIR}/MERGE_REPORT_$(date +%Y%m%d-%H%M).md"

    cat > "$REPORT_FILE" << EOF
# Multi-Agent Merge Report
Generated: $(date)

## Agent Statistics

EOF

    agents=("agent-1-core" "agent-2-backend" "agent-3-ui" "agent-4-qa")

    for agent in "${agents[@]}"; do
        agent_dir="${WORKSPACE_DIR}/${agent}"
        if [ -d "$agent_dir" ]; then
            cd "$agent_dir"

            COMMITS=$(git rev-list --count HEAD 2>/dev/null || echo 0)
            BRANCH=$(git branch --show-current)

            cat >> "$REPORT_FILE" << EOF
### ${agent}
- Branch: ${BRANCH}
- Commits: ${COMMITS}
- Last commit: $(git log -1 --format='%h %s' 2>/dev/null || echo 'No commits')

Modified files:
\`\`\`
$(git diff --name-only HEAD~${COMMITS}..HEAD 2>/dev/null | head -20 || echo 'No changes')
\`\`\`

EOF
        fi
    done

    cat >> "$REPORT_FILE" << EOF
## Conflict Analysis

EOF

    # Check conflicts between agents
    echo "Checking for cross-agent file modifications..." >> "$REPORT_FILE"

    echo -e "${GREEN}✅ Report generated: ${REPORT_FILE}${NC}"
}

# Start the script
echo "Analyzing workspace state..."

# Check if workspaces exist
if [ ! -d "$WORKSPACE_DIR" ]; then
    echo -e "${RED}❌ Workspace directory not found!${NC}"
    echo "Run ./execute-workspace-setup.sh first"
    exit 1
fi

# Count workspaces
WORKSPACE_COUNT=$(ls -d ${WORKSPACE_DIR}/agent-* 2>/dev/null | wc -l)
echo "Found ${WORKSPACE_COUNT} agent workspaces"

if [ "$WORKSPACE_COUNT" -ne 4 ]; then
    echo -e "${YELLOW}⚠️  Expected 4 workspaces, found ${WORKSPACE_COUNT}${NC}"
fi

# Show menu
show_menu