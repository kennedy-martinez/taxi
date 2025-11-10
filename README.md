# 🚕 Taximeter Technical Challenge

This is a production-ready implementation of the Taximeter technical challenge, built following Clean Architecture principles, MVVM, and a fully extensible design.

## ✨ Key Features

* **Multi-Module Architecture:** The project is decoupled into `:app`, `:data`, and `:domain` modules for a clear separation of concerns.
* **Real-time Fare Calculation:** The fare is calculated in real-time based on distance (km) and time (seconds) using `StateFlow` and `combine`.
* **100% Offline (Cache-First):** The app uses Room as a cache. It only calls the API on the first run. If the API fails or the device is offline, the app continues to function perfectly with the last known configuration.
* **Extensible Design (Strategy Pattern):** Fulfills the "senior" requirement. Adding new supplements (like a "Baby Seat") is trivial and does **not** require modifying the main `ViewModel` logic.
* **Error Management:** The UI handles `Loading` and `Error` states (with a retry button) if the price configuration cannot be fetched.
* **Trip Simulation:** Includes 3 fake GPS routes (`Route1`, `Route2`, `Route3`) and two speeds (`Default` and `Fast`) for testing, as requested.

---

## 🏗️ Architecture

The project adheres to **Clean Architecture** and **MVVM** principles.

### `:domain`
A pure Kotlin (`kotlin-jvm`) module. It contains the core business models (e.g., `PriceConfig`), repository interfaces (`TaximeterRepository`), and the business logic interfaces (e.g., `SupplementStrategy`). It has no knowledge of Android, Room, or Retrofit.

### `:data`
An Android library module that contains the implementation details.
* **`TaximeterRepositoryImpl`**: Implements the "Cache-First" logic.
* **Network (Retrofit & Moshi):** Handles fetching the remote price configuration.
* **Database (Room):** Caches the price configuration for offline use.
* **Strategies (`LuggageStrategy`):** Concrete implementations of the Strategy Pattern.
* **`LocationProvider`**: The fake GPS simulator.

### `:app`
The main Android Application module (UI layer).
* **Hilt:** Used for Dependency Injection across the entire app (with KSP).
* **`TaximeterViewModel`**: The "brain" of the UI. It orchestrates business logic and exposes a single, reactive `StateFlow` (`uiState`).
* **`TaximeterScreen` (Compose):** The "dumb" view that observes the `uiState` and recomposes accordingly.

---

## 🚀 Key Requirement: Extensibility

> "Adding a new supplement (e.g., 'Baby Seat') must be simple."

This solution addresses extensibility on two levels:

### 1. Logic (Strategy Pattern)
The calculation logic **is not "hardcoded"** into the `ViewModel`. The **Strategy Pattern** is used via Hilt:

1.  The `ViewModel` receives an injected `Set<SupplementStrategy>`.
2.  Each `SupplementStrategy` (like `LuggageStrategy`) defines its own `id` ("luggage") and its calculation logic (`count * 5.0`).
3.  The `ViewModel` simply iterates over this `Set` to calculate the total price.

**To add an "Infant Seat (€3)":**
1.  Create the `InfantSeatStrategy` class (with `id = "infant_seat"` and `calculate { count * 3.0 }`).
2.  Add it to Hilt's `StrategyModule.kt` using `@Binds @IntoSet`.
*(The `ViewModel` requires no changes to calculate the new price).*

### 2. UI (Dynamic Supplements)
The UI (`TaximeterScreen`) **is also not "hardcoded"**.
1.  The `ViewModel` uses the `Set<SupplementStrategy>` to dynamically build the list of `SupplementUiModel` that is sent to the view.
2.  The `TaximeterScreen` simply uses a `forEach` to render *any* supplement it receives in the list.

**To add the "Infant Seat" to the UI:**
1.  (After completing Logic Step 1) Only the UI display name needs to be added to the `supplementDisplayNames` map in the `ViewModel`.

**The UI (`TaximeterScreen.kt`) and the core `ViewModel` logic (`combine`, `createActiveUiState`, etc.) require no other changes.**

---

## 🧪 Testing

The project is protected with a test suite that validates the business and data logic.

### 1. `TaximeterViewModelTest` (Unit Test - `app/src/test`)
* Tests the "brain" (`ViewModel`) using `MockK` and `Turbine`.
* Validates 5 scenarios:
    * ✅ **Loading State:** Confirms `isLoadingConfig` is true on startup.
    * ✅ **Error State:** Confirms `isConfigError` is true if the API fails.
    * ✅ **Scenario 1 (Full):** 11km/1800s + 1 Luggage = **€25.20**
    * ✅ **Scenario 2 (No Luggage):** 11km/1800s + 0 Luggage = **€20.20**
    * ✅ **Scenario 3 (Short Ride):** Route2 + 1 Luggage = **~€5.60**

### 2. `TaximeterRepositoryImplTest` (Instrumentation Test - `app/src/androidTest`)
* Tests the "Cache-First" logic of the repository.
* Uses `Room.inMemoryDatabaseBuilder` (for fake DB) and `MockWebServer` (for fake API).
* Validates 5 scenarios:
    * ✅ Cache is Empty (calls API)
    * ✅ Cache is Full (does NOT call API)
    * ✅ 404 Error (API failure)
    * ✅ Malformed JSON (API failure)
    * ✅ Network Timeout (API failure)