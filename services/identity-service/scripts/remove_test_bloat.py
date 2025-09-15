#!/usr/bin/env python3

import os
import csv
import shutil
from datetime import datetime
from pathlib import Path
import subprocess

def create_backup():
    """Create a backup of the test directory before making changes"""
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    backup_dir = f"test_backup_{timestamp}"
    
    print(f"📦 Creating backup: {backup_dir}")
    
    if os.path.exists('src/test'):
        shutil.copytree('src/test', backup_dir)
        print(f"✅ Backup created successfully at: {backup_dir}")
        return backup_dir
    else:
        print("❌ Test directory not found!")
        return None

def find_file_path(class_name):
    """Find the actual file path for a class name"""
    
    # Try common patterns
    possible_paths = [
        f"src/test/java/com/focushive/identity/config/{class_name}.java",
        f"src/test/java/com/focushive/identity/integration/{class_name}.java", 
        f"src/test/java/com/focushive/identity/{class_name}.java",
        f"src/test/java/com/focushive/identity/controller/{class_name}.java",
        f"src/test/java/com/focushive/identity/entity/{class_name}.java",
        f"src/test/java/com/focushive/identity/service/{class_name}.java"
    ]
    
    for path in possible_paths:
        if os.path.exists(path):
            return path
    
    # Search more broadly
    try:
        result = subprocess.run(['find', 'src/test', '-name', f'{class_name}.java'], 
                              capture_output=True, text=True)
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip().split('\n')[0]
    except:
        pass
    
    return None

def check_dependencies(file_path):
    """Check if other files depend on this file"""
    if not file_path or not os.path.exists(file_path):
        return []
    
    class_name = os.path.basename(file_path).replace('.java', '')
    dependencies = []
    
    try:
        # Search for imports or references to this class
        result = subprocess.run(['grep', '-r', class_name, 'src/test/', '--include=*.java'], 
                              capture_output=True, text=True)
        
        if result.returncode == 0:
            lines = result.stdout.strip().split('\n')
            for line in lines:
                if ':' in line:
                    file_ref = line.split(':')[0]
                    if file_ref != file_path:  # Exclude self-references
                        dependencies.append(file_ref)
        
    except:
        pass
    
    return list(set(dependencies))  # Remove duplicates

def remove_bloat_files():
    """Remove the identified bloat files"""
    
    if not os.path.exists('reports/deletion_candidates.csv'):
        print("❌ Deletion candidates file not found. Run test_cleanup_analysis.py first.")
        return
    
    # Create backup first
    backup_dir = create_backup()
    if not backup_dir:
        print("❌ Cannot proceed without backup")
        return
    
    print(f"\n🗑️  Starting test bloat removal...")
    
    # Load deletion candidates
    with open('reports/deletion_candidates.csv', 'r') as f:
        reader = csv.DictReader(f)
        candidates = list(reader)
    
    removed_files = []
    skipped_files = []
    missing_files = []
    
    for candidate in candidates:
        class_name = candidate['file']
        reason = candidate['reason']
        category = candidate['category']
        
        print(f"\n🔍 Processing: {class_name}")
        
        # Find the actual file
        file_path = find_file_path(class_name)
        
        if not file_path:
            print(f"  ⚠️  File not found: {class_name}")
            missing_files.append(class_name)
            continue
        
        print(f"  📁 Found: {file_path}")
        
        # Check for dependencies
        dependencies = check_dependencies(file_path)
        
        if dependencies:
            print(f"  ⚠️  Has dependencies ({len(dependencies)} files), skipping for safety:")
            for dep in dependencies[:3]:  # Show first 3 dependencies
                print(f"    - {os.path.basename(dep)}")
            if len(dependencies) > 3:
                print(f"    ... and {len(dependencies) - 3} more")
            skipped_files.append({
                'file': class_name,
                'path': file_path, 
                'dependencies': len(dependencies),
                'reason': reason
            })
            continue
        
        # Safe to remove
        try:
            print(f"  ❌ Removing: {class_name}")
            os.remove(file_path)
            removed_files.append({
                'file': class_name,
                'path': file_path,
                'reason': reason,
                'category': category
            })
            print(f"  ✅ Removed successfully")
            
        except Exception as e:
            print(f"  ❌ Error removing {class_name}: {e}")
            skipped_files.append({
                'file': class_name,
                'path': file_path,
                'error': str(e),
                'reason': reason
            })
    
    # Generate summary report
    generate_removal_report(removed_files, skipped_files, missing_files, backup_dir)

