#!/usr/bin/env python3

import os
import re
import shutil
from pathlib import Path
import subprocess

def find_config_usages():
    """Find all files that import or use config classes"""
    
    config_usages = {}
    
    # Configs to replace with NewBaseTestConfig
    old_base_configs = [
        'BaseTestConfig',
        'TestConfig', 
        'MinimalTestConfig',
        'ComprehensiveTestConfig'
    ]
    
    # Integration configs to replace with NewIntegrationTestConfig  
    old_integration_configs = [
        'IntegrationTestConfig',
        'OAuth2IntegrationTestConfig'
    ]
    
    # Unified configs to replace with new configs
    unified_configs = [
        'UnifiedTestConfig'  # This needs special handling - replace with NewBaseTestConfig
    ]
    
    # All config patterns to search for
    all_old_configs = old_base_configs + old_integration_configs + unified_configs + [
        'SecurityTestConfig',
        'TestContainersConfig'  # Keep this one for now
    ]
    
    print("🔍 Searching for config usage in test files...")
    
    # Find all Java test files
    test_files = list(Path('src/test').glob('**/*.java'))
    
    for test_file in test_files:
        try:
            with open(test_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            file_usages = []
            
            # Check for various patterns
            for config in all_old_configs:
                # Check for import statements (with package)
                if re.search(f'import.*\.config\.{config}', content):
                    file_usages.append(f'imports {config}')
                
                # Check for @Import annotations (class name only)
                if re.search(f'@Import.*{config}', content):
                    file_usages.append(f'@Import({config})')
                
                # Check for extends clauses
                if re.search(f'extends\s+{config}', content):
                    file_usages.append(f'extends {config}')
                
                # Check for direct class references
                if f'{config}.class' in content:
                    file_usages.append(f'references {config}.class')
                
                # Check for @SpringBootTest classes parameter
                if re.search(f'@SpringBootTest.*classes.*{config}', content, re.DOTALL):
                    file_usages.append(f'@SpringBootTest classes={config}')
            
            if file_usages:
                config_usages[str(test_file)] = file_usages
        
        except Exception as e:
            print(f"Error reading {test_file}: {e}")
    
    return config_usages

def generate_migration_plan(usages):
    """Generate a migration plan based on usage patterns"""
    
    plan = {
        'phase1_base_configs': [],      # Files using old base configs
        'phase2_integration_configs': [], # Files using integration configs  
        'phase3_specialized': [],        # Files using specialized configs
        'phase4_manual_review': []       # Files that need manual review
    }
    
    for file_path, usage_list in usages.items():
        usage_str = ' '.join(usage_list)
        
        # Categorize based on usage patterns
        if any(base in usage_str for base in ['BaseTestConfig', 'TestConfig', 'MinimalTestConfig', 'UnifiedTestConfig']):
            plan['phase1_base_configs'].append({
                'file': file_path,
                'usages': usage_list,
                'replacement': 'NewBaseTestConfig'
            })
        
        elif any(integ in usage_str for integ in ['IntegrationTestConfig', 'OAuth2IntegrationTestConfig']):
            plan['phase2_integration_configs'].append({
                'file': file_path,
                'usages': usage_list,
                'replacement': 'NewIntegrationTestConfig'
            })
        
        elif 'SecurityTestConfig' in usage_str:
            plan['phase3_specialized'].append({
                'file': file_path,
                'usages': usage_list,
                'replacement': 'Keep SecurityTestConfig for now'
            })
        
        else:
            plan['phase4_manual_review'].append({
                'file': file_path,
                'usages': usage_list,
                'replacement': 'Manual review needed'
            })
    
    return plan

def execute_phase1_migration(phase1_files):
    """Migrate files to use NewBaseTestConfig"""
    
    print(f"\n🔧 PHASE 1: Migrating {len(phase1_files)} files to NewBaseTestConfig")
    
    replacements = {
        'BaseTestConfig': 'NewBaseTestConfig',
        'TestConfig': 'NewBaseTestConfig', 
        'MinimalTestConfig': 'NewBaseTestConfig',
        'UnifiedTestConfig': 'NewBaseTestConfig',
        'ComprehensiveTestConfig': 'NewBaseTestConfig'
    }
    
    migrated_files = []
    
    for item in phase1_files:
        file_path = item['file']
        
        try:
            print(f"  📝 Migrating: {os.path.basename(file_path)}")
            
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original_content = content
            
            # Replace imports
            for old_config, new_config in replacements.items():
                # Replace import statements
                content = re.sub(
                    f'import.*\\.config\\.{old_config}',
                    f'import com.focushive.identity.config.{new_config}',
                    content
                )
                
                # Replace @Import annotations
                content = re.sub(
                    f'@Import\\({old_config}\\.class\\)',
                    f'@Import({new_config}.class)',
                    content
                )
                
                # Replace @SpringBootTest classes references
                content = re.sub(
                    f'{old_config}\\.class',
                    f'{new_config}.class',
                    content
                )
                
                # Replace extends clauses
                content = re.sub(
                    f'extends {old_config}',
                    f'extends {new_config}',
                    content
                )
            
            # Only write if content changed
            if content != original_content:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                migrated_files.append(file_path)
                print(f"    ✅ Migrated successfully")
            else:
                print(f"    ℹ️  No changes needed")
        
        except Exception as e:
            print(f"    ❌ Error migrating {file_path}: {e}")
    
    return migrated_files

def test_compilation():
    """Test that the code still compiles after migration"""
    print(f"\n🧪 Testing compilation...")
    
    try:
        result = subprocess.run(
            ['./gradlew', 'compileTestJava'],
            capture_output=True,
            text=True,
            timeout=120
        )
        
        if result.returncode == 0:
            print(f"  ✅ Compilation successful!")
            return True
        else:
            print(f"  ❌ Compilation failed!")
            print(f"  Error output: {result.stderr[:500]}")
            return False
    
    except subprocess.TimeoutExpired:
        print(f"  ⏰ Compilation timed out")
        return False
    except Exception as e:
        print(f"  💥 Compilation error: {e}")
        return False

def main():
    """Main migration execution"""
    
    print("🚀 Starting Config Migration Process")
    print("=" * 50)
    
    # Step 1: Find all config usages
    usages = find_config_usages()
    print(f"Found {len(usages)} files using old config classes")
    
    if not usages:
        print("✅ No old config usages found - migration may already be complete!")
        return
    
    # Step 2: Generate migration plan
    plan = generate_migration_plan(usages)
    
    print(f"\n📋 MIGRATION PLAN:")
    print(f"  Phase 1 (Base configs): {len(plan['phase1_base_configs'])} files")
    print(f"  Phase 2 (Integration): {len(plan['phase2_integration_configs'])} files")
    print(f"  Phase 3 (Specialized): {len(plan['phase3_specialized'])} files")
    print(f"  Phase 4 (Manual review): {len(plan['phase4_manual_review'])} files")
    
    # Step 3: Execute Phase 1 migration
    if plan['phase1_base_configs']:
        response = input(f"\n🤔 Proceed with Phase 1 migration? (y/N): ")
        
        if response.lower() == 'y':
            migrated = execute_phase1_migration(plan['phase1_base_configs'])
            
            # Test compilation
            if test_compilation():
                print(f"\n🎉 Phase 1 migration successful!")
                print(f"✅ Migrated {len(migrated)} files to NewBaseTestConfig")
                
                # Save migration report
                save_migration_report(plan, migrated)
            else:
                print(f"\n⚠️  Phase 1 migration had compilation errors")
                print(f"Check the build output and fix any issues before continuing")
        else:
            print("❌ Phase 1 migration cancelled")
    else:
        print("\n✅ No Phase 1 migration needed")

def save_migration_report(plan, migrated_files):
    """Save migration progress report"""
    
    report_content = f"""# Config Migration Report

**Generated:** {os.path.basename(__file__)} on {__import__('datetime').datetime.now()}

## Phase 1 Results

**Files migrated to NewBaseTestConfig:** {len(migrated_files)}

### Migrated Files:
"""
    
    for file_path in migrated_files:
        report_content += f"- {file_path}\n"
    
    report_content += f"""

## Remaining Phases:

### Phase 2: Integration Configs ({len(plan['phase2_integration_configs'])} files)
Files that need migration to NewIntegrationTestConfig:
"""
    
    for item in plan['phase2_integration_configs']:
        report_content += f"- {item['file']} - {item['usages']}\n"
    
    report_content += f"""

### Phase 3: Specialized Configs ({len(plan['phase3_specialized'])} files) 
Files using specialized configs (keep for now):
"""
    
    for item in plan['phase3_specialized']:
        report_content += f"- {item['file']} - {item['usages']}\n"
    
    # Save report
    os.makedirs('reports', exist_ok=True)
    with open('reports/config_migration_progress.md', 'w') as f:
        f.write(report_content)
    
    print(f"\n📄 Migration report saved: reports/config_migration_progress.md")

if __name__ == "__main__":
    main()