# Pharmagotchi Implementation Guide

## What's Been Completed ✅

### 1. Core Infrastructure
- ✅ Internet permission added to AndroidManifest
- ✅ Dependencies added (OkHttp, Gson, Coroutines, Room, Security-Crypto)
- ✅ Feet color matching on home page
- ✅ Room database initialized with auto-migration support

### 2. Data Models
- ✅ `ChatMessage.kt` - Chat message model with role, content, timestamp
- ✅ `VitalSign.kt` - Vital signs model with name, unit, category, normal range
- ✅ `GraphData.kt` - GraphMetadata and DataPoint entities for Room database

### 3. Security & Storage
- ✅ `SecurePreferencesManager.kt` - Encrypted API key storage using EncryptedSharedPreferences
- ✅ `PreferencesManager.kt` - Updated with vital signs storage using Gson serialization
- ✅ `GraphDao.kt` - Room DAO with Flow-based reactive queries
- ✅ `PharmagotchiDatabase.kt` - Room database singleton setup

### 4. API Integration
- ✅ `OpenRouterService.kt` - Complete OpenRouter API client
  - sendMessage() for chat completions
  - parseVitalSigns() for extracting health metrics from conditions/medications
  - Uses Claude 3.5 Sonnet model
  - Proper error handling with Result types

### 5. PharmAI Chat System
- ✅ `PharmAIChatActivity.kt` - Full-featured AI chat interface
  - API key prompt on first use
  - Welcome message with user's health profile
  - Context-aware system prompts
  - Loading states and error handling
- ✅ `activity_pharm_ai_chat.xml` - Chat UI layout
- ✅ `item_chat_message.xml` - Chat bubble design
- ✅ `ChatAdapter.kt` - RecyclerView adapter with user/assistant differentiation
- ✅ `PharmAIFragment.kt` - Landing page with "Start Chat" button
- ✅ `fragment_home.xml` - Updated with pharmagotchi icon and description

### 6. Vital Signs Parsing & Onboarding
- ✅ `MedicationsActivity.kt` - Enhanced with AI parsing
  - Automatically calls OpenRouter after onboarding
  - Parses JSON response to extract vital signs
  - Creates initial graphs in database
  - Handles edge cases (no API key, no conditions, already parsed)
  - User feedback with progress dialog

### 7. Graph System - Expandable Blocks
- ✅ `item_graph_block.xml` - Expandable card layout
  - Header shows metric name and latest value
  - Expand/collapse with animated icon
  - Shows recent data points when expanded
  - Action buttons (Add Data, View All)
- ✅ `item_data_point.xml` - Individual data point item layout
- ✅ `DataPointAdapter.kt` - Adapter with relative time formatting
- ✅ `GraphBlockAdapter.kt` - Expandable graph blocks adapter
  - Maintains expansion state per graph
  - Handles empty states
  - Click handlers for all actions

### 8. Progress Tracking Fragment
- ✅ `fragment_notifications.xml` - Redesigned with RecyclerView and FAB
- ✅ `ProgressFragment.kt` - Complete implementation
  - Observes graphs from Room database with Flow
  - Shows empty state when no metrics exist
  - "Add Data" dialog for quick entry
  - "Add Custom Metric" dialog via FAB
  - Real-time updates when data changes

### 9. Track Activity Feature
- ✅ Implemented via "Add Data" button in each graph block
- ✅ Dialog with numeric input and unit display
- ✅ Saves to Room database with timestamp
- ✅ Immediate UI update via Flow

### 10. Custom Graph Creation
- ✅ FAB button in ProgressFragment
- ✅ Dialog with name and unit inputs
- ✅ Saves as custom graph (isCustom = true)
- ✅ Immediately appears in graphs list

## How to Use

### First Time Setup
1. Launch the app
2. Complete onboarding:
   - Customize your pharmagotchi (color & name)
   - Select medical conditions
   - Select medications
3. If you have an OpenRouter API key, the app will:
   - Analyze your health profile
   - Create relevant health metrics automatically
   - Set up graphs for tracking