def generate_removal_report(removed_files, skipped_files, missing_files, backup_dir):
    """Generate a report of the removal process"""
    
    report_content = f"""# Test Bloat Removal Report

**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
**Backup Location:** {backup_dir}

## Summary

- **Files Removed:** {len(removed_files)}
- **Files Skipped:** {len(skipped_files)} (had dependencies)
- **Files Missing:** {len(missing_files)} (already deleted?)

## Files Successfully Removed ({len(removed_files)})

"""
    
    for removed in removed_files:
        report_content += f"- ✅ **{removed['file']}** ({removed['category']})\n"
        report_content += f"  - Path: `{removed['path']}`\n"  
        report_content += f"  - Reason: {removed['reason']}\n\n"
    
    if skipped_files:
        report_content += f"## Files Skipped Due to Dependencies ({len(skipped_files)})\n\n"
        for skipped in skipped_files:
            report_content += f"- ⚠️ **{skipped['file']}**\n"
            report_content += f"  - Reason: {skipped['reason']}\n"
            if 'dependencies' in skipped:
                report_content += f"  - Dependencies: {skipped['dependencies']} files\n"
            if 'error' in skipped:
                report_content += f"  - Error: {skipped['error']}\n"
            report_content += "\n"
    
    if missing_files:
        report_content += f"## Files Not Found ({len(missing_files)})\n\n"
        for missing in missing_files:
            report_content += f"- ❓ **{missing}**\n"
        report_content += "\n"
    
    report_content += f"""## Next Steps

1. **Verify tests still pass**: Run `./gradlew test` to ensure nothing broke
2. **Update test batches**: Re-run the inventory to update batch manifests
3. **Manual review**: Check skipped files to see if they can be safely removed
4. **Commit changes**: If all tests pass, commit the cleanup

## Rollback Instructions

If something goes wrong, restore from backup:
```bash
rm -rf src/test
mv {backup_dir} src/test
```

## Files Saved

Total test files reduced by: **{len(removed_files)}**
Estimated time savings: **~{len(removed_files) * 30} seconds** per full test run
"""
    
    # Save report
    os.makedirs('reports', exist_ok=True)
    with open('reports/test_removal_report.md', 'w') as f:
        f.write(report_content)
    
    # Print summary
    print(f"\n📊 REMOVAL SUMMARY")
    print(f"{'='*50}")
    print(f"✅ Files removed: {len(removed_files)}")
    print(f"⚠️  Files skipped: {len(skipped_files)}")  
    print(f"❓ Files missing: {len(missing_files)}")
    print(f"💾 Backup saved: {backup_dir}")
    print(f"📄 Report saved: reports/test_removal_report.md")
    
    if removed_files:
        print(f"\n🎉 Successfully removed {len(removed_files)} bloat test files!")
        print(f"💡 Recommended next step: Run './gradlew test' to verify nothing broke")
    
    if skipped_files:
        print(f"\n⚠️  {len(skipped_files)} files were skipped due to dependencies")
        print(f"💡 Review reports/test_removal_report.md for details")

def main():
    """Main execution"""
    print("🧹 Test Bloat Removal Tool")
    print("=" * 40)
    
    # Safety check
    if not os.path.exists('src/test'):
        print("❌ Test directory not found!")
        return
    
    print("⚠️  This will permanently delete test files!")
    print("📦 A backup will be created automatically")
    
    response = input("\nProceed with test bloat removal? (y/N): ")
    
    if response.lower() != 'y':
        print("❌ Operation cancelled")
        return
    
    remove_bloat_files()

if __name__ == "__main__":
    main()