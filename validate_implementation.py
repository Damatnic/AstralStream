#!/usr/bin/env python3
"""
AstralStream Implementation Validation Script

This script validates that all 5 missing features have been properly implemented
according to the original requirements in the Claude CLI Complete Implementation prompt.

Usage: python validate_implementation.py
"""

import os
import sys
import json
import subprocess
from pathlib import Path
from typing import Dict, List, Tuple, Any
from dataclasses import dataclass
from enum import Enum

class ValidationStatus(Enum):
    PASS = "✅ PASS"
    FAIL = "❌ FAIL" 
    WARNING = "⚠️  WARNING"
    SKIP = "⏭️  SKIP"

@dataclass
class ValidationResult:
    feature: str
    component: str
    status: ValidationStatus
    message: str
    details: str = ""

class AstralStreamValidator:
    def __init__(self, project_root: Path):
        self.project_root = project_root
        self.android_root = project_root / "android" / "app" / "src" / "main" / "java" / "com" / "astralplayer"
        self.test_root = project_root / "android" / "app" / "src" / "test" / "java" / "com" / "astralplayer"
        self.results: List[ValidationResult] = []
        
    def log_result(self, feature: str, component: str, status: ValidationStatus, message: str, details: str = ""):
        """Log a validation result"""
        result = ValidationResult(feature, component, status, message, details)
        self.results.append(result)
        print(f"{status.value} {feature} - {component}: {message}")
        if details:
            print(f"    {details}")
    
    def check_file_exists(self, filepath: Path, feature: str, component: str) -> bool:
        """Check if a required file exists"""
        if filepath.exists():
            self.log_result(feature, component, ValidationStatus.PASS, f"File exists: {filepath.name}")
            return True
        else:
            self.log_result(feature, component, ValidationStatus.FAIL, f"Missing file: {filepath}")
            return False
    
    def check_file_content(self, filepath: Path, required_content: List[str], feature: str, component: str) -> bool:
        """Check if file contains required content"""
        if not filepath.exists():
            self.log_result(feature, component, ValidationStatus.FAIL, f"File not found: {filepath}")
            return False
        
        try:
            content = filepath.read_text(encoding='utf-8')
            missing_content = []
            
            for requirement in required_content:
                if requirement not in content:
                    missing_content.append(requirement)
            
            if missing_content:
                self.log_result(
                    feature, component, ValidationStatus.FAIL,
                    f"Missing required content in {filepath.name}",
                    f"Missing: {', '.join(missing_content[:3])}{'...' if len(missing_content) > 3 else ''}"
                )
                return False
            else:
                self.log_result(
                    feature, component, ValidationStatus.PASS,
                    f"All required content found in {filepath.name}"
                )
                return True
                
        except Exception as e:
            self.log_result(feature, component, ValidationStatus.FAIL, f"Error reading {filepath}: {str(e)}")
            return False
    
    def validate_subtitle_cache_system(self) -> bool:
        """Validate Feature 1: Subtitle Cache System"""
        print("\n" + "="*60)
        print("🔍 VALIDATING FEATURE 1: SUBTITLE CACHE SYSTEM")
        print("="*60)
        
        feature = "Subtitle Cache System"
        success = True
        
        # Check core implementation files
        required_files = [
            (self.android_root / "astralstream" / "data" / "entity" / "CachedSubtitleEntity.kt", "CachedSubtitleEntity"),
            (self.android_root / "astralstream" / "data" / "dao" / "SubtitleCacheDao.kt", "SubtitleCacheDao"),
            (self.android_root / "features" / "subtitle" / "SubtitleCacheManager.kt", "SubtitleCacheManager"),
            (self.android_root / "features" / "subtitle" / "EncryptionManager.kt", "EncryptionManager"),
        ]
        
        for filepath, component in required_files:
            if not self.check_file_exists(filepath, feature, component):
                success = False
        
        # Check SubtitleCacheManager implementation
        cache_manager_path = self.android_root / "features" / "subtitle" / "SubtitleCacheManager.kt"
        required_cache_content = [
            "class SubtitleCacheManager",
            "suspend fun cacheSubtitle",
            "suspend fun getCachedSubtitle", 
            "LRU",
            "AES",
            "GZIP",
            "cleanupExpiredEntries",
            "getCacheStatistics"
        ]
        
        if not self.check_file_content(cache_manager_path, required_cache_content, feature, "SubtitleCacheManager Implementation"):
            success = False
        
        # Check database entity
        entity_path = self.android_root / "astralstream" / "data" / "entity" / "CachedSubtitleEntity.kt"
        required_entity_content = [
            "data class CachedSubtitleEntity",
            "isEncrypted",
            "compressionType",
            "cacheTime", 
            "accessCount",
            "quality",
            "fileSize",
            "originalSize",
            "compressedSize"
        ]
        
        if not self.check_file_content(entity_path, required_entity_content, feature, "CachedSubtitleEntity Structure"):
            success = False
        
        # Check test file
        test_path = self.test_root / "features" / "subtitle" / "SubtitleCacheManagerTest.kt"
        if not self.check_file_exists(test_path, feature, "Unit Tests"):
            success = False
        
        return success
    
    def validate_community_features(self) -> bool:
        """Validate Feature 2: Community Features"""
        print("\n" + "="*60)
        print("🔍 VALIDATING FEATURE 2: COMMUNITY FEATURES")
        print("="*60)
        
        feature = "Community Features"
        success = True
        
        # Check core implementation files
        required_files = [
            (self.android_root / "community" / "api" / "CommunityApiService.kt", "CommunityApiService"),
            (self.android_root / "community" / "repository" / "PlaylistSharingRepository.kt", "PlaylistSharingRepository"),
            (self.android_root / "community" / "repository" / "SubtitleContributionRepository.kt", "SubtitleContributionRepository"),
            (self.android_root / "community" / "ui" / "CommunityScreen.kt", "CommunityScreen"),
            (self.android_root / "community" / "data" / "CommunityDataClasses.kt", "CommunityDataClasses"),
        ]
        
        for filepath, component in required_files:
            if not self.check_file_exists(filepath, feature, component):
                success = False
        
        # Check API service implementation
        api_path = self.android_root / "community" / "api" / "CommunityApiService.kt"
        required_api_content = [
            "interface CommunityApiService",
            "sharePlaylist",
            "getSharedPlaylist", 
            "getTrendingPlaylists",
            "searchSharedPlaylists",
            "MockCommunityApiService",
            "ApiResponse"
        ]
        
        if not self.check_file_content(api_path, required_api_content, feature, "CommunityApiService Implementation"):
            success = False
        
        # Check UI implementation
        ui_path = self.android_root / "community" / "ui" / "CommunityScreen.kt"
        required_ui_content = [
            "@Composable",
            "fun CommunityScreen",
            "TabRow",
            "LazyColumn",
            "ShareTab",
            "DiscoverTab",
            "ContributeTab"
        ]
        
        if not self.check_file_content(ui_path, required_ui_content, feature, "CommunityScreen UI"):
            success = False
        
        # Check repository tests
        test_path = self.test_root / "community" / "repository" / "PlaylistSharingRepositoryTest.kt"
        if not self.check_file_exists(test_path, feature, "Unit Tests"):
            success = False
        
        return success
    
    def validate_gesture_customization(self) -> bool:
        """Validate Feature 3: Gesture Customization UI"""
        print("\n" + "="*60)
        print("🔍 VALIDATING FEATURE 3: GESTURE CUSTOMIZATION UI")
        print("="*60)
        
        feature = "Gesture Customization"
        success = True
        
        # Check core implementation files
        required_files = [
            (self.android_root / "features" / "gestures" / "data" / "GestureEntity.kt", "GestureEntity"),
            (self.android_root / "features" / "gestures" / "dao" / "GestureDao.kt", "GestureDao"),
            (self.android_root / "features" / "gestures" / "repository" / "GestureRepository.kt", "GestureRepository"),
            (self.android_root / "features" / "gestures" / "ui" / "GestureCustomizationScreen.kt", "GestureCustomizationScreen"),
            (self.android_root / "features" / "gestures" / "ui" / "GestureTestScreen.kt", "GestureTestScreen"),
        ]
        
        for filepath, component in required_files:
            if not self.check_file_exists(filepath, feature, component):
                success = False
        
        # Check gesture entity
        entity_path = self.android_root / "features" / "gestures" / "data" / "GestureEntity.kt"
        required_entity_content = [
            "data class CustomGestureEntity",
            "enum class GestureType",
            "enum class GestureAction", 
            "enum class GestureZone",
            "data class GestureProfileEntity",
            "TAP", "DOUBLE_TAP", "SWIPE_UP", "PINCH_IN",
            "PLAY_PAUSE", "VOLUME_UP", "SEEK_FORWARD"
        ]
        
        if not self.check_file_content(entity_path, required_entity_content, feature, "GestureEntity Structure"):
            success = False
        
        # Check customization UI
        ui_path = self.android_root / "features" / "gestures" / "ui" / "GestureCustomizationScreen.kt"
        required_ui_content = [
            "@Composable",
            "fun GestureCustomizationScreen",
            "GestureProfileCard",
            "GestureCard",
            "LazyColumn",
            "FloatingActionButton"
        ]
        
        if not self.check_file_content(ui_path, required_ui_content, feature, "GestureCustomizationScreen UI"):
            success = False
        
        # Check test file
        test_path = self.test_root / "features" / "gestures" / "repository" / "GestureRepositoryTest.kt"
        if not self.check_file_exists(test_path, feature, "Unit Tests"):
            success = False
        
        return success
    
    def validate_analytics_dashboard(self) -> bool:
        """Validate Feature 4: Analytics Dashboard UI"""
        print("\n" + "="*60)
        print("🔍 VALIDATING FEATURE 4: ANALYTICS DASHBOARD UI")
        print("="*60)
        
        feature = "Analytics Dashboard"
        success = True
        
        # Check core implementation files
        required_files = [
            (self.android_root / "features" / "analytics" / "data" / "AnalyticsEntity.kt", "AnalyticsEntity"),
            (self.android_root / "features" / "analytics" / "dao" / "AnalyticsDao.kt", "AnalyticsDao"),
            (self.android_root / "features" / "analytics" / "repository" / "AnalyticsRepository.kt", "AnalyticsRepository"),
            (self.android_root / "features" / "analytics" / "ui" / "AnalyticsDashboardScreen.kt", "AnalyticsDashboardScreen"),
            (self.android_root / "features" / "analytics" / "ui" / "AnalyticsComponents.kt", "AnalyticsComponents"),
            (self.android_root / "features" / "analytics" / "service" / "AnalyticsTracker.kt", "AnalyticsTracker"),
        ]
        
        for filepath, component in required_files:
            if not self.check_file_exists(filepath, feature, component):
                success = False
        
        # Check analytics entities
        entity_path = self.android_root / "features" / "analytics" / "data" / "AnalyticsEntity.kt"
        required_entity_content = [
            "data class PlaybackAnalyticsEntity",
            "data class ViewingSessionEntity",
            "data class DailyStatisticsEntity",
            "data class VideoStatisticsEntity", 
            "data class FeatureUsageEntity",
            "data class PerformanceMetricEntity",
            "enum class PlaybackEventType",
            "enum class FeatureCategory"
        ]
        
        if not self.check_file_content(entity_path, required_entity_content, feature, "AnalyticsEntity Structure"):
            success = False
        
        # Check dashboard UI
        dashboard_path = self.android_root / "features" / "analytics" / "ui" / "AnalyticsDashboardScreen.kt"
        required_dashboard_content = [
            "@Composable",
            "fun AnalyticsDashboardScreen",
            "HorizontalPager",
            "TabRow",
            "OverviewTab",
            "WatchTimeTab", 
            "ContentTab",
            "FeaturesTab",
            "PerformanceTab"
        ]
        
        if not self.check_file_content(dashboard_path, required_dashboard_content, feature, "AnalyticsDashboardScreen UI"):
            success = False
        
        # Check analytics components
        components_path = self.android_root / "features" / "analytics" / "ui" / "AnalyticsComponents.kt"
        required_components_content = [
            "WatchTimeChart",
            "HourlyHeatmap",
            "WeeklyPatternChart",
            "GenreChart",
            "PerformanceChart",
            "Canvas",
            "drawRoundRect"
        ]
        
        if not self.check_file_content(components_path, required_components_content, feature, "AnalyticsComponents Charts"):
            success = False
        
        # Check test file
        test_path = self.test_root / "features" / "analytics" / "repository" / "AnalyticsRepositoryTest.kt"
        if not self.check_file_exists(test_path, feature, "Unit Tests"):
            success = False
        
        return success
    
    def validate_video_editing(self) -> bool:
        """Validate Feature 5: Video Editing Implementation"""
        print("\n" + "="*60)
        print("🔍 VALIDATING FEATURE 5: VIDEO EDITING IMPLEMENTATION")
        print("="*60)
        
        feature = "Video Editing"
        success = True
        
        # Check core implementation files
        required_files = [
            (self.android_root / "features" / "editing" / "service" / "VideoEditingService.kt", "VideoEditingService"),
            (self.android_root / "features" / "editing" / "ui" / "VideoEditorScreen.kt", "VideoEditorScreen"),
            (self.android_root / "features" / "editing" / "ui" / "TimelineView.kt", "TimelineView"),
            (self.android_root / "features" / "editing" / "ui" / "ExportDialog.kt", "ExportDialog"),
            (self.android_root / "features" / "editing" / "viewmodel" / "VideoEditorViewModel.kt", "VideoEditorViewModel"),
            (self.android_root / "nextplayer" / "editing" / "VideoEditingDataClasses.kt", "VideoEditingDataClasses"),
        ]
        
        for filepath, component in required_files:
            if not self.check_file_exists(filepath, feature, component):
                success = False
        
        # Check video editing service
        service_path = self.android_root / "features" / "editing" / "service" / "VideoEditingService.kt"
        required_service_content = [
            "class VideoEditingService",
            "suspend fun initialize",
            "suspend fun createProject",
            "suspend fun importVideo",
            "suspend fun applyEffect",
            "suspend fun addTransition",
            "suspend fun exportVideo",
            "FFmpegKit"
        ]
        
        if not self.check_file_content(service_path, required_service_content, feature, "VideoEditingService Implementation"):
            success = False
        
        # Check editor UI
        editor_path = self.android_root / "features" / "editing" / "ui" / "VideoEditorScreen.kt"
        required_editor_content = [
            "@Composable",
            "fun VideoEditorScreen",
            "PlayerView",
            "TimelineView",
            "EditorTab",
            "TrimTools",
            "EffectsPanel",
            "TransitionsPanel",
            "ExportDialog"
        ]
        
        if not self.check_file_content(editor_path, required_editor_content, feature, "VideoEditorScreen UI"):
            success = False
        
        # Check timeline implementation  
        timeline_path = self.android_root / "features" / "editing" / "ui" / "TimelineView.kt"
        required_timeline_content = [
            "@Composable", 
            "fun TimelineView",
            "Canvas",
            "drawTimeline",
            "detectTransformGestures",
            "zoomLevel",
            "drawVideoTrack",
            "drawAudioTrack"
        ]
        
        if not self.check_file_content(timeline_path, required_timeline_content, feature, "TimelineView Implementation"):
            success = False
        
        # Check test file
        test_path = self.test_root / "features" / "editing" / "service" / "VideoEditingServiceTest.kt"
        if not self.check_file_exists(test_path, feature, "Unit Tests"):
            success = False
        
        return success
    
    def validate_integration_navigation(self) -> bool:
        """Validate Integration & Navigation"""
        print("\n" + "="*60)
        print("🔍 VALIDATING INTEGRATION & NAVIGATION")
        print("="*60)
        
        feature = "Integration & Navigation"
        success = True
        
        # Check navigation files
        required_files = [
            (self.android_root / "features" / "navigation" / "AstralStreamNavigation.kt", "AstralStreamNavigation"),
        ]
        
        for filepath, component in required_files:
            if not self.check_file_exists(filepath, feature, component):
                success = False
        
        # Check navigation implementation
        nav_path = self.android_root / "features" / "navigation" / "AstralStreamNavigation.kt"
        required_nav_content = [
            "sealed class AstralStreamDestination",
            "@Composable",
            "fun AstralStreamApp",
            "fun AstralStreamNavHost",
            "NavHost",
            "NavigationBar",
            "VideoEditorScreen",
            "AnalyticsDashboardScreen",
            "CommunityScreen"
        ]
        
        if not self.check_file_content(nav_path, required_nav_content, feature, "Navigation Implementation"):
            success = False
        
        # Check AppModule integration
        app_module_path = self.android_root / "astralstream" / "di" / "AppModule.kt"
        required_module_content = [
            "provideSubtitleCacheManager",
            "provideAnalyticsRepository", 
            "provideVideoEditingService",
            "provideGestureRepository"
        ]
        
        if not self.check_file_content(app_module_path, required_module_content, feature, "Dependency Injection"):
            success = False
        
        return success
    
    def validate_testing(self) -> bool:
        """Validate Testing Implementation"""
        print("\n" + "="*60)
        print("🔍 VALIDATING TESTING IMPLEMENTATION")
        print("="*60)
        
        feature = "Testing"
        success = True
        
        # Check test files exist
        required_test_files = [
            (self.test_root / "features" / "subtitle" / "SubtitleCacheManagerTest.kt", "SubtitleCacheManagerTest"),
            (self.test_root / "community" / "repository" / "PlaylistSharingRepositoryTest.kt", "PlaylistSharingRepositoryTest"),
            (self.test_root / "features" / "gestures" / "repository" / "GestureRepositoryTest.kt", "GestureRepositoryTest"),
            (self.test_root / "features" / "analytics" / "repository" / "AnalyticsRepositoryTest.kt", "AnalyticsRepositoryTest"),
            (self.test_root / "features" / "editing" / "service" / "VideoEditingServiceTest.kt", "VideoEditingServiceTest"),
            (self.test_root / "integration" / "AstralStreamIntegrationTest.kt", "AstralStreamIntegrationTest"),
        ]
        
        for filepath, component in required_test_files:
            if not self.check_file_exists(filepath, feature, component):
                success = False
        
        return success
    
    def validate_database_integration(self) -> bool:
        """Validate Database Integration"""
        print("\n" + "="*60)
        print("🔍 VALIDATING DATABASE INTEGRATION")
        print("="*60)
        
        feature = "Database Integration"
        success = True
        
        # Check database file
        db_path = self.android_root / "astralstream" / "data" / "database" / "AstralStreamDatabase.kt"
        if not self.check_file_exists(db_path, feature, "AstralStreamDatabase"):
            success = False
        
        # Check database content
        required_db_content = [
            "@Database",
            "version = 4",
            "entities = [",
            "CachedSubtitleEntity",
            "PlaybackAnalyticsEntity",
            "CustomGestureEntity",
            "SharedPlaylistEntity",
            "MIGRATION_1_2",
            "MIGRATION_2_3", 
            "MIGRATION_3_4"
        ]
        
        if not self.check_file_content(db_path, required_db_content, feature, "Database Configuration"):
            success = False
        
        return success
    
    def run_validation(self) -> Dict[str, Any]:
        """Run complete validation"""
        print("🚀 STARTING ASTRALSTREAM IMPLEMENTATION VALIDATION")
        print("="*80)
        
        validation_functions = [
            self.validate_subtitle_cache_system,
            self.validate_community_features, 
            self.validate_gesture_customization,
            self.validate_analytics_dashboard,
            self.validate_video_editing,
            self.validate_integration_navigation,
            self.validate_testing,
            self.validate_database_integration
        ]
        
        all_passed = True
        for validation_func in validation_functions:
            if not validation_func():
                all_passed = False
        
        # Generate summary
        print("\n" + "="*80)
        print("📊 VALIDATION SUMMARY")
        print("="*80)
        
        # Count results by status
        status_counts = {}
        feature_status = {}
        
        for result in self.results:
            status = result.status
            if status not in status_counts:
                status_counts[status] = 0
            status_counts[status] += 1
            
            # Track feature-level status
            feature = result.feature
            if feature not in feature_status:
                feature_status[feature] = {"pass": 0, "fail": 0, "warning": 0}
            
            if status == ValidationStatus.PASS:
                feature_status[feature]["pass"] += 1
            elif status == ValidationStatus.FAIL:
                feature_status[feature]["fail"] += 1
            elif status == ValidationStatus.WARNING:
                feature_status[feature]["warning"] += 1
        
        # Print status summary
        for status, count in status_counts.items():
            print(f"{status.value}: {count}")
        
        print(f"\nTotal Validations: {len(self.results)}")
        
        # Print feature summary
        print("\n📋 FEATURE STATUS:")
        for feature, counts in feature_status.items():
            total = counts["pass"] + counts["fail"] + counts["warning"]
            if counts["fail"] > 0:
                status_icon = "❌"
            elif counts["warning"] > 0:
                status_icon = "⚠️"
            else:
                status_icon = "✅"
            
            print(f"{status_icon} {feature}: {counts['pass']}/{total} passed")
        
        # Overall result
        overall_status = "✅ ALL VALIDATIONS PASSED" if all_passed else "❌ SOME VALIDATIONS FAILED"
        print(f"\n🎯 OVERALL RESULT: {overall_status}")
        
        if all_passed:
            print("\n🎉 SUCCESS: All 5 features have been successfully implemented!")
            print("   - Subtitle Cache System: ✅ Complete")
            print("   - Community Features: ✅ Complete") 
            print("   - Gesture Customization: ✅ Complete")
            print("   - Analytics Dashboard: ✅ Complete")
            print("   - Video Editing: ✅ Complete")
            print("   - Integration & Testing: ✅ Complete")
            print("\n   AstralStream implementation is READY! 🚀")
        else:
            print("\n⚠️  Some validations failed. Please review the failed items above.")
        
        return {
            "overall_success": all_passed,
            "total_validations": len(self.results),
            "status_counts": {status.name: count for status, count in status_counts.items()},
            "feature_status": feature_status,
            "results": [
                {
                    "feature": r.feature,
                    "component": r.component, 
                    "status": r.status.name,
                    "message": r.message,
                    "details": r.details
                }
                for r in self.results
            ]
        }

def main():
    """Main validation entry point"""
    if len(sys.argv) > 1:
        project_root = Path(sys.argv[1])
    else:
        # Auto-detect project root
        current_dir = Path.cwd()
        if current_dir.name == "AstralStream":
            project_root = current_dir
        else:
            # Look for AstralStream directory
            for parent in current_dir.parents:
                astral_dir = parent / "AstralStream"
                if astral_dir.exists():
                    project_root = astral_dir
                    break
            else:
                print("❌ Could not find AstralStream project root")
                print("Usage: python validate_implementation.py [PROJECT_ROOT]")
                sys.exit(1)
    
    print(f"🔍 Validating AstralStream implementation at: {project_root}")
    
    if not project_root.exists():
        print(f"❌ Project root does not exist: {project_root}")
        sys.exit(1)
    
    # Run validation
    validator = AstralStreamValidator(project_root)
    results = validator.run_validation()
    
    # Save results to JSON
    results_file = project_root / "validation_results.json"
    with open(results_file, 'w') as f:
        json.dump(results, f, indent=2)
    
    print(f"\n💾 Detailed results saved to: {results_file}")
    
    # Exit with appropriate code
    sys.exit(0 if results["overall_success"] else 1)

if __name__ == "__main__":
    main()