### PharmAI Chat
1. Navigate to "PharmAI" tab (first tab)
2. Tap "Start Chat"
3. If prompted, enter your OpenRouter API key
4. Ask questions about medications, conditions, or health topics
5. The AI has context about your health profile

### Tracking Health Metrics
1. Navigate to "Progress" tab (third tab)
2. View all your health metrics as expandable blocks
3. Tap a block to expand and see:
   - Recent data points
   - Graph visualization placeholder
4. Tap "Add Data" to log a new measurement
5. Tap "View All" to see all historical data (coming soon)

### Adding Custom Metrics
1. In "Progress" tab, tap the + FAB button
2. Enter metric name (e.g., "Water Intake")
3. Enter unit (e.g., "glasses")
4. Tap "Create"
5. Your custom metric appears in the list

## Architecture

### Data Flow
1. **Onboarding** → PreferencesManager → OpenRouter API → VitalSigns → Room Database
2. **Chat** → PharmAIChatActivity → OpenRouter API → ChatAdapter
3. **Graphs** → Room Database (Flow) → ProgressFragment → GraphBlockAdapter
4. **Data Entry** → Dialog → Room Database → Flow update → UI refresh

### Key Technologies
- **Kotlin Coroutines** for async operations
- **Flow** for reactive database queries
- **Room** for local data persistence
- **EncryptedSharedPreferences** for secure API key storage
- **OkHttp** for HTTP networking
- **Gson** for JSON parsing
- **Material Design Components** for UI

## Files Reference

### Activities
- `WelcomeActivity.kt` - First launch screen
- `CustomizationActivity.kt` - Pharmagotchi customization
- `MedicalConditionsActivity.kt` - Medical conditions selection
- `MedicationsActivity.kt` - Medications selection + vital signs parsing
- `PharmAIChatActivity.kt` - AI chat interface
- `MainActivity.kt` - Main app with bottom navigation

### Fragments
- `HomeFragment.kt` - Home tab with animated pharmagotchi
- `PharmAIFragment.kt` - PharmAI landing page
- `ProgressFragment.kt` - Health metrics tracking

### Adapters
- `ChatAdapter.kt` - Chat messages
- `GraphBlockAdapter.kt` - Expandable graph blocks
- `DataPointAdapter.kt` - Individual data points

### Database
- `PharmagotchiDatabase.kt` - Room database
- `GraphDao.kt` - Graph data access object

### Models
- `ChatMessage.kt` - Chat message data
- `VitalSign.kt` - Vital sign metadata
- `GraphMetadata.kt` - Graph configuration
- `DataPoint.kt` - Health measurement data

### Managers
- `PreferencesManager.kt` - App preferences
- `SecurePreferencesManager.kt` - Encrypted API key

### API
- `OpenRouterService.kt` - OpenRouter API client

## Future Enhancements

### Potential Features
- Graph visualization (charts/plots)
- Data export (CSV, PDF)
- Reminders for measurements
- Trends and insights
- Share data with healthcare providers
- Multiple user profiles
- Medication reminders
- Integration with health apps (Google Fit, Apple Health)

### Known Limitations
- Graph visualization is placeholder only
- "View All" button shows toast (not implemented)
- No data editing/deletion UI
- No data backup/restore
- Chat history not persisted

## Troubleshooting

### No graphs appear after onboarding
- Make sure you entered medical conditions or medications
- Ensure you provided a valid OpenRouter API key
- Check internet connection during onboarding
- You can manually add graphs using the + button

### Chat not working
- Verify your OpenRouter API key is correct
- Check internet connection
- Ensure you have API credits in your OpenRouter account

### Data not saving
- Check app permissions
- Ensure sufficient storage space
- Try restarting the app

## API Key Setup

### Getting an OpenRouter API Key
1. Visit https://openrouter.ai/
2. Sign up for an account
3. Navigate to API Keys section
4. Create a new API key
5. Copy the key and paste it when prompted in the app

### Security
- API keys are stored using Android's EncryptedSharedPreferences
- Keys are encrypted using AES256-GCM
- Keys never leave the device except for API calls to OpenRouter
